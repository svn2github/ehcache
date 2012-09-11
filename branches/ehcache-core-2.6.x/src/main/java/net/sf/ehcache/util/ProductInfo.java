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

package net.sf.ehcache.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.sf.ehcache.CacheException;

/**
 * Build properties of the product
 *
 * @author hhuynh
 *
 */
public class ProductInfo {
    private static final String BIGMEMORY_VERSION_RESOURCE = "/org/terracotta/bigmemory/version.properties";
    private static final String EHCACHE_VERSION_RESOURCE = "/net/sf/ehcache/version.properties";
    private static final String UNKNOWN = "UNKNOWN";
    private final Properties props = new Properties();

    /**
     * Construct a default product info
     */
    public ProductInfo() {
        if (ProductInfo.class.getResource(BIGMEMORY_VERSION_RESOURCE) != null) {
            parseProductInfo(BIGMEMORY_VERSION_RESOURCE);
        } else {
            parseProductInfo(EHCACHE_VERSION_RESOURCE);
        }
    }

    /**
     * Construct product info object from a resource name
     *
     * @param resource
     */
    public ProductInfo(String resource) {
        parseProductInfo(resource);
    }

    /**
     * Construct product info object from a resource input stream
     *
     * @param resource
     * @throws java.io.IOException
     */
    public ProductInfo(InputStream resource) {
        try {
            props.load(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (resource != null) {
                try {
                    resource.close();
                } catch (IOException e) {
                    //
                }
            }
        }
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
        } finally {
            try {
                in.close();
            } catch (IOException e2) {
                // ignore
            }
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
     * @return the hostname
     */
    public String getBuildHostname() {
        return props.getProperty("build-hostname", UNKNOWN);
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
     *
     * @return required core version
     */
    public String getRequiredCoreVersion() {
        return props.getProperty("required-core-version");
    }

    /**
     *
     * @return true if the current product is an enterprise one
     */
    public boolean isEnterprise() {
        return Boolean.parseBoolean(props.getProperty("enterprise"));
    }

    /**
     * Assert that the current product is compatible with the version of ehcache available on the classpath
     */
    public void assertRequiredCoreVersionPresent() {
        boolean ignoreVersionCheck = Boolean.getBoolean("terracotta.ehcache.versioncheck.skip");
        String requiredCoreVersion = getRequiredCoreVersion();
        if (ignoreVersionCheck || requiredCoreVersion == null) {
            // no requirement
            return;
        }

        ProductInfo coreProductInfo = new ProductInfo();

        String coreVersion = coreProductInfo.getVersion();
        if (!coreVersion.equals(requiredCoreVersion)) {
            String msg = getName() + " version [" + getVersion() + "] only works with ehcache-core version [" + requiredCoreVersion
                    + "] (found version [" + coreVersion + "] on the classpath). " + " Please make sure both versions are compatible!";
            throw new CacheException(msg);
        }
    }

    /**
     * returns long version of the build string
     */
    @Override
    public String toString() {
        String versionString = String.format("%s version %s was built on %s, at revision %s, with jdk %s by %s@%s", getName(),
                getVersion(), getBuildTime(), getBuildRevision(), getBuildJdk(), getBuiltBy(), getBuildHostname());
        if (!UNKNOWN.equals(getPatchLevel())) {
            versionString = versionString + ". Patch level " + getPatchLevel();
        }
        return versionString;
    }
}
