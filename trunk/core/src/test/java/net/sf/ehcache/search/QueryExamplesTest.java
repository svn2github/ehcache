package net.sf.ehcache.search;

import static net.sf.ehcache.search.expression.Logic.and;
import static net.sf.ehcache.search.expression.Logic.or;

import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.Count;
import net.sf.ehcache.search.aggregator.Sum;
import net.sf.ehcache.search.expression.And;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class was used to develop the API and now that the code has been written it has been made an
 * executable test.
 * 
 * @author teck
 * @author Greg Luck
 */
public class QueryExamplesTest {

    private static final Logger LOG = LoggerFactory.getLogger(QueryExamplesTest.class);

    private CacheManager cacheManager;
    private Ehcache cache;

    @Before
    public void before() {
        cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        cache = cacheManager.getCache("cache1");
        SearchTestUtil.populateData(cache);
    }

    @After
    public void after() {
        cacheManager.shutdown();
    }

    @Test
    public void testExamples() {
        examples();
    }

    void examples() throws SearchException {

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<String> gender = cache.getSearchAttribute("gender");
        Attribute<String> attr3 = cache.getSearchAttribute("name");

        Query query;
        Results results;
        Aggregator sum = new Sum();
        Aggregator count = new Count();

        // include all keys in the cache
        query = cache.createQuery().includeKeys().end();
        results = query.execute();
        for (Result result : results.all()) {
            LOG.info("" + result.getKey());
        }
        results.discard(); // not required but will speed resource freeing

        // access results in a "paged" manner
        query = cache.createQuery().includeKeys().end();
        results = query.execute();

        int pageSize = 100;
        int index = 0;
        List<Result> page;
        do {
            page = results.range(index, pageSize);

            for (Result result : page) {
                LOG.info("" + result.getKey());
            }

            index += page.size();
        } while (page.size() == pageSize);

        // select age from the cache
        query = cache.createQuery().includeAttribute(age).end();
        results = query.execute();
        for (Result result : results.all()) {
            LOG.info("" + result.getAttribute(age));
        }

        // select age, gender from the cache
        query = cache.createQuery().includeAttribute(age, gender).end();
        results = query.execute();
        for (Result result : results.all()) {
            LOG.info("" + result.getAttribute(age));
            // slf4j weirdness
            Object object = result.getAttribute(gender);
            LOG.info("" + object);
        }

        // select max(age) -- named indexed attribute "age"
        Query sumQuery = cache.createQuery().includeAggregator(sum, age).end();
        results = sumQuery.execute();
        Long sumResult = (Long) results.aggregateResult();
        LOG.info("Sum is: " + sumResult);

        // select keys with criteria age == 12 AND gender = "timmy"
        query = cache.createQuery().includeKeys().add(and(age.eq(12), gender.eq("timmy"))).end();

        // same as above without static import
        query = cache.createQuery().includeKeys().add(new And(age.eq(12), gender.eq("timmy"))).end();

        // same as above (but without AND, uses two add() -- multiple
        // criteria implies AND)
        query = cache.createQuery().includeKeys().add(age.eq(12)).add(gender.eq("timmy")).end();

        // slightly more complicated expression and multiple ordering
        // age = 13 OR (age == 12 AND gender = "timmy") order by age asc, gender desc limit 10
        query = cache.createQuery().includeKeys().add(or(age.eq(13), and(age.eq(12), gender.eq("timmy"))))
                .addOrder(age, Direction.ASCENDING).addOrder(gender, Direction.DESCENDING).maxResults(10).end();
    }
}
