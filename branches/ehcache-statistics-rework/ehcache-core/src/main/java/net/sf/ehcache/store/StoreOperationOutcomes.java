/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.store;

/**
 *
 * @author cdennis
 */
public interface StoreOperationOutcomes {

    public enum GetOutcome {
        HIT, MISS;
    }

    public enum PutOutcome { ADDED, UPDATED };
    public enum RemoveOutcome { SUCCESS };

}
