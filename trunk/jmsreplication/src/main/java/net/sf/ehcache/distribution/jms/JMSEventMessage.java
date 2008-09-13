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

import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.EventMessage;

import java.io.Serializable;


/**
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSEventMessage extends EventMessage {

    private static final long serialVersionUID = 927345728947584L;

    private String cacheName;
    private String originatingCacheGUID;

    /**
     *
     * @param event
     * @param key
     * @param element
     * @param cacheName
     * @param sendingCacheGUID
     */
    public JMSEventMessage(int event, Serializable key, Element element, String cacheName, String sendingCacheGUID) {
        super(event, key, element);
        setCacheName(cacheName);
        this.originatingCacheGUID = sendingCacheGUID;
    }

    /**
     *
     * @return
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     *
     * @param cacheName
     */
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     *
     * @return
     */
    public String getOriginatingCacheGUID() {
        return originatingCacheGUID;
    }

    /**
     *
     * @param originatingCacheGUID
     */
    public void setOriginatingCacheGUID(String originatingCacheGUID) {
        this.originatingCacheGUID = originatingCacheGUID;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "JMSEventMessage ( event = " + getEvent() + ", element = " + getElement() + ", cacheName = "
                + cacheName + ", originatingCacheGUID = " + originatingCacheGUID;
    }

}
