/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.distribution;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMISocketFactory;


/**
 * Default socket timeouts are unlikely to be suitable for cache replication. Sockets should
 * fail fast.
 * <p/>
 * This class decorates the RMIClientSocketFactory so as to enable customisations to be placed
 * on newly created sockets.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: ConfigurableRMIClientSocketFactory.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 * @see "http://java.sun.com/j2se/1.5.0/docs/guide/rmi/socketfactory/#1"
 */
public final class ConfigurableRMIClientSocketFactory implements Serializable, RMIClientSocketFactory {

    private static final int ONE_SECOND = 1000;

    private int socketTimeoutMillis;

    /**
     * Construct a new socket factory with the given timeout
     *
     * @param socketTimeoutMillis
     * @see Socket#setSoTimeout
     */
    public ConfigurableRMIClientSocketFactory(Integer socketTimeoutMillis) {
        if (socketTimeoutMillis == null) {
            this.socketTimeoutMillis = ONE_SECOND;
        } else {
            this.socketTimeoutMillis = socketTimeoutMillis.intValue();
        }
    }

    /**
     * Create a client socket connected to the specified host and port.
     * <p/>
     * If necessary this implementation can be changed to specify the outbound address to use
     * e.g. <code>Socket socket = new Socket(host, port, localInterface , 0);</code>
     *
     * @param host the host name
     * @param port the port number
     * @return a socket connected to the specified host and port.
     * @throws java.io.IOException if an I/O error occurs during socket creation
     * @since 1.2
     */
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = RMISocketFactory.getDefaultSocketFactory().createSocket(host, port);

        socket.setSoTimeout(socketTimeoutMillis);
        
        return socket;
    }

    /**
     * Implements the Object hashCode method.
     *
     * @return a hash based on socket options
     */
    public int hashCode() {
        return socketTimeoutMillis;
    }

    /**
     * The standard hashCode method which is necessary for SocketFactory classes.
     * Omitting this method causes RMI to quickly error out
     * with "too many open files" errors.
     *
     * @param object the comparison object
     * @return equal if the classes are the same and the socket options are the name.
     */
    public boolean equals(Object object) {
        return (getClass() == object.getClass() &&
                socketTimeoutMillis == ((ConfigurableRMIClientSocketFactory) object).socketTimeoutMillis);
    }

}


