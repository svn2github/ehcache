package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementComparer;

/**
 * @author Ludovic Orban
 */
public class ArraysAwareElementComparer implements ElementComparer {

    public boolean fullElementEquals(Element e1, Element e2) {
        if (e1.equals(e2)) {
            if (e1.getObjectValue() == null) {
                return e2.getObjectValue() == null;
            } else {
                return compareValues(e1.getObjectValue(), e2.getObjectValue());
            }
        } else {
            return false;
        }
    }

    private boolean compareValues(Object objectValue1, Object objectValue2) {
        if (objectValue1 instanceof Object[] && objectValue2 instanceof Object[]) {
            Object[] objectValue1Array = (Object[]) objectValue1;
            Object[] objectValue2Array = (Object[]) objectValue2;
            if (objectValue1Array.length != objectValue2Array.length) {
                return false;
            }

            for (int i = 0; i < objectValue1Array.length; i++) {
                Object o1 = objectValue1Array[i];
                Object o2 = objectValue2Array[i];

                boolean equals = compareValues(o1, o2);
                if (!equals) {
                    return false;
                }
            }
            return true;
        } else {
            if (objectValue1 == null) {
                return objectValue2 == null;
            } else {
                return objectValue1.equals(objectValue2);
            }
        }
    }

}
