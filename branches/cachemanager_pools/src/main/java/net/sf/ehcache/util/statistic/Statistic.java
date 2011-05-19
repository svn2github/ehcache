package net.sf.ehcache.util.statistic;

public interface Statistic {
  
  void event();

  long getCount();

  float getRate();  
}
