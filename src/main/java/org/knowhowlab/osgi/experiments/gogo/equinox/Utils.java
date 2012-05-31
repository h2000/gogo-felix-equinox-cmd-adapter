/*
 * Copyright (c) 2010 Dmytro Pishchukhin (http://knowhowlab.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.knowhowlab.osgi.experiments.gogo.equinox;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to create GoGo service that is based on Equinox CommandProvider serice
 *
 * @author dmytro.pishchukhin
 */
public class Utils {
    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(Utils.class.getName());

    /**
     * Default scope
     */
    private static final String DEFAULT_SCOPE = "equinox";

    /**
     * Classes pool
     */
    private static final ClassPool POOL = ClassPool.getDefault();

    /**
     * Initialization of Classes pool
     */
    static {
        POOL.appendClassPath(new ClassClassPath(EquinoxGogoAdapter.class));
        POOL.appendClassPath(new ClassClassPath(CommandInterpreter.class));
    }

    /**
     * Create GoGo service based on CommandProvider service
     *
     * @param provider Equinox CommandProvider service instance
     * @return GoGo service info or <code>null</code>
     */
    public static <T extends CommandProvider> ShellInfo createGogoService(T provider) {
        // create class
        CtClass ctClass = POOL.makeClass(EquinoxGogoAdapter.class.getName() + "_" + provider.getClass().getSimpleName());

        // init set of commands
        SortedSet<String> commands = new TreeSet<String>();

        Class<EquinoxGogoAdapter> shellClass;

        try {
            // check if class could be extended
            if (!ctClass.isFrozen()) {
                // create files and pool
                ClassFile ccFile = ctClass.getClassFile();
                ccFile.setVersionToJava5();
                ConstPool constPool = ccFile.getConstPool();

                // set superclass
                CtClass abstractCtClass = POOL.getCtClass(EquinoxGogoAdapter.class.getName());
                ctClass.setSuperclass(abstractCtClass);

                // create constructor
                CtClass serviceCtClass = POOL.getCtClass(CommandProvider.class.getName());
                CtConstructor ctConstructor = new CtConstructor(new CtClass[]{
                        serviceCtClass
                }, ctClass);
                ctConstructor.setModifiers(Modifier.PUBLIC);
                ctConstructor.setBody("super($1);");
                ctClass.addConstructor(ctConstructor);

                Map<String, String> help = parseHelp(provider.getHelp());

                // create methods based on methods in Equinox CommandProvider service instance
                Class<? extends CommandProvider> providerClass = provider.getClass();
                CtClass commandSessionCtClass = POOL.getCtClass(CommandSession.class.getName());
                CtClass argsCtClass = POOL.getCtClass(String[].class.getName());
                CtClass objectCtClass = POOL.getCtClass(Object.class.getName());
                Method[] methods = providerClass.getMethods();
                for (Method method : methods) {
                    // if method starts with "_" - Equinox command name convention
                    if (method.getName().startsWith("_")) {
                        Class<?>[] params = method.getParameterTypes();
                        // if method has only one param CommandInterpreter - Equinox command name convention
                        if (params.length == 1 && params[0].equals(CommandInterpreter.class)) {
                            String shellCommandName = method.getName().substring(1);
                            commands.add(shellCommandName);

                            // generate method for GoGo shell with 2 params: CommaneSession and
                            // String[] for shell command parameters
                            CtMethod ctMethod;
                            // method returns nothing
                            if (Void.class.equals(method.getReturnType())) {
                                ctMethod = new CtMethod(CtClass.voidType, shellCommandName, new CtClass[]{
                                        commandSessionCtClass, argsCtClass
                                }, ctClass);
                                ctMethod.setModifiers(Modifier.PUBLIC);
                                ctMethod.setBody("runCommand($1, $2, \"" + shellCommandName + "\");");
                            } else {
                                // method return something
                                ctMethod = new CtMethod(objectCtClass, shellCommandName, new CtClass[]{
                                        commandSessionCtClass, argsCtClass
                                }, ctClass);
                                ctMethod.setModifiers(Modifier.PUBLIC);
                                ctMethod.setBody("return runCommandWithResult($1, $2, \"" + shellCommandName + "\");");
                            }

                            // if help for this command is found - add GoGo descriptor for this shell command
                            if (help.containsKey(shellCommandName)) {
                                AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
                                Annotation annotation = new Annotation(Descriptor.class.getName(), constPool);
                                annotation.addMemberValue("value", new StringMemberValue(help.get(shellCommandName), constPool)); //todo: add help
                                annotationsAttribute.addAnnotation(annotation);
                                ctMethod.getMethodInfo().addAttribute(annotationsAttribute);
                            }

                            // add method to class
                            ctClass.addMethod(ctMethod);
                        }
                    }
                }
            }
            // generate class
            shellClass = ctClass.toClass(EquinoxGogoAdapter.class.getClassLoader(),
                    EquinoxGogoAdapter.class.getProtectionDomain());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to create Equinox GoGo adapter for: " + provider.getClass(), e);
            return null;
        }
        return new ShellInfo(DEFAULT_SCOPE, commands.toArray(new String[commands.size()]), shellClass);
    }

    /**
     * Parse Equinox CommandProvider help
     * @param help help string
     * @return map with parsed commands and usages
     */
    private static Map<String, String> parseHelp(String help) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (help != null) {
            // split by lines
            String[] commandsHelp = help.split("\n");
            for (String commandHelp : commandsHelp) {
                // parse command name (<command_name> <command_usage>)
                int spaceIndex = commandHelp.indexOf(" ");
                if (spaceIndex != -1) {
                    String commandName = commandHelp.substring(0, spaceIndex).trim();
                    String commandUsage = commandHelp.substring(spaceIndex).trim();
                    // if command usage starts with "- " - remove those symbols
                    if (commandUsage.startsWith("- ")) {
                        commandUsage = commandUsage.substring(2);
                    }
                    map.put(commandName, commandUsage);
                }
            }
        }
        return map;
    }

    /**
     * Detach generated class
     */
    public static void clean(String className) {
        try {
            CtClass ctClass = POOL.getCtClass(className);
            ctClass.defrost();
            ctClass.detach();
        } catch (NotFoundException e) {
            LOG.log(Level.WARNING, "Unable to clean Console Service. " + e.getMessage(), e);
        }
    }
}
