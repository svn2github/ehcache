/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.management;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * ManagementServerLoader is a facility class to access the rest management interface
 * One would use it form EhCache or QuartzScheduler to start or stop a new rest management interface
 * or to simply register a cache or a scheduler to an already started management interface.
 * 
 * It uses internally a ResourceClassLoader to load classes from a rest agent jar.
 * 
 * @author Anthony Dahanne
 * 
 */
public class ManagementServerLoader {

    private static final String PRIVATE_CLASSPATH = "rest-management-private-classpath";
    private static final Map<String, Object> MGMT_SVR_BY_BIND = new HashMap<String, Object>();

    private static final ClassLoader RESOURCE_CLASS_LOADER;
    private static final Logger LOG = LoggerFactory.getLogger(ManagementServerLoader.class);

    static {
        URL depsResource = DevModeClassLoader.devModeResource();
        if (depsResource != null) {
            RESOURCE_CLASS_LOADER = new DevModeClassLoader(depsResource, CacheManager.class.getClassLoader());
        } else {
            RESOURCE_CLASS_LOADER = new ResourceClassLoader(PRIVATE_CLASSPATH, CacheManager.class.getClassLoader());
        }
        LOG.debug("XXX: using classloader: " + RESOURCE_CLASS_LOADER);
    }

    /**
     * Check if the ehcache-rest-agent jar is on the classpath
     * 
     * @return true if ehcache-rest-agent is available, false otherwise.
     */
    public static boolean isManagementAvailable() {
        try {
            RESOURCE_CLASS_LOADER.loadClass("net.sf.ehcache.management.ManagementServerImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Register a cacheManager to management rest server.
     * If the server does not exist, starts it.
     * 
     * @param cacheManager
     * @param managementRESTServiceConfiguration
     */
    public static void register(CacheManager cacheManager, String clientUUID,
            ManagementRESTServiceConfiguration managementRESTServiceConfiguration) {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // because some code in Jersey is using the TCCL to resolve some classes
            Thread.currentThread().setContextClassLoader(RESOURCE_CLASS_LOADER);

            Class<?> managementServerImplClass = RESOURCE_CLASS_LOADER.loadClass("net.sf.ehcache.management.ManagementServerImpl");
            Object managementServerImpl = null;
            if (!MGMT_SVR_BY_BIND.containsKey(managementRESTServiceConfiguration.getBind())) {
                if (!MGMT_SVR_BY_BIND.isEmpty()) {
                    String alreadyBound = MGMT_SVR_BY_BIND.keySet().iterator().next();
                    managementRESTServiceConfiguration.setBind(alreadyBound);
                    LOG.warn("You can not have several Ehcache management rest agents running in the same ClassLoader; CacheManager "
                            + cacheManager.getName()
                            + " will be registered to the already running Ehcache management rest agent listening on port " + alreadyBound
                            + ", the configuration will not be changed");
                } else {
                    startRestAgent(managementRESTServiceConfiguration, managementServerImplClass, clientUUID);
                }
            } else {
                LOG.warn("A previous CacheManager already instanciated the Ehcache Management rest agent, on port "
                        + managementRESTServiceConfiguration.getBind() + ", the configuration will not be changed for "
                        + cacheManager.getName());
            }
            managementServerImpl = MGMT_SVR_BY_BIND.get(managementRESTServiceConfiguration.getBind());
            Method registerMethod = managementServerImplClass.getMethod("register", new Class[] {cacheManager.getClass()});
            registerMethod.invoke(managementServerImpl, cacheManager);

        } catch (Exception e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                throw new RuntimeException(
                        "Failed to initialize the ManagementRESTService - Did you include ehcache-rest-agent on the classpath?", e);
            } else {
                throw new RuntimeException("Failed to instantiate ManagementServer.", e);
            }
        } finally {
            // setting back the appClassLoader as the TCCL
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private static void startRestAgent(ManagementRESTServiceConfiguration managementRESTServiceConfiguration,
            Class<?> managementServerImplClass, String clientUUID) throws Exception {
        Object managementServerImpl;
        Constructor<?> managementServerImplClassConstructor = managementServerImplClass.getConstructor(new Class[] {String.class,
                managementRESTServiceConfiguration.getClass()});
        managementServerImpl = managementServerImplClassConstructor.newInstance(new Object[] {clientUUID,
                managementRESTServiceConfiguration});
        Method startMethod = managementServerImplClass.getMethod("start", new Class[] {});
        startMethod.invoke(managementServerImpl, new Object[] {});
        MGMT_SVR_BY_BIND.put(managementRESTServiceConfiguration.getBind(), managementServerImpl);

    }

    /**
     * Unregister a cache manager from a management rest server
     * If it is the last cache manager bound to this server, stops the server too.
     * 
     * @param registeredMgmtSvrBind
     * @param cacheManager
     */
    public static void unregister(String registeredMgmtSvrBind, CacheManager cacheManager) {
        Object managementServerImpl = MGMT_SVR_BY_BIND.get(registeredMgmtSvrBind);

        Class<?> managementServerImplClass;
        boolean removeMgmtSvr = false;
        try {
            managementServerImplClass = RESOURCE_CLASS_LOADER.loadClass("net.sf.ehcache.management.ManagementServerImpl");
            Method registerMethod = managementServerImplClass.getMethod("unregister", new Class[] {cacheManager.getClass()});
            registerMethod.invoke(managementServerImpl, cacheManager);

            Method hasRegisteredMethod = managementServerImplClass.getMethod("hasRegistered", new Class[] {});
            Boolean hasRegistered = (Boolean) hasRegisteredMethod.invoke(managementServerImpl, new Object[] {});

            // there are no more cacheManagers registered to the rest agent, we can now stop it
            if (!hasRegistered) {
                removeMgmtSvr = true;
                Method stopMethod = managementServerImplClass.getMethod("stop", new Class[] {});
                stopMethod.invoke(managementServerImpl, new Object[] {});
            }

        } catch (Exception e) {
            LOG.warn("Failed to shutdown the ManagementRESTService", e);
        } finally {
            if (removeMgmtSvr) {
                MGMT_SVR_BY_BIND.remove(registeredMgmtSvrBind);
            }
        }

    }

}
