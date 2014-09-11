/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate.domain;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HolidayCalendar {
  private Long   id;
  private Map holidays = new HashMap(); // Date -> String
  
  public HolidayCalendar init() {
    DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
    try {
      holidays.clear();
      holidays.put(df.parse("2009.01.01"), "New Year's Day");
      holidays.put(df.parse("2009.02.14"), "Valentine's Day");
      holidays.put(df.parse("2009.11.11"), "Armistice Day");
    } catch (ParseException e) {
      System.out.println("Error parsing date string");
      throw new RuntimeException(e);
    }
    return this;
  }
  
  public Map getHolidays() {
    return holidays;
  }

  protected void setHolidays(Map holidays) {
    this.holidays = holidays;
  }

  public void addHoliday(Date d, String name) {
    holidays.put(d, name);
  }
  
  public String getHoliday(Date d) {
    return (String)holidays.get(d);
  }
  
  public boolean isHoliday(Date d) {
    return holidays.containsKey(d);
  }

  protected Long getId() {
    return id;
  }
  
  protected void setId(Long id) {
    this.id = id;
  }
}

