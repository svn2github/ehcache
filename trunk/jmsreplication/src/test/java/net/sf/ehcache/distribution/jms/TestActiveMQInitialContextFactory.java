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

package net.sf.ehcache.distribution.jms;

import org.apache.activemq.jndi.ActiveMQInitialContextFactory;

import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.NamingException;

public class TestActiveMQInitialContextFactory extends ActiveMQInitialContextFactory {

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Context getInitialContext(Hashtable environment) throws NamingException {
	        
        Map<String, Object> data = new ConcurrentHashMap<String, Object>();
        
        String factoryBindingName = (String)environment.get(JMSCacheManagerPeerProviderFactory.TOPICCONNECTIONFACTORYBINDINGNAME);
        
        try {
        	data.put(factoryBindingName, createConnectionFactory(environment));
        } catch (URISyntaxException e) {
        	throw new NamingException("Error initialisating ConnectionFactory with message " + e.getMessage());
        }
        
        String topicBindingName = (String)environment.get(JMSCacheManagerPeerProviderFactory.TOPICBINDINGNAME);

        data.put(topicBindingName, createTopic(topicBindingName));

        return createContext(environment, data);
    }
}
