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

package net.sf.ehcache.hibernate.management.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardMBean;

/**
 * @author gkeim
 *
 */
public abstract class BaseEmitterBean extends StandardMBean implements NotificationEmitter {
    /**
     * emitter
     */
    protected final Emitter emitter = new Emitter();

    /**
     * sequenceNumber
     */
    protected final AtomicLong sequenceNumber = new AtomicLong();


    private final List<NotificationListener> notificationListeners = new CopyOnWriteArrayList<NotificationListener>();

    /**
     * BaseEmitterBean
     *
     * @param <T>
     * @param mbeanInterface
     * @throws NotCompliantMBeanException
     */
    protected <T> BaseEmitterBean(Class<T> mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);
    }

    /**
     * sendNotification
     *
     * @param eventType
     */
    public void sendNotification(String eventType) {
        sendNotification(eventType, null, null);
    }

    /**
     * sendNotification
     *
     * @param eventType
     * @param data
     */
    public void sendNotification(String eventType, Object data) {
        sendNotification(eventType, data, null);
    }

    /**
     * sendNotification
     *
     * @param eventType
     * @param data
     * @param msg
     */
    public void sendNotification(String eventType, Object data, String msg) {
        Notification notif = new Notification(eventType, this, sequenceNumber.incrementAndGet(), System.currentTimeMillis(), msg);
        if (data != null) {
            notif.setUserData(data);
        }
        emitter.sendNotification(notif);
    }

    /**
     * Dispose of this SampledCacheManager and clean up held resources
     */
    public final void dispose() {
        doDispose();
        removeAllNotificationListeners();
    }

    /**
     * Dispose callback of subclasses
     */
    protected abstract void doDispose();

    /**
     * @author gkeim
     */
    private class Emitter extends NotificationBroadcasterSupport {
        /**
         * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
         */
        @Override
        public MBeanNotificationInfo[] getNotificationInfo() {
            return BaseEmitterBean.this.getNotificationInfo();
        }
    }

    /**
     * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, java.lang.Object)
     */
    public void addNotificationListener(NotificationListener notif, NotificationFilter filter, Object callBack) {
        emitter.addNotificationListener(notif, filter, callBack);
        notificationListeners.add(notif);
    }

    /**
     * remove all added notification listeners
     */
    private void removeAllNotificationListeners() {
        for (NotificationListener listener : notificationListeners) {
            try {
                emitter.removeNotificationListener(listener);
            } catch (ListenerNotFoundException e) {
                // ignore
            }
        }
        notificationListeners.clear();
    }

    /**
     * @see javax.management.NotificationBroadcaster#getNotificationInfo()
     */
    public abstract MBeanNotificationInfo[] getNotificationInfo();


    /**
     * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
     */
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener);
        notificationListeners.remove(listener);
    }

    /**
     * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, java.lang.Object)
     */
    public void removeNotificationListener(NotificationListener notif, NotificationFilter filter, Object callBack)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(notif, filter, callBack);
        notificationListeners.remove(notif);
    }
}
