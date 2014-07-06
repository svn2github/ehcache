/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.ElementData;
import net.sf.ehcache.EternalElementData;
import net.sf.ehcache.NonEternalElementData;

import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import junit.framework.Assert;

public class ValueModeHandlerSerializationTest {

  private static final String           KEY   = "key";
  private static final String VALUE = "value";
  private ValueModeHandlerSerialization valueModeHandler;
  private Element                       eternalElement;
  private Element                       nonEternalElement;

  @Before
  public void setUp() {
    valueModeHandler = new ValueModeHandlerSerialization();
    eternalElement = new Element(KEY, VALUE, 1, 10, 50, 10, false, 0, 0, 20);
    nonEternalElement = new Element(KEY, VALUE, 1, 10, 50, 10, false, 30, 0, 20);
  }


  @Test
  public void testCreateElementDataForEternalElement() {
    ElementData elementData = valueModeHandler.createElementData(eternalElement);
    Assert.assertTrue(elementData instanceof EternalElementData);
  }


  @Test
  public void testCreateElementDataForNonEternalElement() {
    ElementData elementData = valueModeHandler.createElementData(nonEternalElement);
    Assert.assertTrue(elementData instanceof NonEternalElementData);
  }


  @Test
  public void testCreateElementForEternalElement() {
    Element createdElement = valueModeHandler.createElement(KEY, new EternalElementData(eternalElement));
    Assert.assertTrue(createdElement.isEternal());
  }


  @Test
  public void testCreateElementForNonEternalElement() {
    Element createdElement = valueModeHandler.createElement(KEY, new NonEternalElementData(nonEternalElement));
    Assert.assertFalse(createdElement.isEternal());
  }


  @Test
  public void testSerializationForEternalElement() throws Exception {
    checkSerialization(eternalElement);
  }


  @Test
  public void testSerializationForNonEternalElement() throws Exception {
    checkSerialization(nonEternalElement);
  }


  private void checkSerialization(Element element) throws Exception {
    // Create ElementData and Serialize it
    ElementData eternalElementData = valueModeHandler.createElementData(element);
    serialize("eternalElement", eternalElementData);

    // Deserialize elemenData and create new Element
    Serializable deserialized = deserialize("eternalElement");
    Element deserializedElement = valueModeHandler.createElement(KEY, deserialized);

    // Assert deserialized Element is equal to original Element
    assertElementsAreEqual(element, deserializedElement);
  }


  private void assertElementsAreEqual(Element element1, Element element2) {
    Assert.assertEquals(element1.getCreationTime(), element2.getCreationTime());
    Assert.assertEquals(element1.getHitCount(), element2.getHitCount());
    Assert.assertEquals(element1.getLastAccessTime(), element2.getLastAccessTime());
    Assert.assertEquals(element1.getLastUpdateTime(), element2.getLastUpdateTime());
    Assert.assertEquals(element1.getTimeToIdle(), element2.getTimeToIdle());
    Assert.assertEquals(element1.getTimeToLive(), element2.getTimeToLive());
    Assert.assertEquals(element1.getVersion(), element2.getVersion());
    Assert.assertEquals((String) element1.getObjectKey(), (String) element2.getObjectKey());
    Assert.assertEquals((String) element1.getObjectValue(), (String) element2.getObjectValue());
  }

  private void serialize(String fileName, ElementData elementData) throws Exception {
    FileOutputStream fos = new FileOutputStream(fileName);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(elementData);
    oos.flush();
    oos.close();
  }

  private Serializable deserialize(String fileName) throws Exception {
    FileInputStream fis = new FileInputStream(fileName);
    ObjectInputStream ois = new ObjectInputStream(fis);
    return (Serializable) ois.readObject();
  }

}
