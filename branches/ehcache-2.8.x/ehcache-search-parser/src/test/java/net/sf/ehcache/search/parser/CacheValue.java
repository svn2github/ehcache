package net.sf.ehcache.search.parser;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
 

/**
 * @author Alex Snaps
 */
public class CacheValue implements Serializable {

  private static final long serialVersionUID = -7902875028420080125L;
  final Object value;
  final Map<String, Object> nvPairs = new HashMap<String, Object>();

  public CacheValue(final Object value) {
    this.value = value;
  }

  public CacheValue(final Object value, final Map<String, Object> nvPairs) {
    this.value = value;
    if (nvPairs != null) {
      for (Map.Entry<String, Object> entry : nvPairs.entrySet()) {
        this.nvPairs.put(entry.getKey(), entry.getValue());
      }
    }
  }

  public Object getValue() {
    return value;
  }

  public Object getValue(final String key) {
    return nvPairs.get(key);
  }


  public Map<String, Object> getNvPairs() {
    return Collections.unmodifiableMap(nvPairs);
  }
}