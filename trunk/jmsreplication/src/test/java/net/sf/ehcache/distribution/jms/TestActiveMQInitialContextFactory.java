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
     * Creates an initial context with
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Context getInitialContext(Hashtable environment) throws NamingException {
	        
        Map<String, Object> data = new ConcurrentHashMap<String, Object>();
        
        String topicConnectionfactoryBindingName = (String)environment.get(JMSCacheManagerPeerProviderFactory.TOPIC_CONNECTION_FACTORY_BINDING_NAME);

        try {
        	data.put(topicConnectionfactoryBindingName, createConnectionFactory(environment));
        } catch (URISyntaxException e) {
        	throw new NamingException("Error initialisating TopicConnectionFactory with message " + e.getMessage());
        }

        String getQueueConnectionfactoryBindingName = (String)environment.get(JMSCacheManagerPeerProviderFactory.GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME);

        try {
        	data.put(getQueueConnectionfactoryBindingName, createConnectionFactory(environment));
        } catch (URISyntaxException e) {
        	throw new NamingException("Error initialisating TopicConnectionFactory with message " + e.getMessage());
        }

        String replicationTopicBindingName = (String)environment.get(JMSCacheManagerPeerProviderFactory.REPLICATION_TOPIC_BINDING_NAME);
        String getQueueBindingName = (String)environment.get(JMSCacheManagerPeerProviderFactory.GET_QUEUE_BINDING_NAME);

        data.put(replicationTopicBindingName, createTopic(replicationTopicBindingName));
        data.put(getQueueBindingName, createQueue(getQueueBindingName));

        return createContext(environment, data);
    }
}
