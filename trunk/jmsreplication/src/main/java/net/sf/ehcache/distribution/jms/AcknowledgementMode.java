package net.sf.ehcache.distribution.jms;

import javax.jms.Session;

/**
 * An enum of JMS acknowledgement mode.
 */
public enum AcknowledgementMode {

    AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE),
    //CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE), not supported
    DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE),
    SESSION_TRANSACTED(Session.SESSION_TRANSACTED);

    private int mode;

    public static AcknowledgementMode forString(String value) {
        for (AcknowledgementMode mode : values()) {
            if (mode.name().equals(value)) {
                return mode;
            }
        }
        return DUPS_OK_ACKNOWLEDGE;
    }

    AcknowledgementMode(int mode) {
        this.mode = mode;
    }

    public int toInt() {
        return mode;
    }
}
