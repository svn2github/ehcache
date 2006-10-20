/**
 *  Copyright 2003-2006 Greg Luck
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
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Mock of Context for testing.
 *
 * @author Andy McNutt
 * @author Greg Luck
 * @version $Id$
 */
public class MockContext implements Context {

    private static final Log LOG = LogFactory.getLog(MockContext.class.getName());

    /**
     * Map of objects registered for this context representing the local context.
     */
    private final Map nameToObjectMap = Collections.synchronizedMap(new HashMap());

    private String jndiProviderUrl;

    /**
     * Constructor
     *
     * @param jndiProviderUrl
     */
    public MockContext(String jndiProviderUrl) {
        this.jndiProviderUrl = jndiProviderUrl;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#addToEnvironment(String, Object)
     */
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        methodNotImplemented("addToEnvironment(String, Object)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#bind(javax.naming.Name, Object)
     */
    public void bind(Name name, Object obj) throws NamingException {
        methodNotImplemented("bind(String, Object)");
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#bind(String, Object)
     */
    public void bind(String name, Object obj) throws NamingException {
        methodNotImplemented("bind(String, Object)");
    }

    /**
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException {
        nameToObjectMap.clear();
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#composeName(javax.naming.Name, javax.naming.Name)
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        methodNotImplemented("composeName(Name, Name)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#composeName(String, String)
     */
    public String composeName(String name, String prefix)
            throws NamingException {
        methodNotImplemented("composeName(String, String)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext(Name name) throws NamingException {
        methodNotImplemented("createSubcontext(Name)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext(String name) throws NamingException {
        methodNotImplemented("createSubcontext(String)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext(Name name) throws NamingException {
        methodNotImplemented("destroySubcontext(Name)");
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext(String name) throws NamingException {
        methodNotImplemented("destroySubcontext(String)");
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable getEnvironment() throws NamingException {
        methodNotImplemented("getEnvironment()");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException {
        methodNotImplemented("getNameInNamespace()");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser(Name name) throws NamingException {
        methodNotImplemented("getNameParser(Name)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser(String name) throws NamingException {
        methodNotImplemented("getNameParser(String)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration list(Name name) throws NamingException {
        methodNotImplemented("list(Name)");
        return null;
    }


    /**
     * Not implemented
     *
     * @see javax.naming.Context#listBindings(String)
     */
    public NamingEnumeration list(String name) throws NamingException {
        methodNotImplemented("list(String)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration listBindings(Name name) throws NamingException {
        methodNotImplemented("listBindings(Name)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration listBindings(String name) throws NamingException {
        methodNotImplemented("listBindings(String)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup(Name name) throws NamingException {
        methodNotImplemented("lookup(Name)");
        return null;
    }

    /**
     * Retrieves the named object.
     *
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup(String name) throws NamingException {
        return nameToObjectMap.get(name);
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink(Name name) throws NamingException {
        methodNotImplemented("lookupLink(Name)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#lookupLink(String)
     */
    public Object lookupLink(String name) throws NamingException {
        methodNotImplemented("lookupLink(String)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#rebind(javax.naming.Name, Object)
     */
    public void rebind(Name name, Object obj) throws NamingException {
        methodNotImplemented("rebind(Name, Object)");
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     *
     * @see javax.naming.Context#rebind(javax.naming.Name, Object)
     */
    public void rebind(String name, Object obj) throws NamingException {
        LOG.debug(jndiProviderUrl + " rebind " + name + " " + obj);
        nameToObjectMap.put(name, obj);
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#removeFromEnvironment(String)
     */
    public Object removeFromEnvironment(String propName) throws NamingException {
        methodNotImplemented("removeFromEnvironment(String)");
        return null;
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        methodNotImplemented("rename(Name, Name)");
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#rename(String, String)
     */
    public void rename(String oldName, String newName) throws NamingException {
        methodNotImplemented("rename(String, String)");
    }

    /**
     * Not implemented
     *
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind(Name name) throws NamingException {
        methodNotImplemented("unbind(Name)");
    }

    /**
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind(String name) throws NamingException {
        LOG.debug(jndiProviderUrl + " unbind " + name);
        nameToObjectMap.remove(name);
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
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString()).append(" providerUrl=") .append(jndiProviderUrl).append(" nameToObjectMap=[");
        synchronized (nameToObjectMap) {
            Iterator it = nameToObjectMap.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                buff.append(" ").append(key).append("=") .append(nameToObjectMap.get(key));
            }
            buff.append("]");
        }
        return buff.toString();
    }

    private void methodNotImplemented(String s) {
        String msg = getClass().getName()
                + ": method not implemented " + s;
        throw new RuntimeException(msg);
    }
}
