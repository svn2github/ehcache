/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

class SearchConstants {

  static final String PUT_COMMAND                   = "PUT";
  static final String PUT_IF_ABSENT_COMMAND         = "PUT_IF_ABSENT";
  static final String REPLACE                       = "REPLACE";
  static final Object REMOVE_COMMAND                = "REMOVE";
  static final Object CLEAR_COMMAND                 = "CLEAR";
  static final Object REPLACE_COMMAND               = "REPLACE";
  static final Object REMOVE_IF_VALUE_EQUAL_COMMAND = "REMOVE_IF_VALUE_EQUAL";

  private SearchConstants() {
    //
  }

}
