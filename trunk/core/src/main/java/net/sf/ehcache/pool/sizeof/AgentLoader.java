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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.config.MemoryUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will try to load the agent using the Attach API of JDK6.
 * If you are on an older JDK (v5) you can still use the agent by adding the -javaagent:[pathTojar] to your VM
 * startup script
 *
 * @author Alex Snaps
 */
final class AgentLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentLoader.class);

    private static final String VIRTUAL_MACHINE_CLASSNAME = "com.sun.tools.attach.VirtualMachine";
    private static final Method VIRTUAL_MACHINE_ATTACH;
    private static final Method VIRTUAL_MACHINE_DETACH;
    private static final Method VIRTUAL_MACHINE_LOAD_AGENT;

    static {
        Method attach = null;
        Method detach = null;
        Method loadAgent = null;
        try {
            Class<?> virtualMachineClass = getVirtualMachineClass();
            attach = virtualMachineClass.getMethod("attach", String.class);
            detach = virtualMachineClass.getMethod("detach");
            loadAgent = virtualMachineClass.getMethod("loadAgent", String.class);
        } catch (Throwable e) {
            LOGGER.info("Failed to locate dynamic agent loading classes or methods, {}: {} - Sizes will be guessed",
                e.getClass().getName(), e.getMessage());
        }
        VIRTUAL_MACHINE_ATTACH = attach;
        VIRTUAL_MACHINE_DETACH = detach;
        VIRTUAL_MACHINE_LOAD_AGENT = loadAgent;
    }

    private static Class<?> getVirtualMachineClass() throws ClassNotFoundException {
        try {
            return Class.forName(VIRTUAL_MACHINE_CLASSNAME);
        } catch (ClassNotFoundException cnfe) {
            for (File jar : getPossibleToolsJars()) {
                try {
                    Class<?> vmClass = new URLClassLoader(new URL[] {jar.toURL()}).loadClass(VIRTUAL_MACHINE_CLASSNAME);
                    LOGGER.info("Located valid 'tools.jar' at '{}'", jar);
                    return vmClass;
                } catch (Throwable t) {
                    LOGGER.info("Exception while loading tools.jar from '{}': {}", jar, t);
                }
            }
            throw new ClassNotFoundException(VIRTUAL_MACHINE_CLASSNAME);
        }
    }

    private static List<File> getPossibleToolsJars() {
        List<File> jars = new ArrayList<File>();

        File javaHome = new File(System.getProperty("java.home"));
        File jreSourced = new File(javaHome, "lib/tools.jar");
        if (jreSourced.exists()) {
            jars.add(jreSourced);
        }
        if ("jre".equals(javaHome.getName())) {
            File jdkHome = new File(javaHome, "../");
            File jdkSourced = new File(jdkHome, "lib/tools.jar");
            if (jdkSourced.exists()) {
                jars.add(jdkSourced);
            }
        }
        return jars;
    }

    /**
     * Attempts to load the agent through the Attach API
     * @return true if agent was loaded (which could have happened thought the -javaagent switch)
     */
    static boolean loadAgent() {

        if (VIRTUAL_MACHINE_LOAD_AGENT == null) {
            return false;
        }

        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            Object vm = VIRTUAL_MACHINE_ATTACH.invoke(null, name.substring(0, name.indexOf('@')));
            try {
                File agent = getAgentFile();
                LOGGER.info("Trying to load agent @ {}", agent);
                if (agent != null) {
                    VIRTUAL_MACHINE_LOAD_AGENT.invoke(vm, agent.getAbsolutePath());
                }
            } finally {
                VIRTUAL_MACHINE_DETACH.invoke(vm);
            }
            if (!agentIsAvailable()) {
                System.err.println("Hitting a classloader issue while loading the agent it seems. It got loaded, "
                                   + "we didn't get the Instrumentation instance injected on this SizeOfAgent class instance ?!");
            }
        } catch (Throwable e) {
            LOGGER.info("Failed to attach to VM and load the agent: {}: {} - sizes will be guessed", e.getClass(), e.getMessage());
        }

        return agentIsAvailable();
    }

    private static File getAgentFile() throws IOException {
        URL agent = AgentLoader.class.getResource("sizeof-agent.jar");
        if (agent == null) {
            return null;
        } else if (agent.getProtocol().equals("file")) {
            return new File(agent.getFile());
        } else {
            File temp = File.createTempFile("ehcache-sizeof-agent", ".jar");
            try {
                FileOutputStream fout = new FileOutputStream(temp);
                try {
                    InputStream in = agent.openStream();
                    try {
                        byte[] buffer = new byte[(int)MemoryUnit.KILOBYTES.toBytes(1)];
                        while (true) {
                            int read = in.read(buffer);
                            if (read < 0) {
                                break;
                            } else {
                                fout.write(buffer, 0, read);
                            }
                        }
                    } finally {
                        in.close();
                    }
                } finally {
                    fout.close();
                }
            } finally {
                temp.deleteOnExit();
            }
            LOGGER.info("Extracted agent jar to temporary file {}", temp);
            return temp;
        }
    }

    /**
     * Checks whether the agent is available
     * @return true if available
     */
    static boolean agentIsAvailable() {
        try {
            return SizeOfAgent.isAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Returns the size of this Java object as calculated by the loaded agent.
     *
     * @param obj object to be sized
     * @return size of the object in bytes
     */
    static long agentSizeOf(Object obj) {
        return SizeOfAgent.sizeOf(obj);
    }
}
