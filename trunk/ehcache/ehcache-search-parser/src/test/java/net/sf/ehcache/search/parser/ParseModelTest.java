package net.sf.ehcache.search.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.search.parser.EhcacheSearchParser;
import net.sf.ehcache.search.parser.ParseException;
import net.sf.ehcache.search.parser.MAggregate;
import net.sf.ehcache.search.parser.MAttribute;
import net.sf.ehcache.search.parser.MCriteria;
import net.sf.ehcache.search.parser.MOrderBy;
import net.sf.ehcache.search.parser.MValue;
import net.sf.ehcache.search.parser.ParseModel;
import net.sf.ehcache.search.parser.MCriteria.Simple;

import org.junit.Assert;
import org.junit.Test;

public class ParseModelTest {

  public static enum Foo { Bar, Baz };
  
  @Test
  public void testSimpleSelect() throws ParseException {
    String test = "select key, value from foo where name = 10";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Simple mcrit = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"10"));
    Assert.assertTrue(model.getCriteria().equals(mcrit));
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());

    Assert.assertFalse(model.isIncludedTargetStar());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertTrue(model.isIncludedTargetValues());
    Assert.assertTrue(model.getIncludedTargetAgregators().isEmpty());
    Assert.assertTrue(model.getIncludedTargetAttributes().isEmpty());
  }


  @Test
  public void testSimpleSelectStar() throws ParseException {
    String test = "select * from foo where name = 10";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Simple mcrit = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"10"));
    Assert.assertTrue(model.getCriteria().equals(mcrit));
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());

    Assert.assertTrue(model.isIncludedTargetStar());
    Assert.assertFalse(model.isIncludedTargetKeys());
    Assert.assertFalse(model.isIncludedTargetValues());
    Assert.assertTrue(model.getIncludedTargetAgregators().isEmpty());
    Assert.assertTrue(model.getIncludedTargetAttributes().isEmpty());
  }

  @Test
  public void testSimpleSelectLimit() throws ParseException {
    String test = "select key, value from foo where name = 10 limit 10";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Simple mcrit = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"10"));
    Assert.assertTrue(model.getCriteria().equals(mcrit));
    Assert.assertTrue(model.isLimited());
    Assert.assertTrue(model.getLimit() == 10);
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    
    Assert.assertFalse(model.isIncludedTargetStar());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertTrue(model.isIncludedTargetValues());
    Assert.assertTrue(model.getIncludedTargetAgregators().isEmpty());
    Assert.assertTrue(model.getIncludedTargetAttributes().isEmpty());
  }

  @Test
  public void testSimpleSelectOrderBy() throws ParseException {
    String test = "select key,value from foo where name = 10 order by age descending";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Simple mcrit = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"10"));
    Assert.assertTrue(model.getCriteria().equals(mcrit));
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().size() == 1);
    MOrderBy ord = model.getOrderBy().get(0);
    Assert.assertFalse(ord.isOrderAscending());
    Assert.assertEquals(ord.getAttribute().getName(), "age");
  }

  @Test
  public void testSimpleSelectGroupBy() throws ParseException {
    String test = "select key,value from foo where name = 10 group by age";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Simple mcrit = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"10"));
    Assert.assertTrue(model.getCriteria().equals(mcrit));
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getOrderBy().size() == 0);
    Assert.assertTrue(model.getGroupBy().size() == 1);
    MAttribute grp = model.getGroupBy().get(0);
    Assert.assertEquals(grp.getName(), "age");
  }

  @Test
  public void testSimpleSelectSum() throws ParseException {
    String test = "select sum(age) from foo where name = 10";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Simple mcrit = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"10"));
    Assert.assertTrue(model.getCriteria().equals(mcrit));
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    Assert.assertTrue(model.getIncludedTargetAgregators().size()==1);
    MAggregate agg = model.getIncludedTargetAgregators().get(0);
    Assert.assertEquals(agg.getAttribute().getName(), "age");
    Assert.assertEquals(agg.getOp(), MAggregate.AggOp.Sum);
  }

  @Test
  public void testSimpleSelectKeyAndSum() throws ParseException {
    String test = "select key, sum(age) from foo where name = 10";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Simple mcrit = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"10"));
    Assert.assertTrue(model.getCriteria().equals(mcrit));
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertFalse(model.isIncludedTargetValues());
    MAggregate agg = model.getIncludedTargetAgregators().get(0);
    Assert.assertEquals(agg.getAttribute().getName(), "age");
    Assert.assertEquals(agg.getOp(), MAggregate.AggOp.Sum);
  }

  @Test
  public void testSelectWithAnd() throws ParseException {
    String test = "select key, value from foo where (name = 'Chris' and age = 21)";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertTrue(model.isIncludedTargetValues());

    Simple mcrit1 = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MString(null,"Chris"));
    Simple mcrit2 = new MCriteria.Simple(new MAttribute("age"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"21"));
    MCriteria.And andCrit = new MCriteria.And(mcrit1, mcrit2);
    Assert.assertTrue(model.getCriteria().equals(andCrit));

  }

  @Test
  public void testSelectWithTripleAnd() throws ParseException {
    String test = "select key, value from foo where (name = 'Chris' and age = 21 and zip != '911')";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertTrue(model.isIncludedTargetValues());

    Simple mcrit1 = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MString(null,"Chris"));
    Simple mcrit2 = new MCriteria.Simple(new MAttribute("age"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"21"));
    Simple mcrit3 = new MCriteria.Simple(new MAttribute("zip"), MCriteria.SimpleOp.NE, new MValue.MString(null,"911"));
    MCriteria.And andCrit = new MCriteria.And(mcrit1, mcrit2, mcrit3);
    Assert.assertTrue(model.getCriteria().equals(andCrit));

  }

  @Test
  public void testSelectWithNestedAnd() throws ParseException {
    String test = "select key, value from foo where (name = 'Chris' and (age = 21 and zip != '911'))";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertTrue(model.isIncludedTargetValues());

    Simple mcrit1 = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MString(null,"Chris"));
    Simple mcrit2 = new MCriteria.Simple(new MAttribute("age"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"21"));
    Simple mcrit3 = new MCriteria.Simple(new MAttribute("zip"), MCriteria.SimpleOp.NE, new MValue.MString(null,"911"));
    MCriteria.And andCrit = new MCriteria.And(mcrit1, new MCriteria.And(mcrit2, mcrit3));
    Assert.assertTrue(model.getCriteria().equals(andCrit));

  }

  @Test
  public void testSelectWithOr() throws ParseException {
    String test = "select key, value from foo where (name = 'Chris' or age = 21)";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertTrue(model.isIncludedTargetValues());

    Simple mcrit1 = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MString(null,"Chris"));
    Simple mcrit2 = new MCriteria.Simple(new MAttribute("age"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"21"));
    MCriteria.Or andCrit = new MCriteria.Or(mcrit1, mcrit2);
    Assert.assertTrue(model.getCriteria().equals(andCrit));

  }

  @Test
  public void testSelectWithTripleOr() throws ParseException {
    String test = "select key, value from foo where (name = 'Chris' or age = 21 or zip != '911')";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertTrue(model.isIncludedTargetValues());

    Simple mcrit1 = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MString(null,"Chris"));
    Simple mcrit2 = new MCriteria.Simple(new MAttribute("age"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"21"));
    Simple mcrit3 = new MCriteria.Simple(new MAttribute("zip"), MCriteria.SimpleOp.NE, new MValue.MString(null,"911"));
    MCriteria.Or andCrit = new MCriteria.Or(mcrit1, mcrit2, mcrit3);
    Assert.assertTrue(model.getCriteria().equals(andCrit));

  }

  @Test
  public void testSelectWithNestedOr() throws ParseException {
    String test = "select key, value from foo where (name = 'Chris' or (age = 21 or zip != '911'))";
    EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(test));
    ParseModel model = parser.QueryStatement();
    Assert.assertFalse(model.isLimited());
    Assert.assertTrue(model.getGroupBy().isEmpty());
    Assert.assertTrue(model.getOrderBy().isEmpty());
    Assert.assertTrue(model.isIncludedTargetKeys());
    Assert.assertTrue(model.isIncludedTargetValues());

    Simple mcrit1 = new MCriteria.Simple(new MAttribute("name"), MCriteria.SimpleOp.EQ, new MValue.MString(null,"Chris"));
    Simple mcrit2 = new MCriteria.Simple(new MAttribute("age"), MCriteria.SimpleOp.EQ, new MValue.MInt(null,"21"));
    Simple mcrit3 = new MCriteria.Simple(new MAttribute("zip"), MCriteria.SimpleOp.NE, new MValue.MString(null,"911"));
    MCriteria.Or andCrit = new MCriteria.Or(mcrit1, new MCriteria.Or(mcrit2, mcrit3));
    Assert.assertTrue(model.getCriteria().equals(andCrit));

  }


  @Test
  public void testParseSuccesses() throws IOException {
    InputStream is = this.getClass().getResourceAsStream("/parseable.txt");
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      int success = 0;
      int cnt=1;
      Map<String, Throwable> fails = new HashMap<String, Throwable>();
      for (String line = br.readLine(); line != null; line = br.readLine()) {
        line = line.trim();
        if (line.length() > 0 && !line.startsWith("//")) {
          try {
            EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(line));
            @SuppressWarnings("unused")
            ParseModel model = parser.QueryStatement();
            success++;
          } catch (Throwable e) {
            fails.put(cnt+": "+line, e);
          }
        }
        cnt++;
      }
      if (fails.isEmpty()) {
        System.out.println(this.getClass().getName() + ".testParseSuccesses(): " + success + " statements parsed");
      } else {
        System.out.println(this.getClass().getName() + ".testParseSuccesses(): Failures:");
        for (String fail : fails.keySet()) {
          System.out.println(fail);
          System.out.println("\t" + fails.get(fail));
        }
        Assert.fail("parsing failure");
      }

    } finally {
      is.close();
    }
  }

  @Test
  public void testUnparseableFails() throws IOException {
    InputStream is = this.getClass().getResourceAsStream("/unparseable.txt");
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      int success = 0;
      Set<Class<?>> errs=new HashSet<Class<?>>();
      List<String> succeeds = new ArrayList<String>();

      int cnt=1;
      for (String line = br.readLine(); line != null; line = br.readLine()) {
        line = line.trim();
        if (line.length() > 0 && !line.startsWith("//")) {
          EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(line));
          try {
            parser.QueryStatement();
            succeeds.add(cnt+": "+line);
          } catch (Throwable r) {
            errs.add(r.getClass());
          }
        }
        cnt++;
      }
      if (!succeeds.isEmpty()) {
        System.out.println(this.getClass().getName() + ".testUnparseableFails(): Successes:");
        for (String fail : succeeds) {
          System.out.println(fail);
        }
        Assert.fail("parsing failure");
      }
      for(Class<?> c:errs) {
        System.out.println(c.getName());
      }
    } finally {
      is.close();
    }
  }

}
