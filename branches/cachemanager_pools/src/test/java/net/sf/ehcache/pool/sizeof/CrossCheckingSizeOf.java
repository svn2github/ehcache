package net.sf.ehcache.pool.sizeof;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.pool.sizeof.AgentSizeOf;
import net.sf.ehcache.pool.sizeof.ReflectionSizeOf;
import net.sf.ehcache.pool.sizeof.SizeOf;
import net.sf.ehcache.pool.sizeof.UnsafeSizeOf;
import net.sf.ehcache.pool.sizeof.filter.PassThroughFilter;
import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;


public class CrossCheckingSizeOf extends SizeOf {

  private final List<SizeOf> engines;

  public CrossCheckingSizeOf() {
    this(new PassThroughFilter());
  }
  
  public CrossCheckingSizeOf(SizeOfFilter filter) {
    this(filter, true);
  }

  public CrossCheckingSizeOf(SizeOfFilter filter, boolean caching) {
    super(filter, caching);
    engines = new ArrayList<SizeOf>();

    try {
      engines.add(new AgentSizeOf());
    } catch (UnsupportedOperationException usoe) {
      System.err.println("Not using AgentSizeOf: " + usoe);
    }
    try {
      engines.add(new UnsafeSizeOf());
    } catch (UnsupportedOperationException usoe) {
      System.err.println("Not using UnsafeSizeOf: " + usoe);
    }
    try {
      engines.add(new ReflectionSizeOf());
    } catch (UnsupportedOperationException usoe) {
      System.err.println("Not using ReflectionSizeOf: " + usoe);
    }
    
    if (engines.isEmpty()) {
      throw new AssertionError("No SizeOf engines available");
    }
  }

  @Override
  protected long measureSizeOf(Object obj) {
    long[] values = new long[engines.size()];
    for (int i = 0; i < engines.size(); i++) {
      values[i] = engines.get(i).measureSizeOf(obj);
    }
    for (long value : values) {
      if (values[0] != value) {
        StringBuilder sb = new StringBuilder("Values do not match for ");
        sb.append(obj.getClass());
        if (obj.getClass().isArray()) {
           sb.append(" length:").append(Array.getLength(obj));
        }
        sb.append(" - ");
        for (int i = 0; i < engines.size(); i++) {
          sb.append(engines.get(i).getClass().getSimpleName()).append(":").append(values[i]);
          if (i != engines.size() - 1) {
            sb.append(", ");
          }
        }
        throw new AssertionError(sb.toString());
      }
    }
    
    return values[0];
  }
}
