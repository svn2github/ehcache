/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.pool.sizeof;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.sizeof.SizeOfAgent;

/**
 * This will try to load the agent using the Attach API of JDK6.
 * If you are on an older JDK (v5) you can still use the agent by adding the -javaagent:[pathTojar] to your VM
 * startup script
 *
 * @author Alex Snaps
 */
final class AgentLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentLoader.class);

    private static final Method VIRTUAL_MACHINE_ATTACH;
    private static final Method VIRTUAL_MACHINE_DETACH;
    private static final Method VIRTUAL_MACHINE_LOAD_AGENT;
    static {
        Method attach = null;
        Method detach = null;
        Method loadAgent = null;
        try {
            Class<?> virtualMachineClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            attach = virtualMachineClass.getMethod("attach", String.class);
            detach = virtualMachineClass.getMethod("detach");
            loadAgent = virtualMachineClass.getMethod("loadAgent", String.class);
        } catch (Throwable e) {
            LOGGER.info("Failed to locate dynamic agent loading classes or methods", e);
        }
        VIRTUAL_MACHINE_ATTACH = attach;
        VIRTUAL_MACHINE_DETACH = detach;
        VIRTUAL_MACHINE_LOAD_AGENT = loadAgent;
    }

    /**
     * Attempt to
     * @return
     */
    static boolean loadAgent() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            Object vm = VIRTUAL_MACHINE_ATTACH.invoke(null, name.substring(0, name.indexOf('@')));
            try {
                String agentLocation = jarFor(SizeOfAgent.class);
                File file = new File(agentLocation);
                if (file.isDirectory()) {
                    // hack for maven layout!
                    agentLocation = agentLocation + ".." + File.separatorChar + "sizeOfAgent.jar";
                }
                VIRTUAL_MACHINE_LOAD_AGENT.invoke(vm, agentLocation);
            } finally {
                VIRTUAL_MACHINE_DETACH.invoke(vm);
            }
            if (!SizeOfAgent.isAvailable()) {
                System.err.println("Hitting a classloader issue while loading the agent it seems. It got loaded, "
                        + "we didn't get the Instrumentation instance injected on this SizeOfAgent class instance ?!");
            }
        } catch (Throwable e) {
            LOGGER.info("Failed to load agent, sizes will be guessed", e);
        }
        return SizeOfAgent.isAvailable();
    }

    /**
     * Returns the size of this Java object as calculated by the loaded agent.
     *
     * @param obj object to be sized
     * @return size of the object in bytes
     */
    static long sizeOf(final Object obj) {
        return SizeOfAgent.sizeOf(obj);
    }

    private static String jarFor(Class<?> c) {
        ProtectionDomain protectionDomain = c.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL url = codeSource.getLocation();
        String path = url.getPath();
        if (System.getProperty("os.name", "unknown").toLowerCase().indexOf("windows") >= 0 && path.startsWith("/")) {
            path = path.substring(1);
        }
        return URLDecoder.decode(path);
    }

}
