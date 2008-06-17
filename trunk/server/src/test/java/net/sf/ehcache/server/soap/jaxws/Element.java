
package net.sf.ehcache.server.soap.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for element complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="element">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="creationTime" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="eternal" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="mimeType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="resourceUri" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="timeToIdle" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="timeToLive" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/>
 *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "element", propOrder = {
    "creationTime",
    "eternal",
    "key",
    "mimeType",
    "resourceUri",
    "timeToIdle",
    "timeToLive",
    "value",
    "version"
})
public class Element {

    protected long creationTime;
    protected boolean eternal;
    protected Object key;
    protected String mimeType;
    protected String resourceUri;
    protected int timeToIdle;
    protected int timeToLive;
    protected byte[] value;
    protected long version;

    /**
     * Gets the value of the creationTime property.
     * 
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Sets the value of the creationTime property.
     * 
     */
    public void setCreationTime(long value) {
        this.creationTime = value;
    }

    /**
     * Gets the value of the eternal property.
     * 
     */
    public boolean isEternal() {
        return eternal;
    }

    /**
     * Sets the value of the eternal property.
     * 
     */
    public void setEternal(boolean value) {
        this.eternal = value;
    }

    /**
     * Gets the value of the key property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getKey() {
        return key;
    }

    /**
     * Sets the value of the key property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setKey(Object value) {
        this.key = value;
    }

    /**
     * Gets the value of the mimeType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the value of the mimeType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMimeType(String value) {
        this.mimeType = value;
    }

    /**
     * Gets the value of the resourceUri property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResourceUri() {
        return resourceUri;
    }

    /**
     * Sets the value of the resourceUri property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResourceUri(String value) {
        this.resourceUri = value;
    }

    /**
     * Gets the value of the timeToIdle property.
     * 
     */
    public int getTimeToIdle() {
        return timeToIdle;
    }

    /**
     * Sets the value of the timeToIdle property.
     * 
     */
    public void setTimeToIdle(int value) {
        this.timeToIdle = value;
    }

    /**
     * Gets the value of the timeToLive property.
     * 
     */
    public int getTimeToLive() {
        return timeToLive;
    }

    /**
     * Sets the value of the timeToLive property.
     * 
     */
    public void setTimeToLive(int value) {
        this.timeToLive = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setValue(byte[] value) {
        this.value = ((byte[]) value);
    }

    /**
     * Gets the value of the version property.
     * 
     */
    public long getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     */
    public void setVersion(long value) {
        this.version = value;
    }

}
