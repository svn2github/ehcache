package net.sf.ehcache.search.query;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.search.Query;

/**
 * @author Anthony Dahanne
 */
public class TestQueryManagerBuilder {
    public static QueryManagerBuilder getQueryManagerBuilder() {
        return new QueryManagerBuilder(QM.class);
    }

    public static class QM implements QueryManager {
        @Override
        public Query createQuery(String statement) throws CacheException {
            return null;
        }
    }
}
