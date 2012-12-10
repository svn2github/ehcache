/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;
import org.terracotta.context.query.Query;

/**
 *
 * @author cdennis
 */
 enum OperationType {
    CACHE_GET(CacheOperationOutcomes.GetOutcome.class),
    CACHE_PUT(CacheOperationOutcomes.PutOutcome.class),
    CACHE_REMOVE(CacheOperationOutcomes.RemoveOutcome.class),
    
    HEAP_GET(StoreOperationOutcomes.GetOutcome.class),
    HEAP_PUT(StoreOperationOutcomes.PutOutcome.class),
    HEAP_REMOVE(StoreOperationOutcomes.RemoveOutcome.class),
    
    OFFHEAP_GET(StoreOperationOutcomes.GetOutcome.class),
    OFFHEAP_PUT(StoreOperationOutcomes.PutOutcome.class),
    OFFHEAP_REMOVE(StoreOperationOutcomes.RemoveOutcome.class),
    
    DISK_GET(StoreOperationOutcomes.GetOutcome.class),
    DISK_PUT(StoreOperationOutcomes.PutOutcome.class),
    DISK_REMOVE(StoreOperationOutcomes.RemoveOutcome.class),
    
    XA_COMMIT(XaCommitOutcome.class),
    XA_ROLLBACK(XaRollbackOutcome.class),
    XA_RECOVERY(XaRecoveryOutcome.class),
    
    SEARCH(CacheOperationOutcomes.SearchOutcome.class) {
        @Override
        long interval() {
            return 10;
        }
    },
    
    EVICTED(null),
    EXPIRED(null);

    private final Class<? extends Enum> type;
    
    private OperationType(Class<? extends Enum> type) {
        this.type = type;
    }

    final Class<? extends Enum> type() {
        return type;
    }
    
    Query query() {
        throw new UnsupportedOperationException();
    }

    int history() {
        return 30;
    }

    long interval() {
        return 1;
    }
    
    long window() {
        return 1;
    }
  
}
