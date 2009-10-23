package net.sf.ehcache.server.soap.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getStatisticsAccuracyResponse complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="getStatisticsAccuracyResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="return" type="{http://soap.server.ehcache.sf.net/}statisticsAccuracy" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getStatisticsAccuracyResponse", propOrder = {
        "_return"
})
public class GetStatisticsAccuracyResponse {

    @XmlElement(name = "return")
    protected StatisticsAccuracy _return;

    /**
     * Gets the value of the return property.
     *
     * @return possible object is
     *         {@link StatisticsAccuracy }
     */
    public StatisticsAccuracy getReturn() {
        return _return;
    }

    /**
     * Sets the value of the return property.
     *
     * @param value allowed object is
     *              {@link StatisticsAccuracy }
     */
    public void setReturn(StatisticsAccuracy value) {
        this._return = value;
    }

}
