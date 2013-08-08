/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.search.parser;

import java.io.StringReader;

public class Harness {
    public static void main(String[] args) throws Exception {
        {
            EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader("select * " +
                                                                                  " where  ('name' = 'tom' and (not ('age' = (class foo.bar.Baz)'10' or  'foo' > 11 )) and 'zip' = '21104') group by key order by " +
                                                                                  "'name' desc limit 10"
            ));
            ParseModel model = parser.QueryStatement();
            System.out.println(model);
        }

    }

}
