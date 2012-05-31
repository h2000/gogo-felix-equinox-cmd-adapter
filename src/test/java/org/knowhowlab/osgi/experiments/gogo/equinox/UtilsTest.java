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

import org.apache.felix.service.command.Descriptor;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author dmytro.pishchukhin
 */
public class UtilsTest {
    @Test
    public void convertTest() {
        ShellInfo shellInfo = Utils.createGogoService(new NoHelpCommandProvider());
        Assert.assertNotNull(shellInfo);
        Assert.assertEquals("equinox", shellInfo.getScope());
        Assert.assertNotNull(shellInfo.getCommands());
        Assert.assertEquals(2, shellInfo.getCommands().length);
        Assert.assertEquals("hello", shellInfo.getCommands()[0]);
        Assert.assertEquals("print", shellInfo.getCommands()[1]);

        Class<? extends EquinoxGogoAdapter> shellClass = shellInfo.getShellClass();
        Assert.assertNotNull(shellClass);

        Method[] methods = shellClass.getMethods();

        Method hello = findMethod(methods, "hello");
        Method print = findMethod(methods, "print");
        Assert.assertNotNull(hello);
        Assert.assertNotNull(print);

        Assert.assertNotNull(findDescriptionAnnotation(hello));
        Assert.assertNull(findDescriptionAnnotation(print));
    }

    private String findDescriptionAnnotation(Method method) {
        Descriptor descriptor = method.getAnnotation(Descriptor.class);
        if (descriptor != null) {
            return descriptor.value();
        }
        return null;
    }

    private Method findMethod(Method[] methods, String name) {
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }
}
