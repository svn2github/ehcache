/**
 *  Copyright 2003-2006 Greg Luck
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache;

import java.io.Serializable;

/**
 * A pre JDK1.5 compatible enum class to indicate the status of a {@link CacheManager} or {@link Cache}.
 * <p/>
 * ehcache historically used int values for status. This is unsuitable for third party use thus this class.
 * Methods are provided to convert from the int status values to enum values and vice versa.
 *
 * @author Greg Luck
 * @version $Id: Status.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 * @since 1.2
 */
public final class Status implements Serializable {
    /**
     * The cache is uninitialised. It cannot be used.
     */
    public static final Status STATUS_UNINITIALISED = new Status(0, "STATUS_UNINITIALISED");
    /**
     * The cache is alive. It can be used.
     */
    public static final Status STATUS_ALIVE = new Status(1, "STATUS_ALIVE");
    /**
     * The cache is shudown. It cannot be used.
     */
    public static final Status STATUS_SHUTDOWN = new Status(2, "STATUS_SHUTDOWN");

    private static final Status[] STATUSES = {STATUS_UNINITIALISED, STATUS_ALIVE, STATUS_SHUTDOWN};

    private final String name;
    private final int intValue;

    private Status(int intValue, String name) {
        this.intValue = intValue;
        this.name = name;

    }

    /**
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p/>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return name;
    }

    /**
     * @param statusAsInt an int argument between 1 and 3
     * @return an enum Status
     * @throws IllegalArgumentException if the argument is not between 1 and 3
     */
    public static Status convertIntToStatus(int statusAsInt) throws IllegalArgumentException {
        if ((statusAsInt < STATUS_UNINITIALISED.intValue) || (statusAsInt > STATUS_SHUTDOWN.intValue)) {
            throw new IllegalArgumentException("int value of statuses must be between 1 and three");
        }
        return STATUSES[statusAsInt];
    }

    /**
     * @return the int value of this status. For backward compatibility with ehcache versions below 1.2
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p/>
     * The <code>equals</code> method implements an equivalence relation
     * on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     * <code>x</code>, <code>x.equals(x)</code> should return
     * <code>true</code>.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     * <code>x</code> and <code>y</code>, <code>x.equals(y)</code>
     * should return <code>true</code> if and only if
     * <code>y.equals(x)</code> returns <code>true</code>.
     * <li>It is <i>transitive</i>: for any non-null reference values
     * <code>x</code>, <code>y</code>, and <code>z</code>, if
     * <code>x.equals(y)</code> returns <code>true</code> and
     * <code>y.equals(z)</code> returns <code>true</code>, then
     * <code>x.equals(z)</code> should return <code>true</code>.
     * <li>It is <i>consistent</i>: for any non-null reference values
     * <code>x</code> and <code>y</code>, multiple invocations of
     * <tt>x.equals(y)</tt> consistently return <code>true</code>
     * or consistently return <code>false</code>, provided no
     * information used in <code>equals</code> comparisons on the
     * objects is modified.
     * <li>For any non-null reference value <code>x</code>,
     * <code>x.equals(null)</code> should return <code>false</code>.
     * </ul>
     * <p/>
     * The <tt>equals</tt> method for class <code>Object</code> implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values <code>x</code> and
     * <code>y</code>, this method returns <code>true</code> if and only
     * if <code>x</code> and <code>y</code> refer to the same object
     * (<code>x == y</code> has the value <code>true</code>).
     * <p/>
     * Note that it is generally necessary to override the <tt>hashCode</tt>
     * method whenever this method is overridden, so as to maintain the
     * general contract for the <tt>hashCode</tt> method, which states
     * that equal objects must have equal hash codes.
     *
     * @param object the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     * @see #hashCode()
     * @see java.util.Hashtable
     */
    public boolean equals(Object object) {
        if (!(object instanceof Status)) {
            return false;
        }
        return ((Status) object).intValue == intValue;
    }

    /**
     * Equality checker when the comparison object is of the same type
     * @param status the status to check
     * @return true is the statuses are the same
     */
    public boolean equals(Status status) {
        return intValue == status.intValue;
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * <p/>
     * The general contract of <code>hashCode</code> is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     * an execution of a Java application, the <tt>hashCode</tt> method
     * must consistently return the same integer, provided no information
     * used in <tt>equals</tt> comparisons on the object is modified.
     * This integer need not remain consistent from one execution of an
     * application to another execution of the same application.
     * <li>If two objects are equal according to the <tt>equals(Object)</tt>
     * method, then calling the <code>hashCode</code> method on each of
     * the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     * according to the {@link Object#equals(Object)}
     * method, then calling the <tt>hashCode</tt> method on each of the
     * two objects must produce distinct integer results.  However, the
     * programmer should be aware that producing distinct integer results
     * for unequal objects may improve the performance of hashtables.
     * </ul>
     * <p/>
     * As much as is reasonably practical, the hashCode method defined by
     * class <tt>Object</tt> does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java<font size="-2"><sup>TM</sup></font> programming language.)
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see java.util.Hashtable
     */
    public int hashCode() {
        return intValue;
    }

}
