package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * @author Ludovic Orban
 */
public class ConstantSizeOfEngine implements SizeOfEngine {

    private final long keySize;
    private final long valueSize;
    private final long containerSize;

    public ConstantSizeOfEngine() {
        this(
            1536,  /* 1.5 KB*/
            14336, /* 14 KB */
            512    /* 0.5 KB */
        );
    }

    public ConstantSizeOfEngine(long keySize, long valueSize, long containerSize) {
        this.keySize = keySize;
        this.valueSize = valueSize;
        this.containerSize = containerSize;
    }

    public Size sizeOf(Object key, Object value, Object container) {
        long result = 0L;

        if (key != null) {
            result += keySize;
        }
        if (value != null) {
            result += valueSize;
        }
        if (container != null) {
            result += containerSize;
        }

        return new Size(result, true);
    }

    public SizeOfEngine copyWith(int maxDepth, boolean abortWhenMaxDepthExceeded) {
        return new ConstantSizeOfEngine();
    }
}
