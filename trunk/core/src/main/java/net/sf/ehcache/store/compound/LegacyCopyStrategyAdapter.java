package net.sf.ehcache.store.compound;

import net.sf.ehcache.Element;

/**
 * ReadWriteCopyStrategy adaptor for a legacy CopyStrategy instance
 * @author Ludovic Orban
 */
public class LegacyCopyStrategyAdapter implements ReadWriteCopyStrategy<Element> {

    private final CopyStrategy legacyCopyStrategy;

    /**
     * create a LegacyCopyStrategyAdapter
     * @param legacyCopyStrategy the legacy CopyStrategy to adapt
     */
    public LegacyCopyStrategyAdapter(CopyStrategy legacyCopyStrategy) {
        this.legacyCopyStrategy = legacyCopyStrategy;
    }

    /**
     * {@inheritDoc}
     */
    public Element copyForWrite(Element value) {
        return legacyCopyStrategy.copy(value);
    }

    /**
     * {@inheritDoc}
     */
    public Element copyForRead(Element storedValue) {
        return legacyCopyStrategy.copy(storedValue);
    }
}
