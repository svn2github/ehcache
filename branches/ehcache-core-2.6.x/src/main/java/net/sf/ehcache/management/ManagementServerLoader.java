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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagementServerLoader {

    private static final String PRIVATE_CLASSPATH = "rest-management-private-classpath";
    private static final Map<String, Object> MGMT_SVR_BY_BIND = new HashMap<String, Object>();

    private static final ResourceClassLoader resourceClassLoader;
    private static final Logger LOG = LoggerFactory.getLogger(ManagementServerLoader.class);


    static {
        try {
            resourceClassLoader = new ResourceClassLoader(PRIVATE_CLASSPATH, CacheManager.class.getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException("Failed to instantiate ManagementServer.", e);
        }
    }


    public static void register(CacheManager cacheManager, ManagementRESTServiceConfiguration managementRESTServiceConfiguration) {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // because some code in Jersey is using the TCCL to resolve some classes
            Thread.currentThread().setContextClassLoader(resourceClassLoader);

            Class<?> managementServerImplClass = resourceClassLoader.loadClass("net.sf.ehcache.management.ManagementServerImpl");
            Object managementServerImpl = null;
            if (!MGMT_SVR_BY_BIND.containsKey(managementRESTServiceConfiguration.getBind())) {
                Constructor<?> managementServerImplClassConstructor = managementServerImplClass
                        .getConstructor(new Class[] {managementRESTServiceConfiguration.getClass()});
                managementServerImpl = managementServerImplClassConstructor
                        .newInstance(new Object[] {managementRESTServiceConfiguration});
                Method startMethod = managementServerImplClass.getMethod("start", new Class[] {});
                startMethod.invoke(managementServerImpl, new Object[] {});
                MGMT_SVR_BY_BIND.put(managementRESTServiceConfiguration.getBind(), managementServerImpl);
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
        }

        finally {
            // setting back the appClassLoader as the TCCL
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    public static void unregister(String registeredMgmtSvrBind, CacheManager cacheManager) {
        Object managementServerImpl = MGMT_SVR_BY_BIND.get(registeredMgmtSvrBind);

        Class<?> managementServerImplClass;
        boolean removeMgmtSvr = false;
        try {
            managementServerImplClass = resourceClassLoader.loadClass("net.sf.ehcache.management.ManagementServerImpl");
            Method registerMethod = managementServerImplClass.getMethod("unregister", new Class[] {cacheManager.getClass()});
            registerMethod.invoke(managementServerImpl, cacheManager);

            Method hasRegisteredMethod = managementServerImplClass.getMethod("hasRegistered", new Class[] {});
            Boolean hasRegistered = (Boolean) hasRegisteredMethod.invoke(managementServerImpl, new Object[] {});

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
