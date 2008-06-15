
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
 *         &lt;element name="eternal" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="mimeType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="resourceUri" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="timeToIdle" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="timeToLive" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "eternal",
    "key",
    "mimeType",
    "resourceUri",
    "timeToIdle",
    "timeToLive"
})
public class Element {

    protected boolean eternal;
    protected String key;
    protected String mimeType;
    protected String resourceUri;
    protected int timeToIdle;
    protected int timeToLive;

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
     *     {@link String }
     *     
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the key property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKey(String value) {
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

}
