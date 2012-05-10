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
package net.sf.ehcache.constructs.nonstop.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A utility class to check whether one class overrides all methods of its superclass or an interface that it implements
 * 
 * @author Abhishek Sanoujam
 * 
 */
public final class OverrideCheck {

    /**
     * private constructor
     */
    private OverrideCheck() {
        //
    }

    /**
     * Method to check a subclass overrides all methods in its superclass or the interface it implements
     * 
     * @param parent
     * @param subClass
     */
    public static void check(Class parent, Class subClass) {
        boolean excludeSuper = parent.isAssignableFrom(subClass);

        Set<String> superMethods = methodsFor(parent, false);
        Set<String> subMethods = methodsFor(subClass, excludeSuper);

        List<String> missing = new ArrayList();

        for (String method : superMethods) {

            if (!subMethods.contains(method)) {
                // This class should be overriding all methods on the super class
                missing.add(method);
            }
        }

        if (!missing.isEmpty()) {
            throw new RuntimeException(subClass.getName() + " is missing overrides (defined in " + parent.getName() + "):\n" + missing);
        }
    }

    private static Set<String> methodsFor(final Class klass, final boolean excludeSuper) {
        Set<String> set = new HashSet();
        Class currClass = klass;
        while (currClass != null && currClass != Object.class) {
            Method[] methods = currClass.isInterface() ? currClass.getMethods() : currClass.getDeclaredMethods();

            for (Method m : methods) {
                int access = m.getModifiers();

                if (Modifier.isStatic(access) || Modifier.isPrivate(access)) {
                    continue;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(m.getName()).append('(');

                Class[] parameterTypes = m.getParameterTypes();
                for (int j = 0; j < parameterTypes.length; j++) {
                    sb.append(parameterTypes[j].getName());
                    if (j < (parameterTypes.length - 1)) {
                        sb.append(',');
                    }
                }
                sb.append(')');

                set.add(sb.toString());

            }

            if (excludeSuper) {
                return set;
            }

            currClass = currClass.getSuperclass();
        }

        return set;
    }
}
