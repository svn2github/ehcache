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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

/**
 * Abstract selector performing JNDI lookup
 *
 * @author Ludovic Orban
 */
public abstract class JndiSelector extends Selector {
    private static final Logger LOG = LoggerFactory.getLogger(JndiSelector.class);

    private volatile String jndiName;

    /**
     * Constructor
     *
     * @param vendor the transaction manager vendor name
     * @param jndiName the JNDI name to look up
     */
    public JndiSelector(String vendor, String jndiName) {
        super(vendor);
        this.jndiName = jndiName;
    }

    /**
     * Get the configured JNDI name to look up
     * @return the JNDI name to look up
     */
    public String getJndiName() {
        return jndiName;
    }

    /**
     * Set the configured JNDI name to look up
     * @param jndiName the JNDI name to look up
     */
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TransactionManager doLookup() {
        InitialContext initialContext;
        try {
            initialContext = new InitialContext();
        } catch (NamingException ne) {
            LOG.debug("cannot create initial context", ne);
            return null;
        }

        try {
            Object jndiObject;
            try {
                jndiObject = initialContext.lookup(getJndiName());
                if (jndiObject instanceof TransactionManager) {
                    return (TransactionManager) jndiObject;
                }
            } catch (NamingException e) {
                LOG.debug("Couldn't locate TransactionManager for {} under {}", getVendor(), getJndiName());
            }
            return null;
        } finally {
            try {
                initialContext.close();
            } catch (NamingException ne) {
                LOG.warn("error closing initial context", ne);
            }
        }
    }
}