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
package net.sf.ehcache.config;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.store.DefaultElementValueComparator;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.util.ClassLoaderUtil;

/**
 * @author Ludovic Orban
 */
public class ElementValueComparatorConfiguration {

    private volatile String className = DefaultElementValueComparator.class.getName();

    /**
     * Returns the fully qualified class name for the ElementValueComparator to use
     * 
     * @return FQCN to the ElementValueComparator implementation to use
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the fully qualified class name for the ElementValueComparator to use
     * 
     * @param className
     *            FQCN
     */
    public void setClass(final String className) {
        this.className = className;
    }

    /**
     * Get (and potentially) instantiate the instance
     * 
     * @param cacheConfiguration the cache configuration
     * @return the instance
     */
    public ElementValueComparator createElementComparatorInstance(CacheConfiguration cacheConfiguration) {
        try {
            return (ElementValueComparator) ClassLoaderUtil.createNewInstance(
                className,
                new Class[] {CacheConfiguration.class},
                new Object[] {cacheConfiguration}
            );
        } catch (ClassCastException cce) {
            throw new CacheException(className + " must implement " + ElementValueComparator.class.getName(), cce);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ElementValueComparatorConfiguration other = (ElementValueComparatorConfiguration) obj;
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        return true;
    }

}
