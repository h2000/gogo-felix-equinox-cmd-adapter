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

import org.apache.felix.service.command.CommandSession;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class for Gogo adapter
 *
 * @author dmytro.pishchukhin
 */
public abstract class EquinoxGogoAdapter {
    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(Utils.class.getName());

    /**
     * Equinox CommandProvider service instance
     */
    private CommandProvider provider;

    public EquinoxGogoAdapter(CommandProvider provider) {
        this.provider = provider;
    }

    /**
     * Run shell command without any return value
     *
     * @param commandSession GoGo CommandSession
     * @param args           command arguments
     * @param commandName    command name
     */
    protected void runCommand(CommandSession commandSession, String[] args, String commandName) {
        try {
            // get method
            Method method = provider.getClass().getMethod("_" + commandName, CommandInterpreter.class);
            // invoke method
            method.invoke(provider, new CommandInterpreterImpl(commandSession, args));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to execute shell command: " + commandName, e);
            e.printStackTrace();
        }
    }

    /**
     * Run shell command with a return value
     *
     * @param commandSession GoGo CommandSession
     * @param args           command arguments
     * @param commandName    command name
     * @return result or <code>null</code> in case of error
     */
    protected Object runCommandWithResult(CommandSession commandSession, String[] args, String commandName) {
        try {
            Method method = provider.getClass().getMethod("_" + commandName, CommandInterpreter.class);
            return method.invoke(provider, new CommandInterpreterImpl(commandSession, args));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to execute shell command: " + commandName, e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Implementation of Equinox CommandInterpreter
     *
     * @author dmytro.pishchukhin
     */
    public static class CommandInterpreterImpl implements CommandInterpreter {
        private final CommandSession commandSession;
        private Iterator<String> argsIterator;

        public CommandInterpreterImpl(CommandSession commandSession, String[] args) {
            this.commandSession = commandSession;
            argsIterator = Arrays.asList(args).iterator();
        }

        public String nextArgument() {
            if (argsIterator.hasNext()) {
                return argsIterator.next();
            }
            return null;
        }

        public Object execute(String cmd) {
            try {
                return commandSession.execute(cmd);
            } catch (Exception e) {
                e.printStackTrace(commandSession.getConsole());
                return null;
            }
        }

        public void print(Object o) {
            commandSession.getConsole().print(o);
        }

        public void println() {
            commandSession.getConsole().println();
        }

        public void println(Object o) {
            commandSession.getConsole().println(o);
        }

        public void printStackTrace(Throwable t) {
            t.printStackTrace(commandSession.getConsole());
        }

        public void printDictionary(Dictionary dic, String title) {
            PrintStream console = commandSession.getConsole();
            console.println(title);
            if (dic != null) {
                Enumeration keys = dic.elements();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    console.printf("%s = %s", key, dic.get(key));
                }
            }
        }

        public void printBundleResource(Bundle bundle, String resource) {
            URL entry = bundle.getEntry(resource);
            if (entry != null) {
                try {
                    println(resource);
                    InputStream in = entry.openStream();
                    byte[] buffer = new byte[1024];
                    int read;
                    try {
                        while ((read = in.read(buffer)) != -1) {
                            print(new String(buffer, 0, read));
                        }
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                } catch (Exception e) {
                    PrintStream console = commandSession.getConsole();
                    console.println("Error reading resource: " + resource);
                    e.printStackTrace(console);
                }
            } else {
                PrintStream console = commandSession.getConsole();
                console.println("Unknown resource: " + resource);
            }
        }
    }
}
