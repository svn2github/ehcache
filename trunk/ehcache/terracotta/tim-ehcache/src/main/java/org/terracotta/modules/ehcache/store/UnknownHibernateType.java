/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * A dummy hibernate type used in CacheKeys returned from the cache via listeners and key iteration.
 */
public class UnknownHibernateType extends AbstractType {

  public int[] sqlTypes(Mapping mapping) {
    throw new UnsupportedOperationException();
  }

  public int getColumnSpan(Mapping mapping) {
    throw new UnsupportedOperationException();
  }

  public Class getReturnedClass() {
    throw new UnsupportedOperationException();
  }

  public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) {
    throw new UnsupportedOperationException();
  }

  public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) {
    throw new UnsupportedOperationException();
  }

  public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner) {
    throw new UnsupportedOperationException();
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session) {
    throw new UnsupportedOperationException();
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) {
    throw new UnsupportedOperationException();
  }

  public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) {
    throw new UnsupportedOperationException();
  }

  public String toLoggableString(Object value, SessionFactoryImplementor factory) {
    throw new UnsupportedOperationException();
  }

  public Object fromXMLNode(Node xml, Mapping factory) {
    throw new UnsupportedOperationException();
  }

  public String getName() {
    return "Unknown";
  }

  public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) {
    throw new UnsupportedOperationException();
  }

  public boolean isMutable() {
    throw new UnsupportedOperationException();
  }

  public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache) {
    throw new UnsupportedOperationException();
  }

  public boolean[] toColumnNullness(Object value, Mapping mapping) {
    throw new UnsupportedOperationException();
  }
}
