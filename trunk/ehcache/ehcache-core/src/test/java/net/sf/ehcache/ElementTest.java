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

package net.sf.ehcache;


import net.sf.ehcache.config.CacheConfiguration;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Test cases for the Element.
 *
 * @version $Id$
 */
public class ElementTest {

    /**
     * ehcache-1.2 adds support to Objects in addition to Serializable. Check that this works
     */
    @Test
    public void testObjectAccess() {
        Object key = new Object();
        Object value = new Object();
        Element element = new Element(key, value);
        assertThat(element.getObjectKey(), sameInstance(key));
        assertThat(element.getObjectValue(), sameInstance(value));
    }

    @Test
    public void testGetKeyThrowsCacheExceptionOnAccessingNonSerializableKey() {
        Object key = new Object() {
            @Override
            public String toString() {
                return "HAHA!";
            }
        };
        Object value = new Object();
        Element element = new Element(key, value);
        try {
            element.getKey();
            fail("element.getKey() did not throw");
        } catch (CacheException e) {
            assertThat(e.getMessage(), equalTo("The key HAHA! is not Serializable. Consider using Element.getObjectKey()"));
        }
    }

    @Test
    public void testGetValueThrowsCacheExceptionOnAccessingNonSerializableValue() {
        Object key = new Object() {
            @Override
            public String toString() {
                return "daKey";
            }
        };
        Object value = new Object() {
            @Override
            public String toString() {
                return "HOHO!";
            }
        };
        Element element = new Element(key, value);
        try {
            element.getValue();
            fail();
        } catch (CacheException e) {
            assertThat(e.getMessage(), equalTo("The value HOHO! for key daKey is not Serializable. Consider using Element.getObjectValue()"));
        }
    }

    /**
     * ehcache-1.1 and earlier exclusively uses Serializable keys and values. Check that this works
     */
    @Test
    public void testSerializableAccess() {
        // Yes, NEW String, don't you touch that!
        Serializable key = new String("key");
        Serializable value = new String("value");
        Element element = new Element(key, value);

        assertThat(element.getKey(), sameInstance(key));
        assertThat(element.getValue(), sameInstance(value));
    }

    @Test
    public void testNullsAreSerializable() {
        Element element = new Element(null, null);
        assertTrue(element.isSerializable());
        Element elementWithNullValue = new Element("1", null);
        assertTrue(elementWithNullValue.isSerializable());
        Element elementWithNullKey = new Element(null, "1");
        assertTrue(elementWithNullKey.isSerializable());
    }

    @Test
    public void testSerializableIsSerializable() {
        Element element = new Element("I'm with", "idiot");
        assertTrue(element.isSerializable());
    }

    @Test
    public void testNonSerializableAreNotSerializable() {
        Element elementWithObjectKey = new Element(new Object(), "1");
        assertFalse(elementWithObjectKey.isSerializable());
        Element elementWithObjectValue = new Element("1", new Object());
        assertFalse(elementWithObjectValue.isSerializable());
        Element elementWithObjectKeyAndValue = new Element(new Object(), new Object());
        assertFalse(elementWithObjectKeyAndValue.isSerializable());
    }

    @Test
    public void testEquals() {
        Element element = new Element("key", "value");

        assertTrue(element.equals(element));
        assertTrue(element.equals(new Element("key", "hat")));

        assertFalse(element.equals("dog"));
        assertFalse(element.equals(null));
        assertFalse(element.equals(new Element("cat", "hat")));
    }

    @Test
    public void testEqualsCharacterizationTestWithWhichWeDoNotAgree() {
        Element element = new Element(null, "value");
        assertFalse(element.equals(new Element(null, "hat")));
        assertFalse(element.equals(element)); // you brain hurts now, right ?
    }

    @Test
    public void testElementSerializes() throws IOException, ClassNotFoundException {

        Element e = new Element("foo", "bar", 56L, TimeUnit.SECONDS.toMillis(48), TimeUnit.SECONDS.toMillis(12), 23485L, false, 5, 12, 13);

        final Element newElement = serializeAndDeserializeBack(e);
        assertThat(newElement, samePropertyValuesAs(e));
    }

    @Test
    public void testSerializingCeilsTimestamps() throws IOException, ClassNotFoundException {

        Element e = new Element("foo", "bar", 56L, 48001L, 12678, 23485L, false, 5, 12, 13);

        final Element newElement = serializeAndDeserializeBack(e);
        assertThat(newElement.getCreationTime(), is(49000L));
        assertThat(newElement.getLastAccessTime(), is(13000L));
    }

    @Test
    public void testLastAccessTime() throws InterruptedException {
        final long initialValue = new Random().nextLong();
        final AtomicLong now = new AtomicLong(initialValue);
        Element element = new Element("", "") {
            @Override
            long getCurrentTime() {
                return now.get();
            }
        };

        assertThat(element.getLastAccessTime(), is(0L));

        element.updateAccessStatistics();
        assertThat(element.getLastAccessTime(), is(initialValue));
        now.set(13L);
        element.updateAccessStatistics();
        assertThat(element.getLastAccessTime(), is(13L));
    }

    @Test
    public void testSetsCreationTimeAtConstruction() throws InterruptedException {
        final long initialValue = new Random().nextLong();
        final AtomicLong now = new AtomicLong(initialValue);
        Element element = new Element("", "") {
            @Override
            long getCurrentTime() {
                return now.get();
            }
        };
        assertThat(element.getCreationTime(), is(initialValue));
    }

    @Test
    public void testCreationTimeNeverChanges() {

        final long initialValue = new Random().nextLong();
        final AtomicLong now = new AtomicLong(initialValue);
        Element element = new Element("", "") {
            @Override
            long getCurrentTime() {
                return now.get();
            }
        };
        now.set(-1);

        element.resetAccessStatistics();
        element.updateAccessStatistics();
        element.updateUpdateStatistics();
        assertThat(element.getCreationTime(), is(initialValue));
    }

    @Test
    public void testUpdateUpdateStatistics() throws InterruptedException {
        final long initialValue = new Random().nextLong();
        final AtomicLong now = new AtomicLong(initialValue);
        Element element = new Element("", "") {
            @Override
            long getCurrentTime() {
                return now.get();
            }
        };

        final long newValue = initialValue + 10000;
        now.set(newValue);
        element.updateUpdateStatistics();
        assertThat(element.getLastUpdateTime(), is(newValue));
    }

    @Test
    public void testGetLatestOfCreationAndUpdateTime() throws InterruptedException {
        final long initialValue = new Random().nextLong();
        final AtomicLong now = new AtomicLong(initialValue);
        Element element = new Element("", "") {
            @Override
            long getCurrentTime() {
                return now.get();
            }
        };

        final long newValue = initialValue + 10000;
        now.set(newValue);
        element.updateUpdateStatistics();
        assertThat(element.getLatestOfCreationAndUpdateTime(), is(newValue));
    }

    @Test
    public void testGetLatestOfCreationAndUpdateTimeReturnsCreationWhenNotUpdated() {
        final long initialValue = new Random().nextLong();
        final AtomicLong now = new AtomicLong(initialValue);
        Element element = new Element("", "") {
            @Override
            long getCurrentTime() {
                return now.get();
            }
        };

        assertThat(element.getLatestOfCreationAndUpdateTime(), is(initialValue));
    }

    @Test
    public void testIsExpiredProvidedConfig() {
        final Element element = new Element("foo", "bar");
        final CacheConfiguration config = mock(CacheConfiguration.class);
        when(config.isEternal()).thenReturn(true);
        assertThat(element.isExpired(config), is(false));
        when(config.isEternal()).thenReturn(false);
        when(config.getTimeToIdleSeconds()).thenReturn(123L);
        when(config.getTimeToLiveSeconds()).thenReturn(124L);
        element.isExpired(config);
        assertThat(element.getTimeToIdle(), is(123));
        assertThat(element.getTimeToLive(), is(124));
    }

    @Test
    public void testExpirationTimeWhenNotSet() {
        final Element element = new Element("foo", "bar");
        assertThat(element.getExpirationTime(), is(Long.MAX_VALUE));
    }

    @Test
    public void testExpirationTimeWhenEternal() {
        final Element element = new Element("foo", "bar");
        element.setEternal(true);
        assertThat(element.getExpirationTime(), is(Long.MAX_VALUE));
    }

    @Test
    public void testEternalFalseUnsetsLifespan() {
        final Element element = new Element("foo", "bar");
        element.setEternal(true);
        assertThat(element.isLifespanSet(), is(true));
        element.setEternal(false);
        assertThat(element.isLifespanSet(), is(false));
    }

    @Test
    public void testLifespanDefaultEternalWins() {
        final Element element = new Element("foo", "bar");
        element.setLifespanDefaults(12, 14, true);
        assertThat(element.isEternal(), is(true));
        assertThat(element.getTimeToIdle(), is(0));
        assertThat(element.getTimeToLive(), is(0));
    }

    @Test
    public void testLifespanSetTTIandTTL() {
        final Element element = new Element("foo", "bar");
        element.setLifespanDefaults(12, 14, false);
        assertThat(element.isEternal(), is(false));
        assertThat(element.getTimeToIdle(), is(12));
        assertThat(element.getTimeToLive(), is(14));
    }

    @Test
    public void testLifespanUnsetsLifespan() {
        final Element element = new Element("foo", "bar");
        element.setEternal(true);
        assertThat(element.isLifespanSet(), is(true));
        element.setLifespanDefaults(12, 14, false);
        assertThat(element.isLifespanSet(), is(false));
    }

    @Test
    public void testCloneForMetaData() throws CloneNotSupportedException {
        final long version = 1L;
        final long creationTime = 12L;
        final long lastAccessTime = 123L;
        final long lastUpdateTime = 1234L;
        final long hitCount = 12345L;
        Element clone = (Element)new Element("", "", version, creationTime, lastAccessTime, lastUpdateTime, hitCount).clone();
        assertThat(clone.getVersion(), is(version));
        assertThat(clone.getCreationTime(), is(creationTime));
        assertThat(clone.getLastAccessTime(), is(lastAccessTime));
        assertThat(clone.getLastUpdateTime(), is(not(lastUpdateTime))); // For some reason lastUpdateTime isn't cloned
        assertThat(clone.getLastUpdateTime(), is(0L));
        assertThat(clone.getHitCount(), is(hitCount));
    }

    @Test
    public void testConstructorArgPrecedence() {
        Element element = new Element("key", "value", true, 10, 10);
        assertThat(element.isEternal(), is(false));
        assertThat(element.usesCacheDefaultLifespan(), is(false));

        element = new Element("key", "value", null, 10, 10);
        assertThat(element.isEternal(), is(false));
        assertThat(element.usesCacheDefaultLifespan(), is(false));

        element = new Element("key", "value", null, 0, 0);
        assertThat(element.isEternal(), is(true));
        assertThat(element.usesCacheDefaultLifespan(), is(false));

        element = new Element("key", "value", true, 0, 0);
        assertThat(element.isEternal(), is(true));
        assertThat(element.usesCacheDefaultLifespan(), is(false));

        element = new Element("key", "value", false, 0, 0);
        assertThat(element.isEternal(), is(true));
        assertThat(element.usesCacheDefaultLifespan(), is(false));

        element = new Element("key", "value", true, null, null);
        assertThat(element.isEternal(), is(true));
        assertThat(element.usesCacheDefaultLifespan(), is(false));

        element = new Element("key", "value", null, null, null);
        assertThat(element.isEternal(), is(false));
        assertThat(element.usesCacheDefaultLifespan(), is(true));
    }

    @Test
    public void testDoesNotDefaultToEternal() {
        final Element element = new Element("key", "bar");
        assertThat(element.isEternal(), is(false));
    }

    @Test
    public void testExplicitlySetToEternalElementDoesNotExpire() {

        final AtomicLong now = new AtomicLong(0L);

        final Element element = new Element("key", "bar") {
            @Override
            long getCurrentTime() {
                return now.get();
            }
        };
        element.setEternal(true);

        assertThat(element.isExpired(), is(false));
        now.set(Long.MAX_VALUE);
        assertThat(element.isExpired(), is(false));
        now.set(Long.MIN_VALUE);
        assertThat(element.isExpired(), is(false));
    }

    @Test
    public void testExpiresAfterTTIEvenWhenNeverAccessed() {

        final AtomicLong now = new AtomicLong(0);

        final Element element = new Element("key", "bar") {
            @Override
            long getCurrentTime() {
                return now.get();
            }
        };

        final int timeToIdleSeconds = 10;
        element.setTimeToIdle(timeToIdleSeconds);

        assertThat(element.isExpired(), is(false));

        now.set(now.get() + TimeUnit.SECONDS.toMillis(timeToIdleSeconds) + 1);
        assertThat(element.isExpired(), is(true));
    }

    @Test
    public void testIdNotSetByDefault() {
        Element e = new Element("foo", "bar");
        assertThat(e.hasId(), is(false));
    }

    @Test
    public void testGetIdDoesNotThrowWhenSet() {
        Element e = new Element("foo", "bar");
        final long id = 123L;
        e.setId(id);
        assertThat(e.getId(), is(id));
    }

    @Test
    public void testSetIdThrowsWhenSetToZero() {
        Element e = new Element("foo", "bar");
        try {
            e.setId(0);
            fail();
        } catch (IllegalArgumentException _) {
            // expected
        }
    }

    // WHAT FOLLOWS ARE CHARACTERIZATION TESTS OF ELEMENT CONSTRUCTORS...
    // only expose the bad design of the Class Under Test

    @Test
    public void testTwoSerializableConstructor() {
        final String key = new String("foo");
        final String value = new String("bar");
        Element e = new Element(key, value) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance((Object)key));
        assertThat(e.getObjectValue(), sameInstance((Object)value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(0));
        assertThat(e.getTimeToLive(), is(0));
        assertThat(e.getVersion(), is(1L));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(true));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testTwoObjectConstructor() {
        final Object key = new Object();
        final Object value = new Object();
        Element e = new Element(key, value) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(0));
        assertThat(e.getTimeToLive(), is(0));
        assertThat(e.getVersion(), is(1L));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(true));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testThreeSerializableConstructor() {
        final String key = new String("foo");
        final String value = new String("bar");
        Element e = new Element(key, value, 2L) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance((Object)key));
        assertThat(e.getObjectValue(), sameInstance((Object)value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(0));
        assertThat(e.getTimeToLive(), is(0));
        assertThat(e.getVersion(), is(2L));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(true));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testThreeObjectConstructor() {
        final Object key = new Object();
        final Object value = new Object();
        Element e = new Element(key, value, 2L) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(0));
        assertThat(e.getTimeToLive(), is(0));
        assertThat(e.getVersion(), is(2L));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(true));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testConstructorLLJJJJJJ() {
        final Object key = new Object();
        final Object value = new Object();
        final long creationTime = 123452L;
        final long lastAccessTime = 123453L;
        final long lastUpdateTime = 123455L;
        final long hitCount = 123462L;
        final long version = 123451L;
        Element e = new Element(key, value, version, creationTime, lastAccessTime, 123454L, lastUpdateTime, hitCount) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(creationTime));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(lastAccessTime));
        assertThat(e.getLastUpdateTime(), is(lastUpdateTime));
        assertThat(e.getHitCount(), is(hitCount));
        assertThat(e.getTimeToIdle(), is(0));
        assertThat(e.getTimeToLive(), is(0));
        assertThat(e.getVersion(), is(version));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(true));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testConstructorLLJJJJJ() {
        final Object key = new Object();
        final Object value = new Object();
        final long creationTime = 123452L;
        final long lastAccessTime = 123453L;
        final long lastUpdateTime = 123455L;
        final long hitCount = 123462L;
        final long version = 123451L;
        Element e = new Element(key, value, version, creationTime, lastAccessTime, lastUpdateTime, hitCount) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(creationTime));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(lastAccessTime));
        assertThat(e.getLastUpdateTime(), is(lastUpdateTime));
        assertThat(e.getHitCount(), is(hitCount));
        assertThat(e.getTimeToIdle(), is(0));
        assertThat(e.getTimeToLive(), is(0));
        assertThat(e.getVersion(), is(version));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(true));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testConstructorLLJJJJZIIJ() {
        final Object key = new Object();
        final Object value = new Object();
        final long creationTime = 123452L;
        final long lastAccessTime = 123453L;
        final long lastUpdateTime = 123455L;
        final long hitCount = 123462L;
        final long version = 123451L;
        final int timeToIdle = 3;
        final int timeToLive = 1;
        final boolean cacheDefaultLifespan = false;
        Element e = new Element(key, value, version, creationTime, lastAccessTime, hitCount, cacheDefaultLifespan, timeToLive, timeToIdle, lastUpdateTime) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(creationTime));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(lastAccessTime));
        assertThat(e.getLastUpdateTime(), is(lastUpdateTime));
        assertThat(e.getHitCount(), is(hitCount));
        assertThat(e.getTimeToIdle(), is(timeToIdle));
        assertThat(e.getTimeToLive(), is(timeToLive));
        assertThat(e.getVersion(), is(version));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(cacheDefaultLifespan));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testConstructorLLJJJJZIIJsupportsNegativeTTIsTTLs() {
        final Object key = new Object();
        final Object value = new Object();
        final long creationTime = 123452L;
        final long lastAccessTime = 123453L;
        final long lastUpdateTime = 123455L;
        final long hitCount = 123462L;
        final long version = 123451L;
        final int timeToIdle = -1;
        final int timeToLive = -1;
        final boolean cacheDefaultLifespan = false;
        Element e = new Element(key, value, version, creationTime, lastAccessTime, hitCount, cacheDefaultLifespan, timeToLive, timeToIdle, lastUpdateTime) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(creationTime));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(lastAccessTime));
        assertThat(e.getLastUpdateTime(), is(lastUpdateTime));
        assertThat(e.getHitCount(), is(hitCount));
        assertThat(e.getTimeToIdle(), is(timeToIdle));
        assertThat(e.getTimeToLive(), is(timeToLive));
        assertThat(e.getVersion(), is(version));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(cacheDefaultLifespan));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testConstructorLLII() {
        final Object key = new Object();
        final Object value = new Object();
        final int timeToIdle = 3;
        final int timeToLive = 1;
        Element e = new Element(key, value, timeToIdle, timeToLive) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(timeToIdle));
        assertThat(e.getTimeToLive(), is(timeToLive));
        assertThat(e.getVersion(), is(1L));
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(false));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorLLIIthrowsNegativeTTI() {
        new Element("", "", -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorLLIIthrowsNegativeTTL() {
        new Element("", "", 1, -1);
    }

    @Test
    public void testConstructorLLZ() {
        final Object key = new Object();
        final Object value = new Object();
        Element e = new Element(key, value, true) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(0));
        assertThat(e.getTimeToLive(), is(0));
        assertThat(e.getVersion(), is(1L));
        assertThat(e.isEternal(), is(true));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(false));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testConstructorLLLLL() {
        final Object key = new Object();
        final Object value = new Object();
        final Integer timeToIdleSeconds = 123;
        final Integer timeToLiveSeconds = 321;
        Element e = new Element(key, value, Boolean.TRUE, timeToIdleSeconds, timeToLiveSeconds) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(timeToIdleSeconds));
        assertThat(e.getTimeToLive(), is(timeToLiveSeconds));
        assertThat(e.getVersion(), is(0L)); // WTF?!
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(false));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testConstructorLLLLLnulls() {
        final Object key = new Object();
        final Object value = new Object();
        Element e = new Element(key, value, null, null, null) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(0));
        assertThat(e.getTimeToLive(), is(0));
        assertThat(e.getVersion(), is(0L)); // WTF?!
        assertThat(e.isEternal(), is(false));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(true));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test
    public void testConstructorLLLLLeternalLooses() {
        final Object key = new Object();
        final Object value = new Object();
        final Integer timeToIdleSeconds = 2;
        final Integer timeToLiveSeconds = 2;
        final boolean eternal = true;
        Element e = new Element(key, value, eternal, timeToIdleSeconds, timeToLiveSeconds) {
            @Override
            long getCurrentTime() {
                return Long.MIN_VALUE;
            }
        };
        assertThat(e.getCreationTime(), is(Long.MIN_VALUE));
        assertThat(e.getObjectKey(), sameInstance(key));
        assertThat(e.getObjectValue(), sameInstance(value));
        assertThat(e.getLastAccessTime(), is(0L));
        assertThat(e.getLastUpdateTime(), is(0L));
        assertThat(e.getHitCount(), is(0L));
        assertThat(e.getTimeToIdle(), is(timeToIdleSeconds));
        assertThat(e.getTimeToLive(), is(timeToLiveSeconds));
        assertThat(e.getVersion(), is(0L)); // WTF?!
        assertThat(e.isEternal(), not(eternal));
        assertThat(e.hasId(), is(false));
        assertThat(e.usesCacheDefaultLifespan(), is(false));
        try {
            e.getId();
            fail();
        } catch (IllegalStateException e1) {
            // expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorLLLLLthrowsNegativeTTI() {
        new Element("", "", false, -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorLLLLLthrowsNegativeTTL() {
        new Element("", "", false, 1, -1);
    }

    private static <T extends Serializable> T serializeAndDeserializeBack(final T e) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);

        objectOutputStream.writeObject(e);
        objectOutputStream.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(bais);
        return (T) objectInputStream.readObject();
    }
}
