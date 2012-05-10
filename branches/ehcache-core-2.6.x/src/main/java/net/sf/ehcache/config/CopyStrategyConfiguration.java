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

import net.sf.ehcache.Element;
import net.sf.ehcache.store.compound.CopyStrategy;
import net.sf.ehcache.store.compound.LegacyCopyStrategyAdapter;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.util.ClassLoaderUtil;

/**
 * @author Alex Snaps
 */
public class CopyStrategyConfiguration {

    private volatile String className = "net.sf.ehcache.store.compound.ReadWriteSerializationCopyStrategy";
    private ReadWriteCopyStrategy<Element> strategy;

    /**
     * Returns the fully qualified class name for the CopyStrategy to use
     * 
     * @return FQCN to the CopyStrategy implementation to use
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the fully qualified class name for the CopyStrategy to use
     * 
     * @param className
     *            FQCN
     */
    public void setClass(final String className) {
        this.className = className;
    }

    /**
     * Sets the CopyStrategy instance to use
     *
     * @param copyStrategy the copy strategy
     */
    public synchronized void setCopyStrategyInstance(ReadWriteCopyStrategy<Element> copyStrategy) {
        this.strategy = copyStrategy;
    }

    /**
     * Get (and potentially) instantiate the instance
     * 
     * @return the instance
     */
    public synchronized ReadWriteCopyStrategy<Element> getCopyStrategyInstance() {
        if (strategy == null) {
            Class copyStrategy = null;
            try {
                copyStrategy = ClassLoaderUtil.loadClass(className);
                Object strategyObject = copyStrategy.newInstance();
                if (strategyObject instanceof CopyStrategy) {
                    strategy = new LegacyCopyStrategyAdapter((CopyStrategy) strategyObject);
                } else {
                    strategy = (ReadWriteCopyStrategy<Element>) strategyObject;
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Couldn't find the CopyStrategy class!", e);
            } catch (InstantiationException e) {
                throw new RuntimeException("Couldn't instantiate the CopyStrategy instance!", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Couldn't instantiate the CopyStrategy instance!", e);
            } catch (ClassCastException e) {
                throw new RuntimeException(copyStrategy != null ? copyStrategy.getSimpleName()
                        + " doesn't implement net.sf.ehcache.store.compound.CopyStrategy" : "Error with CopyStrategy", e);
            }
        }
        return strategy;
    }

    /**
     * Make copy of this configuration
     * @return a copy of this configuration
     */
    protected CopyStrategyConfiguration copy() {
        CopyStrategyConfiguration clone = new CopyStrategyConfiguration();
        clone.setClass(getClassName());
        return clone;
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
        CopyStrategyConfiguration other = (CopyStrategyConfiguration) obj;
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
