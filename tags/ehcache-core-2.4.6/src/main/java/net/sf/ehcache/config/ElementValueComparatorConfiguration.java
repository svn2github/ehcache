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
package net.sf.ehcache.config;

import net.sf.ehcache.store.ElementValueComparator;

/**
 * @author Ludovic Orban
 */
public class ElementValueComparatorConfiguration {

    private volatile String className = "net.sf.ehcache.store.DefaultElementValueComparator";
    private ElementValueComparator comparator;

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
     * @return the instance
     */
    public synchronized ElementValueComparator getElementComparatorInstance() {
        if (comparator == null) {
            Class elementComparator = null;
            try {
                elementComparator = Class.forName(className);
                comparator = (ElementValueComparator) elementComparator.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Couldn't find the ElementValueComparator class!", e);
            } catch (InstantiationException e) {
                throw new RuntimeException("Couldn't instantiate the ElementValueComparator instance!", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Couldn't instantiate the ElementValueComparator instance!", e);
            } catch (ClassCastException e) {
                throw new RuntimeException(elementComparator != null ? elementComparator.getSimpleName()
                        + " doesn't implement net.sf.ehcache.store.ElementValueComparator" : "Error with ElementValueComparator", e);
            }
        }
        return comparator;
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
