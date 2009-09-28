
package net.sf.ehcache.server.soap.jaxws;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for statisticsAccuracy.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="statisticsAccuracy">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="STATISTICS_ACCURACY_NONE"/>
 *     &lt;enumeration value="STATISTICS_ACCURACY_BEST_EFFORT"/>
 *     &lt;enumeration value="STATISTICS_ACCURACY_GUARANTEED"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "statisticsAccuracy")
@XmlEnum
public enum StatisticsAccuracy {

    STATISTICS_ACCURACY_NONE,
    STATISTICS_ACCURACY_BEST_EFFORT,
    STATISTICS_ACCURACY_GUARANTEED;

    public String value() {
        return name();
    }

    public static StatisticsAccuracy fromValue(String v) {
        return valueOf(v);
    }

}
