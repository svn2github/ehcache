/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.AbstractCacheWriter;

import org.terracotta.api.ClusteringToolkit;

import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.TransactionManager;

public class CacheWriterBTMTxClient extends AbstractTxClient {

  public CacheWriterBTMTxClient(String[] args) {
    super(args);
    Configuration config = TransactionManagerServices.getConfiguration();
    config.setServerId("simpletx-cachewriter-" + Math.random());
    config.setJournal("null");
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {

    PoolingDataSource pds1 = new PoolingDataSource();
    pds1.setClassName("org.apache.derby.jdbc.EmbeddedXADataSource");
    pds1.setUniqueName("derby1");
    pds1.setMaxPoolSize(1);
    pds1.getDriverProperties().setProperty("databaseName", "target/derby-db1");
    pds1.getDriverProperties().setProperty("createDatabase", "create");
    pds1.init();

    final TransactionManagerLookup lookup = new DefaultTransactionManagerLookup();
    final TransactionManager manager = lookup.getTransactionManager();

    DerbyCacheWriter writer = new DerbyCacheWriter(pds1);

    manager.begin();
    // Creating a database table
    Connection conn = pds1.getConnection();
    Statement sta = conn.createStatement();
    try {
    sta.executeUpdate("DROP TABLE EHCACHE_ELEMENT_TABLE");
    } catch(Exception e) {
      //ignore if table already exists...
    }
    sta.executeUpdate("CREATE TABLE EHCACHE_ELEMENT_TABLE (ID INT, EHCACHE_KEY VARCHAR(20)," + " EHCACHE_VALUE VARCHAR(20))");
    System.out.println("Table created.");
    sta.close();

    conn.close();

    manager.commit();

    manager.begin();

    cache.registerCacheWriter(writer);
    cache.putWithWriter(new Element("key1", "value1"));

    manager.commit();

    manager.begin();
    Element match = writer.get("key1");
    System.out.println("element = " + match);

    if (match == null) { throw new AssertionError("should not be null, since put by writer"); }

    manager.commit();
  }

  private static class DerbyCacheWriter extends AbstractCacheWriter {

    private final PoolingDataSource pds;
    private final AtomicInteger     uniqueIds = new AtomicInteger();

    public DerbyCacheWriter(PoolingDataSource pds) {
      this.pds = pds;
    }

    public void write(Element element) throws CacheException {
      try {
        Connection conn = pds.getConnection();
        PreparedStatement stmt = conn.prepareStatement("insert into EHCACHE_ELEMENT_TABLE values(?,?,?)");
        stmt.setInt(1, uniqueIds.incrementAndGet());
        stmt.setString(2, String.valueOf(element.getKey()));
        stmt.setString(3, String.valueOf(element.getValue()));
        stmt.executeUpdate();
        stmt.close();
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    public void delete(CacheEntry entry) throws CacheException {
      try {
        Connection conn = pds.getConnection();
        String deleteString = "DELETE FROM EHCACHE_ELEMENT_TABLE WHERE EHCACHE_KEY = '" + entry.getKey() + "'";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(deleteString);
        stmt.close();
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    public Element get(String key) {
      try {
        Connection conn = pds.getConnection();
        PreparedStatement s = conn.prepareStatement("SELECT EHCACHE_KEY, EHCACHE_VALUE FROM EHCACHE_ELEMENT_TABLE WHERE EHCACHE_KEY = ?");
        s.setString(1, key);
        ResultSet rs = s.executeQuery();
        if (rs.next()) {

          String keyStr = rs.getString("EHCACHE_KEY");
          String valueStr = rs.getString("EHCACHE_VALUE");
          System.out.println(" Ehcache key = " + keyStr + ", value = " + valueStr);
          return new Element(keyStr, valueStr);
        }
        rs.close();
        s.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  public static void main(String[] args) {
    new CacheWriterBTMTxClient(args).run();
  }

}
