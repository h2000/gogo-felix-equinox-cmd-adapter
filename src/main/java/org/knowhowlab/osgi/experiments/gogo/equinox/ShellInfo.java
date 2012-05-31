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

/**
 * GoGo service information
 *
 * @author dmytro.pishchukhin
 */
public class ShellInfo {
    /**
     * Service scope
     */
    private String scope;
    /**
     * List of commands that are provided by service
     */
    private String[] commands;
    /**
     * Service class
     */
    private Class<? extends EquinoxGogoAdapter> shellClass;

    public ShellInfo(String scope, String[] commands, Class<? extends EquinoxGogoAdapter> shellClass) {
        this.scope = scope;
        this.commands = commands;
        this.shellClass = shellClass;
    }

    public String getScope() {
        return scope;
    }

    public String[] getCommands() {
        return commands;
    }

    public Class<? extends EquinoxGogoAdapter> getShellClass() {
        return shellClass;
    }
}
