/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build properties of the product
 * 
 * @author hhuynh
 * 
 */
public class ProductInfo {
    private static final String VERSION_RESOURCE = "/ehcache-version.properties";
    private static final String UNKNOWN = "UNKNOWN";
    private Properties props = new Properties();

    /**
     * Construct a default product info
     */
    public ProductInfo() {
        this(VERSION_RESOURCE);
    }

    /**
     * Construct product info object from a resource
     * 
     * @param resource
     */
    public ProductInfo(String resource) {
        parseProductInfo(resource);
    }

    private void parseProductInfo(String resource) {
        InputStream in = ProductInfo.class.getResourceAsStream(resource);
        if (in == null) {
            throw new RuntimeException("Can't find resource: " + resource);
        }

        try {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * @return product name
     */
    public String getName() {
        return props.getProperty("product-name", UNKNOWN);
    }

    /**
     * 
     * @return version
     */
    public String getVersion() {
        return props.getProperty("version", UNKNOWN);
    }

    /**
     * 
     * @return the person who built
     */
    public String getBuiltBy() {
        return props.getProperty("built-by", UNKNOWN);
    }

    /**
     * 
     * @return jdk that was used
     */
    public String getBuildJdk() {
        return props.getProperty("build-jdk", UNKNOWN);
    }

    /**
     * 
     * @return build timestamp
     */
    public String getBuildTime() {
        return props.getProperty("build-time", UNKNOWN);
    }

    /**
     * 
     * @return revision
     */
    public String getBuildRevision() {
        return props.getProperty("build-revision", UNKNOWN);
    }

    /**
     * 
     * @return patch number
     */
    public String getPatchLevel() {
        return props.getProperty("patch-level", UNKNOWN);
    }

    /**
     * returns long version of the build string
     */
    @Override
    public String toString() {
        String versionString = String
                .format(
                        "%s version %s was built on %s, at revision %s, with jdk %s by %s",
                        getName(), getVersion(), getBuildTime(),
                        getBuildRevision(), getBuildJdk(), getBuiltBy());
        if (!UNKNOWN.equals(getPatchLevel())) {
            versionString = versionString + ". Patch level " + getPatchLevel();
        }
        return versionString;
    }
}
