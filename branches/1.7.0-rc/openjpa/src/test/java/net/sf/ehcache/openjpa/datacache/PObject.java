package net.sf.ehcache.openjpa.datacache;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.openjpa.persistence.DataCache;

@Entity
@DataCache(name="Cache#1")
public class PObject {
    @Id
    @GeneratedValue
    private long id;
    
    private String value;
    
    public PObject() {
        this("");
    }
    
    public PObject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getId() {
        return id;
    }
    
    
}
