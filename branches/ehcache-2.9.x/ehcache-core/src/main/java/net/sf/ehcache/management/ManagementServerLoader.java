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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * ManagementServerLoader is a facility class to access the rest management interface
 * One would use it from EhCache or QuartzScheduler to start or stop a new rest management interface
 * or to simply register a cache or a scheduler to an already started management interface.
 * <p/>
 * It uses internally a ResourceClassLoader to load classes from a rest agent jar.
 *
 * @author Anthony Dahanne
 */
public class ManagementServerLoader {

    static final Map<String, ManagementServerHolder> MGMT_SVR_BY_BIND = new HashMap<String, ManagementServerHolder>();

    private static final String PRIVATE_CLASSPATH = "rest-management-private-classpath";
    private static final Class<?> MANAGEMENT_SERVER_CLASS;

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

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> managementServerClass = null;
        try {
            // because some code in Jersey is using the TCCL to resolve some classes
            Thread.currentThread().setContextClassLoader(RESOURCE_CLASS_LOADER);
            managementServerClass = RESOURCE_CLASS_LOADER.loadClass("net.sf.ehcache.management.ManagementServer");
        } catch (Exception e) {
            managementServerClass = null;
            if (e.getCause() instanceof ClassNotFoundException) {
                LOG.warn("Failed to initialize the ManagementRESTService - Did you include ehcache-rest-agent on the classpath?", e);
            } else {
                LOG.warn("Failed to load ManagementServer class. Management agent will not be available.", e);
            }
        } finally {
            // setting back the appClassLoader as the TCCL
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            MANAGEMENT_SERVER_CLASS = managementServerClass;
        }
    }

    /**
     * Check if the ehcache-rest-agent jar is on the classpath
     *
     * @return true if ehcache-rest-agent is available, false otherwise.
     */
    public static boolean isManagementAvailable() {
        try {
            ServiceLoader loader = ServiceLoader.load(ManagementServer.class, RESOURCE_CLASS_LOADER);
            Iterator loaderIterator = loader.iterator();
            if (loaderIterator.hasNext()) {
                return true;
            }
        } catch (Exception e) {
            LOG.warn("Unable to load META-INF/services/net.sf.ehcache.management.ManagementServer ; the management" +
                     " agent won't be available");
        }
        return false;
    }

    /**
     * Register a cacheManager to management rest server.
     * If the server does not exist, starts it.
     *
     * @param cacheManager                       the cacheManager to register
     * @param clientUUID                         the client UUID
     * @param managementRESTServiceConfiguration the management configuration
     */
    public static void register(CacheManager cacheManager, String clientUUID,
                                ManagementRESTServiceConfiguration managementRESTServiceConfiguration) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // because some code in Jersey is using the TCCL to resolve some classes
            Thread.currentThread().setContextClassLoader(RESOURCE_CLASS_LOADER);
            if (!MGMT_SVR_BY_BIND.containsKey(managementRESTServiceConfiguration.getBind())) {
                if (!MGMT_SVR_BY_BIND.isEmpty()) {
                    String alreadyBound = MGMT_SVR_BY_BIND.keySet().iterator().next();
                    managementRESTServiceConfiguration.setBind(alreadyBound);
                    LOG.warn("You cannot have several Ehcache management rest agents running in the same ClassLoader; CacheManager "
                             + cacheManager.getName()
                             + " will be registered to the already running Ehcache management rest agent "
                             + (ManagementRESTServiceConfiguration.NO_BIND.equals(managementRESTServiceConfiguration.getBind()) ?
                                "reachable through the TSA agent" : "listening on port " + alreadyBound)
                             + ", the configuration will not be changed for " + cacheManager.getName());
                } else {
                    new ManagementServerHolder(loadOSorEEManagementServer()).start(managementRESTServiceConfiguration);
                }
            } else {
                LOG.warn("Another CacheManager already instantiated the Ehcache Management rest agent" +
                         (ManagementRESTServiceConfiguration.NO_BIND.equals(managementRESTServiceConfiguration.getBind()) ?
                             ", reachable through the TSA agent" : ", on port " + managementRESTServiceConfiguration.getBind()) +
                             ", the configuration will not be changed for " + cacheManager.getName());
            }
            ManagementServerHolder managementServerHolder = MGMT_SVR_BY_BIND.get(managementRESTServiceConfiguration.getBind());
            managementServerHolder.register(cacheManager, clientUUID);
        } catch (Exception e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                throw new RuntimeException(
                    "Failed to initialize the ManagementRESTService - Did you include ehcache-rest-agent on the classpath?", e);
            } else {
                throw new CacheException("Failed to instantiate ManagementServer.", e);
            }
        } finally {
            // setting back the appClassLoader as the TCCL
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * This method instantiates the ManagementServer implementation found in
     * META-INF/services/net.sf.ehcache.management.ManagementServer
     *
     * @return a {@link ManagementServer} instance
     */
    private static Object loadOSorEEManagementServer() throws Exception {
        Object managementServerImpl;
        ServiceLoader loader = ServiceLoader.load(ManagementServer.class, RESOURCE_CLASS_LOADER);

        Iterator loaderIterator = loader.iterator();
        if (!loaderIterator.hasNext()) {
            LOG.info("Could not find any META-INF/services/net.sf.ehcache.management.ManagementServer using the " +
                     "ResourceClassLoader; choosing the default OS implementation : " +
                     "net.sf.ehcache.management.ManagementServerImpl");
            Class<?> managementServerImplClass = RESOURCE_CLASS_LOADER
                .loadClass("net.sf.ehcache.management.ManagementServerImpl");
            Constructor<?> managementServerImplClassConstructor = managementServerImplClass.getConstructor();
            managementServerImpl = managementServerImplClassConstructor.newInstance();
        } else {
            managementServerImpl = loaderIterator.next();
            // more than one file found ? is it even possible ? well across multiple jars, why not..
            if (loaderIterator.hasNext()) {
                throw new RuntimeException("Several META-INF/services/net.sf.ehcache.management.ManagementServer " +
                                           "found in the classpath, aborting agent start up");
            }
            LOG.info("The ManagementServer implementation that is going to be used is {} .",
                managementServerImpl.getClass().toString());
        }
        return managementServerImpl;
    }

    /**
     * Unregister a cache manager from a management rest server
     * If it is the last cache manager bound to this server, stops the server too.
     *
     * @param registeredMgmtSvrBind the bind identifying what to un-register from
     * @param cacheManager          the cacheManager to un-register
     */
    public static void unregister(String registeredMgmtSvrBind, CacheManager cacheManager) {
        ManagementServerHolder managementServerHolder = MGMT_SVR_BY_BIND.get(registeredMgmtSvrBind);

        try {
            managementServerHolder.unregister(cacheManager);

            // there are no more cacheManagers registered to the rest agent, we can now stop it
            if (!managementServerHolder.hasRegistered()) {
                managementServerHolder.stop(registeredMgmtSvrBind);
            }

        } catch (Exception e) {
            LOG.warn("Failed to shutdown the ManagementRESTService", e);
        }
    }


    static final class ManagementServerHolder {
        private Object managementServer;
        private String registeredClientUUID;
        private final Map<String, String> clientUUIDs = new HashMap<String, String>();

        ManagementServerHolder(Object managementServer) {
            this.managementServer = managementServer;
        }

        void start(ManagementRESTServiceConfiguration managementRESTServiceConfiguration) throws Exception {
            Method initializeMethod = MANAGEMENT_SERVER_CLASS.getMethod("initialize", ManagementRESTServiceConfiguration.class);
            initializeMethod.invoke(managementServer, managementRESTServiceConfiguration);

            Method startMethod = MANAGEMENT_SERVER_CLASS.getMethod("start");
            startMethod.invoke(managementServer);

            MGMT_SVR_BY_BIND.put(managementRESTServiceConfiguration.getBind(), this);
        }

        void register(CacheManager cacheManager, String clientUUID) throws Exception {
            Method registerMethod = MANAGEMENT_SERVER_CLASS.getMethod("register", CacheManager.class);
            registerMethod.invoke(managementServer, cacheManager);
            if (clientUUID != null) {
                clientUUIDs.put(cacheManager.getName(), clientUUID);
                Method addClusterUUID = MANAGEMENT_SERVER_CLASS.getMethod("addClientUUID", String.class);
                addClusterUUID.invoke(managementServer, clientUUID);
                Method registerClusterRemoteEndpoint = MANAGEMENT_SERVER_CLASS.getMethod("registerClusterRemoteEndpoint", String.class);
                registerClusterRemoteEndpoint.invoke(managementServer, clientUUID);
            }
        }

        void unregister(CacheManager cacheManager) throws Exception {
            Method unregisterMethod = MANAGEMENT_SERVER_CLASS.getMethod("unregister", CacheManager.class);
            unregisterMethod.invoke(managementServer, cacheManager);

            String unregisteredClientUUID = clientUUIDs.remove(cacheManager.getName());
            Method removeClusterUUID = MANAGEMENT_SERVER_CLASS.getMethod("removeClientUUID", String.class);
            removeClusterUUID.invoke(managementServer, unregisteredClientUUID);
            if (registeredClientUUID != null && registeredClientUUID.equals(unregisteredClientUUID)) {
                Method unregisterClusterRemoteEndpoint = MANAGEMENT_SERVER_CLASS.getMethod("unregisterClusterRemoteEndpoint");
                unregisterClusterRemoteEndpoint.invoke(managementServer);
                Iterator<String> uuidsIt = clientUUIDs.values().iterator();
                if (uuidsIt.hasNext()) {
                    registeredClientUUID = uuidsIt.next();
                    Method registerClusterRemoteEndpoint = MANAGEMENT_SERVER_CLASS.getMethod("registerClusterRemoteEndpoint", String.class);
                    registerClusterRemoteEndpoint.invoke(managementServer, registeredClientUUID);
                } else {
                    registeredClientUUID = null;
                }
            }
        }

        boolean hasRegistered() throws Exception {
            Method hasRegisteredMethod = MANAGEMENT_SERVER_CLASS.getMethod("hasRegistered");
            return (Boolean)hasRegisteredMethod.invoke(managementServer);
        }

        void stop(String bind) throws Exception {
            try {
                Method stopMethod = MANAGEMENT_SERVER_CLASS.getMethod("stop");
                stopMethod.invoke(managementServer);
            } finally {
                MGMT_SVR_BY_BIND.remove(bind);
            }
        }

        public Object getManagementServer() {
            return managementServer;
        }

        public String getRegisteredClientUUID() {
            return registeredClientUUID;
        }

        public Map<String, String> getClientUUIDs() {
            return clientUUIDs;
        }
    }

}
