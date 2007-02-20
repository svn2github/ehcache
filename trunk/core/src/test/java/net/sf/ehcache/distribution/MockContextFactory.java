/**
 *  Copyright 2003-2007 Greg Luck
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
package net.sf.ehcache.distribution;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Mock of InitialContextFactory for testing.
 *
 * @author Andy McNutt
 * @author Greg Luck
 * @version $Id$
 */
public class MockContextFactory implements InitialContextFactory {

    private static final Log LOG = LogFactory.getLog(MockContextFactory.class.getName());

    private static final Map JNDI_PROVIDER_URL_TO_CONTEXT_MAP = Collections.synchronizedMap(new HashMap());

    /**
     * @see javax.naming.spi.InitialContextFactory#getInitialContext(java.util.Hashtable)
     */
    public Context getInitialContext(Hashtable environment) throws NamingException {
        LOG.debug("getInitialContext " + environment);
        String jndiProviderUrl = (String) environment.get(Context.PROVIDER_URL);
        synchronized (JNDI_PROVIDER_URL_TO_CONTEXT_MAP) {
            if (jndiProviderUrl == null) {
                throw new NamingException("getInitialContext: "
                        + Context.PROVIDER_URL + " is null " + environment);
            }
            Context context = null;
            context = (Context) JNDI_PROVIDER_URL_TO_CONTEXT_MAP
                    .get(jndiProviderUrl);
            if (context == null) {
                context = new MockContext(jndiProviderUrl);
                JNDI_PROVIDER_URL_TO_CONTEXT_MAP.put(jndiProviderUrl, context);
            }
            return context;
        }
    }

    /**
     * Clear all bindings in all MockContexts.
     */
    public void clear() throws NamingException {
        synchronized (JNDI_PROVIDER_URL_TO_CONTEXT_MAP) {
            Iterator it = JNDI_PROVIDER_URL_TO_CONTEXT_MAP.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                MockContext mockContext = (MockContext)
                        JNDI_PROVIDER_URL_TO_CONTEXT_MAP.get(key);
                mockContext.close();
            }
            JNDI_PROVIDER_URL_TO_CONTEXT_MAP.clear();
        }

    }

    /**
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p/>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString()).append(" JNDI_PROVIDER_URL_TO_CONTEXT_MAP=");
        synchronized (JNDI_PROVIDER_URL_TO_CONTEXT_MAP) {
            Iterator iterator = JNDI_PROVIDER_URL_TO_CONTEXT_MAP.keySet().iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                sb.append(" ").append(key).append("=")
                        .append(JNDI_PROVIDER_URL_TO_CONTEXT_MAP.get(key));
            }
        }
        return sb.toString();
    }
}
