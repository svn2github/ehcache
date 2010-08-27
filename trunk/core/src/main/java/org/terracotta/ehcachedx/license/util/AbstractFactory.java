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
package org.terracotta.ehcachedx.license.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Abstract class to load factory files indicated in META-INF/services
 * 
 * @author hhuynh
 * 
 */
public abstract class AbstractFactory {

    /**
     * 
     * @param id
     *            the file that contains the runtime factory classname, found under META-INF/services
     * @param defaultImpl
     *            if runtime factory isn't found, fall back on the default impl
     * @return the abstract factory instance
     */
    public static AbstractFactory getFactory(String id, Class defaultImpl) {
        String factoryClassName = findFactoryClassName(id);
        AbstractFactory factory = null;

        if (factoryClassName != null) {
            try {
                factory = (AbstractFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate '" + factoryClassName + "'", e);
            }
        }

        if (factory == null) {
            try {
                factory = (AbstractFactory) defaultImpl.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return factory;
    }

    private static String findFactoryClassName(String id) {
        String serviceId = "META-INF/services/" + id;
        InputStream is = null;

        ClassLoader cl = AbstractFactory.class.getClassLoader();
        if (cl != null) {
            is = cl.getResourceAsStream(serviceId);
        }

        if (is == null) {
            return null;
        }

        BufferedReader rd;
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is));
        }

        String factoryClassName = null;
        try {
            factoryClassName = rd.readLine();
            rd.close();
        } catch (IOException e) {
            throw new RuntimeException("factory " + id + " was not found in resource", e);
        }

        return factoryClassName;
    }
}
