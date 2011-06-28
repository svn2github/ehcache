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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

/**
 * Some useful information about this Runtime for us to do meaningful sizeOf measurements
 *
 * @author Chris Dennis
 */
public final class JvmInformation {

    /**
     * Size of a pointer in bytes on this runtime
     */
    public static final int POINTER_SIZE;
    /**
     * Size of a java pointer in bytes on this runtime (that differs when compressedOops are being used)
     */
    public static final int JAVA_POINTER_SIZE;
    /**
     * Minimal size an object will occupy on the heap
     */
    public static final int MINIMUM_OBJECT_SIZE;
    /**
     * Value when no padding is being done by the GC
     *
     * @see #MINIMUM_OBJECT_SIZE
     */
    public static final int OBJECT_ALIGNMENT = 8;

    static {
        if (is64Bit()) {
            if (isHotspotCompressedOops()) {
                POINTER_SIZE = 8;
                JAVA_POINTER_SIZE = 4;
            } else {
                POINTER_SIZE = 8;
                JAVA_POINTER_SIZE = 8;
            }
        } else {
            POINTER_SIZE = 4;
            JAVA_POINTER_SIZE = 4;
        }

        if (isHotspotConcurrentMarkSweepGC()) {
            /*
            * Hotpot's CMS garbage collector pads objects to ensure they are big enough
            * for it's free-chunk metadata to be stored inside the space left unoccupied
            * by the free of a minimally sized object.  Helpfully enough it doesn't tell
            * tell the java.lang.instrumentation code about this.
            */
            if (is64Bit()) {
                MINIMUM_OBJECT_SIZE = 24;
            } else {
                MINIMUM_OBJECT_SIZE = 16;
            }
        } else {
            MINIMUM_OBJECT_SIZE = OBJECT_ALIGNMENT;
        }
    }

    private JvmInformation() {

    }

    private static boolean is64Bit() {
        String systemProp;
        systemProp = System.getProperty("com.ibm.vm.bitmode");
        if (systemProp != null) {
            return systemProp.equals("64");
        }
        systemProp = System.getProperty("sun.arch.data.model");
        if (systemProp != null) {
            return systemProp.equals("64");
        }
        systemProp = System.getProperty("java.vm.version");
        if (systemProp != null) {
            return systemProp.contains("_64");
        }
        return false;
    }

    private static boolean isHotspotCompressedOops() {
        String value = getVmOptionValue("UseCompressedOops");
        if (value == null) {
            return false;
        } else {
            return Boolean.valueOf(value);
        }
    }

    private static boolean isHotspotConcurrentMarkSweepGC() {
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if ("ConcurrentMarkSweep".equals(bean.getName())) {
                return true;
            }
        }
        return false;
    }

    private static String getVmOptionValue(String name) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName beanName = ObjectName.getInstance("com.sun.management:type=HotSpotDiagnostic");
            Object vmOption = server.invoke(beanName, "getVMOption", new Object[] {name}, new String[] {"java.lang.String"});
            return (String)((CompositeData)vmOption).get("value");
        } catch (Throwable t) {
            return null;
        }
    }
}
