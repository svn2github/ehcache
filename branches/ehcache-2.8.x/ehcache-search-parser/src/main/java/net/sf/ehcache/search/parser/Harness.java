package net.sf.ehcache.search.parser;

import java.io.StringReader;

import net.sf.ehcache.search.parser.EhcacheSearchParser;
 
/**
 * Created with IntelliJ IDEA.
 * User: cschanck
 * Date: 3/26/13
 * Time: 3:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class Harness {
  public static void main(String[] args) throws Exception {
    /*
    {
      EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader("'foo is\\n 100.1 a g\\t\\u0065ood \\'bar\\' i " +
        "think'"));
      String got= parser.SingleQuotedString();
      System.out.println(got);
    }
    {
      EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader("select key, " +
        "'foo' where 'name' = (bool)'true'"));
      parser.Statement();
    }
    {
      EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader("select key " +
        " where ( 'name' = 'tom' and 'age' > 21 )" ));
      parser.Statement();
    }
    */

    /*
    {
      EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader("select key,value,'name', " +
        "average('age') " +
        " where  ('name' = 'tom' and (not ('age' = 10 or  'foo' > 11 )) and 'zip' = '21104') group by key order by " +
        "'name' desc limit 10"
      ));
      QModel model=parser.QueryStatement();
      System.out.println(model);
    }
    */
       
    {
      EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader("select * "+
        " where  ('name' = 'tom' and (not ('age' = (class foo.bar.Baz)'10' or  'foo' > 11 )) and 'zip' = '21104') group by key order by " +
        "'name' desc limit 10"
      ));
      ParseModel model=parser.QueryStatement();
      System.out.println(model);
    }

  }

}
