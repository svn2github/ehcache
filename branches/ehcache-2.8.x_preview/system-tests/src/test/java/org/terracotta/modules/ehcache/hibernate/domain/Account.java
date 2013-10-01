/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package org.terracotta.modules.ehcache.hibernate.domain;

public class Account {

  private Long   id;
  private Person person;

  public Account() {
    // 
  }
  
  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String toString() {
    return super.toString();
  }
}
