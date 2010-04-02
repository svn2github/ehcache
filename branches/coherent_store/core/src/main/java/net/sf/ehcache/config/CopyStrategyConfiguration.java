package net.sf.ehcache.config;

import net.sf.ehcache.store.compound.CopyStrategy;

/**
 * @author Alex Snaps
 */
public class CopyStrategyConfiguration {

    private volatile String className = "net.sf.ehcache.store.compound.SerializationCopyStrategy";
    private CopyStrategy strategy;

    public String getClassName() {
        return className;
    }

    public void setClass(final String className) {
        this.className = className;
    }

    public synchronized CopyStrategy getCopyStrategyInstance() {
        if (strategy == null) {
            Class copyStrategy = null;
            try {
                copyStrategy = Class.forName(className);
                strategy = (CopyStrategy) copyStrategy.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Couldn't find the CopyStrategy class!", e);
            } catch (InstantiationException e) {
                throw new RuntimeException("Couldn't instantiate the CopyStrategy instance!", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Couldn't instantiate the CopyStrategy instance!", e);
            } catch (ClassCastException e) {
                throw new RuntimeException(copyStrategy != null ? copyStrategy.getSimpleName()
                                                                  + " doesn't implement net.sf.ehcache.store.compound.CopyStrategy"
                    : "Error with CopyStrategy", e);
            }
        }
        return strategy;
    }
}
