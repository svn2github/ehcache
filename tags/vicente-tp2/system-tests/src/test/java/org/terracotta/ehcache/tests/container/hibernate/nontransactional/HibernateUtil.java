/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
  private static String         defaultConfig = "/hibernate-config/hibernate.cfg.xml";
  private static SessionFactory sessionFactory;
  private static Configuration  config;

  public synchronized static void configure(String configResource) {
    config = new Configuration().configure(configResource);
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
      config = new Configuration().configure(defaultConfig);
    }
    return config;
  }

  public synchronized static void closeSessionFactory() {
    if (sessionFactory != null) {
      sessionFactory.close();
      sessionFactory = null;
    }
  }
}
