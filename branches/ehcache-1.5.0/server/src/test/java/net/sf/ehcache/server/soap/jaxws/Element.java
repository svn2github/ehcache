
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
 *         &lt;element name="eternal" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="mimeType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="resourceUri" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="timeToIdleSeconds" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="timeToLiveSeconds" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/>
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
    "timeToIdleSeconds",
    "timeToLiveSeconds",
    "value"
})
public class Element {

    protected Boolean eternal;
    protected Object key;
    protected String mimeType;
    protected String resourceUri;
    protected Integer timeToIdleSeconds;
    protected Integer timeToLiveSeconds;
    protected byte[] value;

    /**
     * Gets the value of the eternal property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEternal() {
        return eternal;
    }

    /**
     * Sets the value of the eternal property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEternal(Boolean value) {
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
     * Gets the value of the timeToIdleSeconds property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }

    /**
     * Sets the value of the timeToIdleSeconds property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setTimeToIdleSeconds(Integer value) {
        this.timeToIdleSeconds = value;
    }

    /**
     * Gets the value of the timeToLiveSeconds property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    /**
     * Sets the value of the timeToLiveSeconds property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setTimeToLiveSeconds(Integer value) {
        this.timeToLiveSeconds = value;
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

}
