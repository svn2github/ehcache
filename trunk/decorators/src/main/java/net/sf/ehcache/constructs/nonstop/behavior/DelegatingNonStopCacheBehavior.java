/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.constructs.nonstop.behavior;

import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;

/**
 * CacheBehavior that delegates all operations to another
 * {@link NonStopCacheBehavior}
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class DelegatingNonStopCacheBehavior implements NonStopCacheBehavior {

	/**
	 * Interface that knows how to get the delegate
	 * 
	 */
	public static interface DelegateHolder {
		/**
		 * returns the delegate associated with this holder
		 * 
		 * @return the delegate associated with this holder
		 */
		public NonStopCacheBehavior getDelegate();
	}

	/**
	 * The delegateHolder
	 */
	private final DelegateHolder delegateHolder;

	/**
	 * Constructor accepting a {@link DelegateHolder} that knows how to locate
	 * the delegate {@link NonStopCacheBehavior}
	 * 
	 * @param delegate
	 */
	public DelegatingNonStopCacheBehavior(DelegateHolder holder) {
		this.delegateHolder = holder;
	}

	/**
	 * {@inheritDoc}
	 */
	public Element get(Object key) throws IllegalStateException, CacheException {
		return delegateHolder.getDelegate().get(key);
	}

	/**
	 * {@inheritDoc}
	 */
	public Element getQuiet(Object key) throws IllegalStateException,
			CacheException {
		return delegateHolder.getDelegate().getQuiet(key);
	}

	/**
	 * {@inheritDoc}
	 */
	public List getKeys() throws IllegalStateException, CacheException {
		return delegateHolder.getDelegate().getKeys();
	}

	/**
	 * {@inheritDoc}
	 */
	public List getKeysNoDuplicateCheck() throws IllegalStateException {
		return delegateHolder.getDelegate().getKeysNoDuplicateCheck();
	}

	/**
	 * {@inheritDoc}
	 */
	public List getKeysWithExpiryCheck() throws IllegalStateException,
			CacheException {
		return delegateHolder.getDelegate().getKeysWithExpiryCheck();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isKeyInCache(Object key) {
		return delegateHolder.getDelegate().isKeyInCache(key);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValueInCache(Object value) {
		return delegateHolder.getDelegate().isValueInCache(value);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(Element element, boolean doNotNotifyCacheReplicators)
			throws IllegalArgumentException, IllegalStateException,
			CacheException {
		delegateHolder.getDelegate().put(element, doNotNotifyCacheReplicators);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		delegateHolder.getDelegate().put(element);
	}

	/**
	 * {@inheritDoc}
	 */
	public void putQuiet(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		delegateHolder.getDelegate().putQuiet(element);
	}

	/**
	 * {@inheritDoc}
	 */
	public void putWithWriter(Element element) throws IllegalArgumentException,
			IllegalStateException, CacheException {
		delegateHolder.getDelegate().putWithWriter(element);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean remove(Object key, boolean doNotNotifyCacheReplicators)
			throws IllegalStateException {
		return delegateHolder.getDelegate().remove(key,
				doNotNotifyCacheReplicators);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean remove(Object key) throws IllegalStateException {
		return delegateHolder.getDelegate().remove(key);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAll() throws IllegalStateException, CacheException {
		delegateHolder.getDelegate().removeAll();
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAll(boolean doNotNotifyCacheReplicators)
			throws IllegalStateException, CacheException {
		delegateHolder.getDelegate().removeAll(doNotNotifyCacheReplicators);
	}

}
