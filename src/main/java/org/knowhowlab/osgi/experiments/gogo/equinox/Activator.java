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

import org.apache.felix.service.command.CommandProcessor;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bundle Activator that tracks CommandProvider services and register
 * corresponding GoGo services
 *
 * @author dmytro.pishchukhin
 * @see org.eclipse.osgi.framework.console.CommandProvider
 */
public class Activator implements BundleActivator {
    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    /**
     * Bundle context
     */
    private BundleContext bc;
    /**
     * Service tracker to track CommandProvider
     */
    private ServiceTracker tracker;
    /**
     * Maps CommandProvider services and GoGo services
     */
    private Map<ServiceReference, ServiceRegistration> registrations = new HashMap<ServiceReference, ServiceRegistration>();

    public void start(BundleContext context) throws Exception {
        bc = context;
        // init and start service tracker
        tracker = new ServiceTracker(bc, CommandProvider.class.getName(), new CommandProviderTrackerCustomizer());
        tracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        // close service tracker
        tracker.close();
        tracker = null;

        bc = null;
    }

    private class CommandProviderTrackerCustomizer implements ServiceTrackerCustomizer {
        public Object addingService(ServiceReference reference) {
            Object commandProvider = bc.getService(reference);
            // create GoGo service based on CommandProvider service
            ShellInfo shellInfo = Utils.createGogoService((CommandProvider) commandProvider);
            if (shellInfo != null) {
                try {
                    // create an instance of GoGo service
                    Object instance = shellInfo.getShellClass().getConstructor(CommandProvider.class).newInstance(commandProvider);
                    // set service properties: scope and list of available commands
                    Dictionary<String, Object> props = new Hashtable<String, Object>();
                    props.put(CommandProcessor.COMMAND_SCOPE, shellInfo.getScope());
                    props.put(CommandProcessor.COMMAND_FUNCTION, shellInfo.getCommands());
                    // register service
                    ServiceRegistration registration = bc.registerService(instance.getClass().getName(), instance, props);
                    registrations.put(reference, registration);

                    LOG.log(Level.INFO, String.format("GoGo shell for class: %s registered", commandProvider.getClass()));
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Unable to register GoGo shell for class: " + commandProvider.getClass(), e);
                }
            }
            return commandProvider;
        }

        public void modifiedService(ServiceReference reference, Object service) {
            // do nothing
        }

        public void removedService(ServiceReference reference, Object service) {
            ServiceRegistration registration = registrations.get(reference);
            if (registration != null) {
                // unregister service
                String className = (String) registration.getReference().getProperty(Constants.OBJECTCLASS);
                registration.unregister();
                Utils.clean(className);
                LOG.log(Level.INFO, String.format("GoGo shell for class: %s unregistered", service.getClass()));
            }
        }
    }
}
