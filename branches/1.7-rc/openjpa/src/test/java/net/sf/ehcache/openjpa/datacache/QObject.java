package net.sf.ehcache.openjpa.datacache;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.openjpa.persistence.DataCache;

@Entity
@DataCache(name="Cache#2")
public class QObject {
    private static long idCounter = System.currentTimeMillis();
    
    @Id
    private long id;
    
    private String value;
    
    public QObject() {
        this("");
    }
    
    public QObject(String value) {
        id = idCounter++;
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
