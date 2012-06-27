package org.terracotta.ehcache.tests.container;

import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

/**
 * @author lorban
 */
public class MultiAppServerTransactionManagerLookup implements TransactionManagerLookup {

    private static final String[] JNDI_NAMES = new String[] {
            "java:/TransactionManager",             // JBoss 5.1 & Resin 3
            "java:appserver/TransactionManager",    // Glassfish 2
            "javax.transaction.TransactionManager"  // Weblogic
    };


    public void init() {
        //
    }

    public TransactionManager getTransactionManager() {
        for (String jndiName : JNDI_NAMES) {
            TransactionManager tm = lookup(jndiName);
            if (tm != null)
                return tm;
        }
        return null;
    }

    private TransactionManager lookup(String jndiName) {
        Context ctx = null;
        try {
            ctx = new InitialContext();

            TransactionManager tm = (TransactionManager) ctx.lookup(jndiName);

            return tm;
        } catch (NamingException e) {
            return null;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void register(EhcacheXAResource resource) {
        // no-op
    }

    public void unregister(EhcacheXAResource resource) {
        // no-op
    }

    public void setProperties(Properties properties) {
        // no-op
    }

}
