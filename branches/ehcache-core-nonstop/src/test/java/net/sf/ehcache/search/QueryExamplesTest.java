package net.sf.ehcache.search;

import static net.sf.ehcache.search.Query.KEY;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.SearchAttribute;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bsh.EvalError;
import bsh.Interpreter;

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
        Attribute<String> name = cache.getSearchAttribute("name");

        Query query;
        Results results;

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
        Query sumQuery = cache.createQuery().includeAggregator(age.sum()).end();
        results = sumQuery.execute();
        Long sumResult = (Long) results.getAggregatorResults().get(0);
        LOG.info("Sum is: " + sumResult);

        // select keys with criteria age == 12 AND gender = "timmy"
        query = cache.createQuery().includeKeys().addCriteria(age.eq(12).and(gender.eq("timmy"))).end();

        // same as above (but without AND, uses two add() -- multiple
        // criteria implies AND)
        query = cache.createQuery().includeKeys().addCriteria(age.eq(12)).addCriteria(gender.eq("timmy")).end();

        // slightly more complicated expression and multiple ordering
        // age = 13 OR (age == 12 AND gender = "timmy") order by age asc, gender desc limit 10
        query = cache.createQuery().includeKeys().addCriteria(age.eq(13).or(age.eq(12).and(gender.eq("timmy"))))
                .addOrderBy(age, Direction.ASCENDING).addOrderBy(gender, Direction.DESCENDING).maxResults(10).end();
    }

    @Test
    public void testNoIncludeSpecified() {
        Attribute<Integer> age = cache.getSearchAttribute("age");
        Results results = cache.createQuery().addCriteria(age.eq(35)).execute();
        assertEquals(0, results.size());
    }

    @Test
    public void testUseShorthandKeyAttribute() {
        Results results = cache.createQuery().addCriteria(KEY.eq(1)).includeKeys().execute();
        assertEquals(1, results.size());
        assertEquals(1, results.all().iterator().next().getKey());
    }

    @Test
    public void testUseShorthandValueAttribute() {
        Object tmpKey = new Object();
        Double value = Math.PI * System.nanoTime();
        cache.put(new Element(tmpKey, value));

        try {
            Results results = cache.createQuery().addCriteria(Query.VALUE.eq(value)).includeKeys().execute();
            assertEquals(1, results.size());
            assertEquals(tmpKey, results.all().iterator().next().getKey());
        } finally {
            cache.remove(tmpKey);
        }
    }

    @Test
    public void testIncludeKeysSpecified() {
        Attribute<Integer> age = cache.getSearchAttribute("age");
        Results results = cache.createQuery().addCriteria(age.eq(35)).includeKeys().execute();
        assertEquals(2, results.size());
        for (Result result : results.all()) {
            LOG.info("" + cache.get(result.getKey()));
        }
    }

    @Test
    public void testSearchKeys() {
        Results results = cache.createQuery().addCriteria(Query.KEY.in(Arrays.asList(1, 3))).includeKeys().execute();
        assertEquals(2, results.size());
        for (Result result : results.all()) {
            LOG.info("" + cache.get(result.getKey()));
        }
    }

    /**
     * Show how to execute a query in beanshell
     */
    @Test
    public void testBasicBeanShellQuery() throws EvalError {

        Interpreter i = new Interpreter();
        Query query = cache.createQuery().includeKeys();

        Attribute<Integer> age = cache.getSearchAttribute("age");

        i.set("query", query);
        Results results = null;
        i.set("results", results);
        i.set("age", age);

        i.eval("results = query.addCriteria(age.eq(35)).execute()");
        results = (Results) i.get("results");
        assertEquals(2, results.size());
        for (Result result : results.all()) {
            LOG.info("" + result.getKey());
        }
    }

    /**
     * Show how to execute a query in beanshell
     */
    @Test
    public void testInjectedStringBeanShellQuery() throws EvalError {

        Interpreter i = new Interpreter();
        Query query = cache.createQuery().includeKeys();

        Attribute<Integer> age = cache.getSearchAttribute("age");

        i.set("query", query);
        Results results = null;
        i.set("results", results);
        i.set("age", age);

        String userDefinedQuery = "age.eq(35)";
        String fullQueryString = "results = query.addCriteria(" + userDefinedQuery + ").execute()";

        i.eval(fullQueryString);
        results = (Results) i.get("results");
        assertEquals(2, results.size());
        for (Result result : results.all()) {
            LOG.info("" + result.getKey());
        }
    }

    /**
     * Show how to execute a query in beanshell using autodiscovered attributes
     */
    @Test
    public void testAutoDiscoveredAttributesBeanShellQuery() throws EvalError {

        Interpreter i = new Interpreter();

        // Auto discover the search attributes and add them to the interpreter's context
        Map<String, SearchAttribute> attributes = cache.getCacheConfiguration().getSearchAttributes();
        for (Map.Entry<String, SearchAttribute> entry : attributes.entrySet()) {
            i.set(entry.getKey(), cache.getSearchAttribute(entry.getKey()));
            LOG.info("Setting attribute " + entry.getKey());
        }

        // Define the query and results. Add things which would be set in the GUI i.e. includeKeys and add to context
        Query query = cache.createQuery().includeKeys();
        Results results = null;
        i.set("query", query);
        i.set("results", results);

        // This comes from the freeform text field
        String userDefinedQuery = "age.eq(35)";

        // Add the stuff on that we need
        String fullQueryString = "results = query.addCriteria(" + userDefinedQuery + ").execute()";

        i.eval(fullQueryString);
        results = (Results) i.get("results");
        assertEquals(2, results.size());
        for (Result result : results.all()) {
            LOG.info("" + result.getKey());
        }

    }

}
