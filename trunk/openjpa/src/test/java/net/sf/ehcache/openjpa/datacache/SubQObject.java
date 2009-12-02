package net.sf.ehcache.openjpa.datacache;

import org.apache.openjpa.persistence.DataCache;

import javax.persistence.Entity;

/**
 * @author Alex Snaps
 */
@Entity
@DataCache
public class SubQObject extends QObject {

    private String subProp;

    public SubQObject() {
    }

    public SubQObject(final String value, final String subProp) {
        super(value);
        this.subProp = subProp;
    }

    public String getSubProp() {
        return subProp;
    }

    public void setSubProp(final String subProp) {
        this.subProp = subProp;
    }
}
