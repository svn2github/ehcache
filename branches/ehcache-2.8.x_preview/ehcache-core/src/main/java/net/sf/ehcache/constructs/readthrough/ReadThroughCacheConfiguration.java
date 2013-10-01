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

package net.sf.ehcache.constructs.readthrough;

import java.util.Properties;

/**
 * This classed is used to programmatically configure a {@link ReadThroughCache}.
 *
 * @author cschanck
 *
 */
public class ReadThroughCacheConfiguration implements Cloneable {

    /**
     * Properties key for the name attribute
     */
    public static final String NAME_KEY = "name";

    /**
     * Properties key for the get() proxy attribute
     */
    public static final String GET_KEY = "get";

    private boolean modeGet = true;
    private String name = null;
    private volatile boolean valid = false;

    /**
     * Create default, valid configuration.
     */
    public ReadThroughCacheConfiguration() {
        validate();
    }

    /**
     * Create a configuration object from a properties object. Validate before returning.
     *
     * @param properties
     * @return
     */
    public ReadThroughCacheConfiguration fromProperties(Properties properties) {
        valid = false;
        if (properties != null) {
            for (String property : properties.stringPropertyNames()) {
                String stringValue = properties.getProperty(property).trim();
                if (GET_KEY.equals(property)) {
                    setModeGet(Boolean.parseBoolean(stringValue));
                } else if (NAME_KEY.equals(property)) {
                    setName(stringValue);
                } else {
                    throw new IllegalArgumentException("Unrecognized ReadThrough cache config key: " + property);
                }
            }
        }
        return build();
    }

    /**
     * Return a properties version of this configuration object.
     *
     * @return
     */
    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty(NAME_KEY, getName());
        p.setProperty(GET_KEY, Boolean.toString(isModeGet()));
        return p;
    }

    /**
     * Validate this configuration, mark it valid if it passes.
     *
     * @return
     */
    public ReadThroughCacheConfiguration build() {
        validate();
        return this;
    }

    private void validate() {
        // not much here.
        // removing the decorator.
        valid = true;
    }

    private void checkValid() {
        if (!valid) {
            throw new IllegalStateException("RefreshAheadCacheConfig not built yet");
        }
    }

    /**
     * Set whether get() methods will be transparently proxied.
     *
     * @param modeGet true to proxy get methods
     * @return
     */
    public ReadThroughCacheConfiguration modeGet(boolean modeGet) {
        setModeGet(modeGet);
        return this;
    }

    /**
     * Return whether the get() method is proxied.
     *
     * @return true if proxied
     */
    public boolean isModeGet() {
        checkValid();
        return modeGet;
    }

    /**
     * Set whether get() methods will be transparently proxied.
     *
     * @param modeGet true t pxoy the get() methods
     */
    public void setModeGet(boolean modeGet) {
        valid = false;
        this.modeGet = modeGet;
    }

    /**
     * Get the name to set for this cache decorator. May be null.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name to set for this cache decorator. May be null.
     *
     * @param name name to use, may be null
     * @return this config
     */
    public ReadThroughCacheConfiguration setName(String name) {
        valid = false;
        this.name = name;
        return this;
    }

    /**
     * Set the name to set for this cache decorator. May be null.
     *
     * @param name name to use, may be null
     * @return this config
     */
    public ReadThroughCacheConfiguration name(String name) {
        setName(name);
        return this;
    }

    @Override
    public String toString() {
        return toProperties().toString();
    }


}
