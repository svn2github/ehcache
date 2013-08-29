/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
  public static final String    DB_PORT_VARIABLE = "__PORT__";
  public static final String    DB_PORT_SYSPROP  = "HibernateUtil.DB_PORT";

  private static final String   defaultConfig    = "/hibernate-config/hibernate.cfg.xml";
  private static SessionFactory sessionFactory;
  private static Configuration  config;

  public synchronized static void configure(String configResource) {
    config = makeConfig(configResource);
  }

  public synchronized static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {
        sessionFactory = getConfig().buildSessionFactory();
      } catch (HibernateException ex) {
        System.err.println("Initial SessionFactory creation failed." + ex);
        throw new ExceptionInInitializerError(ex);
      }
    }
    return sessionFactory;
  }

  public synchronized static void dropAndCreateDatabaseSchema() {
    getConfig().setProperty("hibernate.hbm2ddl.auto", "create");
  }

  private synchronized static Configuration getConfig() {
    if (config == null) {
      config = makeConfig(defaultConfig);
    }
    return config;
  }

  private static Configuration makeConfig(String resource) {
    String dbPort = System.getProperty(DB_PORT_SYSPROP, null);
    if (dbPort == null) { throw new AssertionError("System property (" + DB_PORT_SYSPROP + ") not set"); }
    dbPort = dbPort.trim();

    Configuration cfg = new Configuration().configure(resource);

    String[] keys = new String[] { "connection.url", "hibernate.connection.url" };
    for (String key : keys) {
      String value = cfg.getProperty(key);
      value = value.replace(DB_PORT_VARIABLE, dbPort);
      cfg.setProperty(key, value);
    }

    return cfg;
  }

  public synchronized static void closeSessionFactory() {
    if (sessionFactory != null) {
      sessionFactory.close();
      sessionFactory = null;
    }
  }
}
