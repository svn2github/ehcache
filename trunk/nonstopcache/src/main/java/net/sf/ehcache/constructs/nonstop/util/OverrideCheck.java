/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.constructs.nonstop.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OverrideCheck {

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
            throw new RuntimeException("Missing overrides:\n" + missing);
        }
    }

    private static Set<String> methodsFor(Class c, boolean excludeSuper) {
        Set<String> set = new HashSet();

        while (c != null && c != Object.class) {
            Method[] methods = c.isInterface() ? c.getMethods() : c.getDeclaredMethods();

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

            c = c.getSuperclass();
        }

        return set;
    }
}
