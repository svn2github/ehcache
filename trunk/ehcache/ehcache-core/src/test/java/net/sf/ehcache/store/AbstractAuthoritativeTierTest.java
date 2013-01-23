package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alex Snaps
 */
public abstract class AbstractAuthoritativeTierTest<T extends AuthoritativeTier> {

    private T authoritativeTier;

    @Before
    public void setup() {
        authoritativeTier = createAuthoritativeTier();
    }

    @Test
    public void testMaintainsFaultedStateProperly() {
        final String key = "1";
        final Element one = new Element(key, "one is the value");
        final Element otherOne = new Element(key, "one isn't the value");

        authoritativeTier.removeAll();
        authoritativeTier.put(one);
        assertThat(isFaulted(key, authoritativeTier), is(false));
        authoritativeTier.putFaulted(one);
        assertThat(isFaulted(key, authoritativeTier), is(true));
        authoritativeTier.remove(key);
        assertThat(isFaulted(key, authoritativeTier), is(false));
        authoritativeTier.put(one);
        assertThat(isFaulted(key, authoritativeTier), is(false));
        authoritativeTier.remove(key);
        assertThat(isFaulted(key, authoritativeTier), is(false));
        authoritativeTier.putIfAbsent(one);
        assertThat(isFaulted(key, authoritativeTier), is(false));
        authoritativeTier.fault(key, false);
        assertThat(isFaulted(key, authoritativeTier), is(true));
        authoritativeTier.recalculateSize(key);
        assertThat(isFaulted(key, authoritativeTier), is(true));
        assertThat(authoritativeTier.replace(otherOne, one,
            new DefaultElementValueComparator(new CacheConfiguration())), is(false));
        assertThat(isFaulted(key, authoritativeTier), is(true));
        assertThat(authoritativeTier.replace(one, otherOne,
            new DefaultElementValueComparator(new CacheConfiguration())), is(true));
        assertThat(isFaulted(key, authoritativeTier), is(false));
        authoritativeTier.get(key);
        assertThat(isFaulted(key, authoritativeTier), is(false));
        authoritativeTier.fault(key, false);
        assertThat(isFaulted(key, authoritativeTier), is(true));
        authoritativeTier.get(key);
        assertThat(isFaulted(key, authoritativeTier), is(true));
        authoritativeTier.replace(otherOne);
        assertThat(isFaulted(key, authoritativeTier), is(false));
    }

    protected abstract T createAuthoritativeTier();

    protected abstract boolean isFaulted(Object key, T authoritativeTier);
}
