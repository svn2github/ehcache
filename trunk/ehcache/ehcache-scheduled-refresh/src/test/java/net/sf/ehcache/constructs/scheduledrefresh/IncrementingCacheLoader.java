package net.sf.ehcache.constructs.scheduledrefresh;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.loader.CacheLoader;

class IncrementingCacheLoader implements CacheLoader {
	private final int incrementalAmount;
	private boolean matchEvens;

	public IncrementingCacheLoader(boolean matchEvens, int inc) {
		this.incrementalAmount = inc;
		this.matchEvens = matchEvens;
	}

	@Override
	public Map loadAll(Collection keys, Object argument) {
		return loadAll(keys);
	}

	@Override
	public Map loadAll(Collection keys) {

		HashMap ret = new HashMap();
		for (Object k : keys) {
			Object got = load(k);
			if (got != null)
				ret.put(k, got);
		}
		return ret;
	}

	@Override
	public Object load(Object key, Object argument) {
		return load(key);
	}

	@Override
	public Object load(Object key) throws CacheException {
		if (key instanceof Number) {
			int ivalue = ((Number) key).intValue();
			if ((((ivalue & 0x01) != 0) && !matchEvens)
					|| ((ivalue & 0x01) == 0) && matchEvens) {
				int next = ivalue + incrementalAmount;
				return next + "";
			} else {
				return null;
			}
		}
		return key.toString();
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public Status getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() throws CacheException {
		// TODO Auto-generated method stub

	}

	@Override
	public CacheLoader clone(Ehcache cache)
			throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return "IncrementingCacheLoader [matchEvens=" + matchEvens
				+ ", incrementalAmount=" + incrementalAmount + "]";
	}
}