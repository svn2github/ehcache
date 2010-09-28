package net.sf.ehcache.search;

import static net.sf.ehcache.search.expression.Logic.and;
import static net.sf.ehcache.search.expression.Logic.or;

import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.Count;
import net.sf.ehcache.search.aggregator.Sum;
import net.sf.ehcache.search.expression.And;

/**
 * This class is here just to make sure we have a template of how the API is supposed to look. When we have tests to cover this stuff we can
 * delete this
 * 
 * @author teck
 */
public class QueryExamples {

    void examples(Cache cache) throws SearchException {

        Attribute<Integer> attr1 = cache.getSearchAttribute("attr1");
        Attribute<String> attr2 = cache.getSearchAttribute("attr2");

        Query query;
        Results results;
        Aggregator sum = new Sum();
        Aggregator count = new Count();

        // include all keys in the cache
        query = cache.createQuery().includeKeys().end();
        results = query.execute();
        for (Result hit : results.all()) {
            System.err.println(hit.getKey());
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

            for (Result hit : page) {
                System.err.println(hit.getKey());
            }

            index += page.size();
        } while (page.size() == pageSize);

        // select attr1 from the cache
        query = cache.createQuery().includeAttribute(attr1).end();
        results = query.execute();
        for (Result hit : results.all()) {
            System.err.println(hit.getAttribute(attr1));
        }

        // select attr1, attr2 from the cache
        query = cache.createQuery().includeAttribute(attr1, attr2).end();
        results = query.execute();
        for (Result hit : results.all()) {
            System.err.println(hit.getAttribute(attr1));
            System.err.println(hit.getAttribute(attr2));
        }

        // select max(attr1) -- named indexed attribute "attr1"
        Query sumQuery = cache.createQuery().includeAggregator(sum, attr1).end();
        results = sumQuery.execute();
        Integer max = (Integer) results.aggregateResult();
        System.err.println(max);

        // select keys with criteria attr1 == 12 AND attr2 = "timmy"
        query = cache.createQuery().includeKeys().add(and(attr1.eq(12), attr2.eq("timmy"))).end();

        // same as above without static import
        query = cache.createQuery().includeKeys().add(new And(attr1.eq(12), attr2.eq("timmy"))).end();

        // same as above (but without AND, uses two add() -- multiple
        // criteria implies AND)
        query = cache.createQuery().includeKeys().add(attr1.eq(12)).add(attr2.eq("timmy")).end();

        // slightly more complicated expression and multiple ordering
        // attr1 = 13 OR (attr1 == 12 AND attr2 = "timmy") order by attr1 asc, attr2 desc limit 10
        query = cache.createQuery().includeKeys().add(or(attr1.eq(13), and(attr1.eq(12), attr2.eq("timmy"))))
                .addOrder(attr1, Direction.ASCENDING).addOrder(attr2, Direction.DESCENDING).maxResults(10).end();
    }
}
