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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.TransactionManager;

/**
 * Abstract selector for instanciating a class
 *
 * @author Ludovic Orban
 */
public abstract class ClassSelector extends Selector {
    private static final Logger LOG = LoggerFactory.getLogger(ClassSelector.class);

    private final String classname;
    private final ClassLoader classLoader;

    /**
     * Constructor
     *
     * @param vendor the transaction manager vendor name
     * @param loader the classloader to load from
     * @param classname the name of the class to instanciate
     */
    public ClassSelector(String vendor, ClassLoader loader, String classname) {
        super(vendor);
        this.classname = classname;
        this.classLoader = loader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TransactionManager doLookup() {
        TransactionManager transactionManager = null;

        try {
            Class txManagerClass = classLoader.loadClass(classname);
            transactionManager = (TransactionManager) txManagerClass.newInstance();
        } catch (ClassNotFoundException e) {
            LOG.debug("FactorySelector failed lookup", e);
        } catch (InstantiationException e) {
            LOG.debug("FactorySelector failed lookup", e);
        } catch (IllegalAccessException e) {
            LOG.debug("FactorySelector failed lookup", e);
        }
        return transactionManager;
    }
}