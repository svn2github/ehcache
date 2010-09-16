/**
 *  Copyright 2003-2010 Terracotta, Inc.
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


package net.sf.ehcache.googleappengine.gregrlucksamplejpaapp.guestbook;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;


/**
 * @author Greg Luck
 */
//@NamedQuery(
//    name="findAllEmployeesByFirstName",
//    query = "SELECT p FROM Greeting p WHERE p.param2 < :threshold ORDER BY p.param1 ascending")
@Entity(name = "Greeting")
public class Greeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;

    @Basic
    private User author;

    @Basic
    private String content;

    @Basic
    private Date date;




    public Greeting(User author, String content, Date date) {
        this.author = author;
        this.content = content;
        this.date = date;
    }

    public Greeting() {
    }

    public Key getKey() {
        return key;
    }

    public User getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public Date getDate() {
        return date;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
