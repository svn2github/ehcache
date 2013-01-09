/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statistics.extended;

import java.util.HashSet;
import java.util.Set;
import net.sf.ehcache.Cache;
import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Query;

import static org.terracotta.context.query.QueryBuilder.*;
import static org.terracotta.context.query.Matchers.*;

/**
 *
 * @author cdennis
 */
class EhcacheQueryBuilder {

    static EhcacheQuery cache() {
        return new EhcacheQuery(queryBuilder().build()).children(Cache.class);
    }
    
    static EhcacheQuery descendants() {
        return new EhcacheQuery(queryBuilder().build()).descendants();
    }
    
    static class EhcacheQuery implements Query {
        
        private final Query query;

        private EhcacheQuery(Query query) {
            this.query = query;
        }
        
        EhcacheQuery children() {
            return new EhcacheQuery(queryBuilder().chain(query).children().build());
        }
        
        EhcacheQuery children(Class<?> klazz) {
            return new EhcacheQuery(queryBuilder().chain(query).children().filter(context(identifier(subclassOf(klazz)))).build());
        }
        
        EhcacheQuery descendants() {
            return new EhcacheQuery(queryBuilder().chain(query).descendants().build());
        }
        
        EhcacheQuery addDescendants() {
            return new EhcacheQuery(queryBuilder().chain(query).chain(new Query() {

                @Override
                public Set<TreeNode> execute(Set<TreeNode> input) {
                    Set<TreeNode> result = new HashSet<TreeNode>();
                    result.addAll(input);
                    result.addAll(queryBuilder().descendants().build().execute(input));
                    return result;
                }
            }).build());
        }
        
        EhcacheQuery exclude(Class<?> klazz) {
            return new EhcacheQuery(queryBuilder().chain(query).filter(context(identifier(not(subclassOf(klazz))))).build());
        }

        @Override
        public Set<TreeNode> execute(Set<TreeNode> input) {
            return query.execute(input);
        }
    }
}
