
package net.sf.ehcache.server.soap.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for statistics complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="statistics">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="averageGetTime" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *         &lt;element name="cacheHits" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="evictionCount" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="inMemoryHits" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="onDiskHits" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="statisticsAccuracy" type="{http://soap.server.ehcache.sf.net/}statisticsAccuracy" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "statistics", propOrder = {
    "averageGetTime",
    "cacheHits",
    "evictionCount",
    "inMemoryHits",
    "onDiskHits",
    "statisticsAccuracy"
})
public class Statistics {

    protected float averageGetTime;
    protected long cacheHits;
    protected long evictionCount;
    protected long inMemoryHits;
    protected long onDiskHits;
    protected StatisticsAccuracy statisticsAccuracy;

    /**
     * Gets the value of the averageGetTime property.
     * 
     */
    public float getAverageGetTime() {
        return averageGetTime;
    }

    /**
     * Sets the value of the averageGetTime property.
     * 
     */
    public void setAverageGetTime(float value) {
        this.averageGetTime = value;
    }

    /**
     * Gets the value of the cacheHits property.
     * 
     */
    public long getCacheHits() {
        return cacheHits;
    }

    /**
     * Sets the value of the cacheHits property.
     * 
     */
    public void setCacheHits(long value) {
        this.cacheHits = value;
    }

    /**
     * Gets the value of the evictionCount property.
     * 
     */
    public long getEvictionCount() {
        return evictionCount;
    }

    /**
     * Sets the value of the evictionCount property.
     * 
     */
    public void setEvictionCount(long value) {
        this.evictionCount = value;
    }

    /**
     * Gets the value of the inMemoryHits property.
     * 
     */
    public long getInMemoryHits() {
        return inMemoryHits;
    }

    /**
     * Sets the value of the inMemoryHits property.
     * 
     */
    public void setInMemoryHits(long value) {
        this.inMemoryHits = value;
    }

    /**
     * Gets the value of the onDiskHits property.
     * 
     */
    public long getOnDiskHits() {
        return onDiskHits;
    }

    /**
     * Sets the value of the onDiskHits property.
     * 
     */
    public void setOnDiskHits(long value) {
        this.onDiskHits = value;
    }

    /**
     * Gets the value of the statisticsAccuracy property.
     * 
     * @return
     *     possible object is
     *     {@link StatisticsAccuracy }
     *     
     */
    public StatisticsAccuracy getStatisticsAccuracy() {
        return statisticsAccuracy;
    }

    /**
     * Sets the value of the statisticsAccuracy property.
     * 
     * @param value
     *     allowed object is
     *     {@link StatisticsAccuracy }
     *     
     */
    public void setStatisticsAccuracy(StatisticsAccuracy value) {
        this.statisticsAccuracy = value;
    }

}
