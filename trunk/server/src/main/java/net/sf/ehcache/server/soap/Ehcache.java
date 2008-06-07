/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.server.soap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import java.security.Principal;

/**
 * The Ehcache WebService
 * @author Greg Luck
 * @version $Id$
 */
@WebService(name="EhcachePortType", targetNamespace = "http://soap.server.ehcache.sf.net")
public class Ehcache {

    private static final Log LOG = LogFactory.getLog(Ehcache.class.getName());

    @Resource
    private WebServiceContext context;

    /**
     * Test method
     * @param from
     * @return
     */
    @WebMethod
    public String sayHelloWorldFrom(String from) {

        String result = "Hello, world, from " + from + " principal: " + getUserPrincipal();
        LOG.info(result);
        return result;
    }


    /**
     * Gets the user Principal
     * @return
     */
    public Principal getUserPrincipal() {
        return context.getUserPrincipal();
    }
}