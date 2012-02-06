/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import org.terracotta.collections.ClusteredMapConfigFields;
import org.terracotta.config.BaseConfiguration;
import org.terracotta.config.Configuration;
import org.terracotta.locking.LockType;

public class ClusteredMapConfigurationBuilder implements ClusteredMapConfigFields {
  private final BaseConfiguration config = new BaseConfiguration();

  public ClusteredMapConfigurationBuilder concurrency(int concurrency) {
    config.setInt(CONCURRENCY_FIELD_NAME, concurrency);
    return this;
  }

  public int getConcurrency() {
    return config.getInt(CONCURRENCY_FIELD_NAME);
  }

  public ClusteredMapConfigurationBuilder maxTTI(int maxTTI) {
    config.setInt(MAX_TTI_FIELD_NAME, maxTTI);
    return this;
  }

  public int getMaxTTI() {
    return config.getInt(MAX_TTI_FIELD_NAME);
  }

  public ClusteredMapConfigurationBuilder maxTTL(int maxTTL) {
    config.setInt(MAX_TTL_FIELD_NAME, maxTTL);
    return this;
  }

  public int getMaxTTL() {
    return config.getInt(MAX_TTL_FIELD_NAME);
  }

  public ClusteredMapConfigurationBuilder name(String name) {
    config.setString(CACHE_NAME_FIELD, name);
    return this;
  }

  public String getName() {
    return config.getString(CACHE_NAME_FIELD);
  }

  public ClusteredMapConfigurationBuilder maxTotalCount(int maxTotalCount) {
    config.setInt(MAX_TOTAL_COUNT_FIELD_NAME, maxTotalCount);
    return this;
  }

  public int getMaxTotalCount() {
    return config.getInt(MAX_TOTAL_COUNT_FIELD_NAME);
  }

  public ClusteredMapConfigurationBuilder localCacheEnabled(boolean localCacheEnabled) {
    config.setBoolean(LOCAL_CACHE_ENABLED_FIELD_NAME, localCacheEnabled);
    return this;
  }

  public boolean isLocalCacheEnabled() {
    return config.getBoolean(LOCAL_CACHE_ENABLED_FIELD_NAME);
  }

  public ClusteredMapConfigurationBuilder lockType(LockType lockType) {
    config.setString(LOCK_TYPE_FIELD_NAME, lockType.toString());
    return this;
  }

  public LockType getLockType() {
    return LockType.valueOf(config.getString(LOCK_TYPE_FIELD_NAME));
  }

  public ClusteredMapConfigurationBuilder invalidateOnChange(boolean invalidateOnChange) {
    config.setBoolean(INVALIDATE_ON_CHANGE_FIELD_NAME, invalidateOnChange);
    return this;
  }

  public boolean isInvalidateOnChange() {
    return config.getBoolean(INVALIDATE_ON_CHANGE_FIELD_NAME);
  }

  public ClusteredMapConfigurationBuilder localStoreManager(String localStoreManager) {
    config.setString(LOCAL_STORE_MANAGER_NAME_FIELD_NAME, localStoreManager);
    return this;
  }

  public String getLocalStoreManager() {
    return config.getString(LOCAL_STORE_MANAGER_NAME_FIELD_NAME);
  }

  public ClusteredMapConfigurationBuilder localStoreName(String localStoreName) {
    config.setString(LOCAL_STORE_NAME_FIELD_NAME, localStoreName);
    return this;
  }

  public String getLocalStoreName() {
    return config.getString(LOCAL_STORE_NAME_FIELD_NAME);
  }

  public ClusteredMapConfigurationBuilder serverMapLocalStoreFactory(String serverMapLocalStoreFactory) {
    config.setString(SERVERMAP_LOCAL_STORE_FACTORY_FIELD_NAME, serverMapLocalStoreFactory);
    return this;
  }

  public String getServerMapLocalStoreFactory() {
    return config.getString(SERVERMAP_LOCAL_STORE_FACTORY_FIELD_NAME);
  }

  public Configuration build() {
    return config;
  }
}
