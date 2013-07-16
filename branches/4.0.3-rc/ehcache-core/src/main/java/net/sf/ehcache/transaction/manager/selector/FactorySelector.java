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
package net.sf.ehcache.transaction.manager.selector;

import net.sf.ehcache.util.ClassLoaderUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.transaction.TransactionManager;

/**
 * Abstract selector for calling a factory method
 *
 * @author Ludovic Orban
 */
public abstract class FactorySelector extends Selector {
    private static final Logger LOG = LoggerFactory.getLogger(FactorySelector.class);

    private final String factoryClassName;
    private final String factoryMethodName;

    /**
     * Constructor
     *
     * @param vendor the transaction manager vendor name
     * @param factoryClassName the class used to lookup the transaction manager
     * @param factoryMethodName the method to be called on the class used to lookup the transaction manager
     */
    public FactorySelector(String vendor, String factoryClassName, String factoryMethodName) {
        super(vendor);
        this.factoryClassName = factoryClassName;
        this.factoryMethodName = factoryMethodName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TransactionManager doLookup() {
        TransactionManager transactionManager = null;

        try {
            Class factoryClass = ClassLoaderUtil.loadClass(factoryClassName);
            Class[] signature = null;
            Object[] args = null;
            Method method = factoryClass.getMethod(factoryMethodName, signature);
            transactionManager = (TransactionManager) method.invoke(null, args);
        } catch (ClassNotFoundException e) {
            LOG.debug("FactorySelector failed lookup: {}", (Object) e);
        } catch (NoSuchMethodException e) {
            LOG.debug("FactorySelector failed lookup: {}", (Object) e);
        } catch (InvocationTargetException e) {
            LOG.debug("FactorySelector failed lookup: {}", (Object) e);
        } catch (IllegalAccessException e) {
            LOG.debug("FactorySelector failed lookup: {}", (Object) e);
        }
        return transactionManager;
    }
}