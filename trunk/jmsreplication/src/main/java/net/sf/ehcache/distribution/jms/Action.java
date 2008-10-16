package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.distribution.EventMessage;

/**
     * Actions that tie in with EventMessage actions. EventMessage has not been updated to use enums.
 */
public enum Action {

    PUT(EventMessage.PUT),
    REMOVE(EventMessage.REMOVE),
    REMOVE_ALL(EventMessage.REMOVE_ALL),
    GET(10);

    private int action;

    public static Action forString(String value) {
        for (Action action : values()) {
            if (action.name().equals(value)) {
                return action;
            }
        }
        return null;
    }

    Action(int mode) {
        this.action = mode;
    }

    public int toInt() {
        return action;
    }
}
