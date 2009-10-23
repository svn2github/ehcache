package net.sf.ehcache.server.soap.jaxws;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for status.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;simpleType name="status">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="STATUS_UNINITIALISED"/>
 *     &lt;enumeration value="STATUS_ALIVE"/>
 *     &lt;enumeration value="STATUS_SHUTDOWN"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "status")
@XmlEnum
public enum Status {

    STATUS_UNINITIALISED,
    STATUS_ALIVE,
    STATUS_SHUTDOWN;

    public String value() {
        return name();
    }

    public static Status fromValue(String v) {
        return valueOf(v);
    }

}
