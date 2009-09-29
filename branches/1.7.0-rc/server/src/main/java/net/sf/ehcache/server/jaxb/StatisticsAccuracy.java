/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.server.jaxb;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A StatisticsAccuracy Enum which is ordered the same as the core Ehcache
 * typesafe enum which is pre jdk 5.
 *
 * @author Greg Luck
 * @version $Id: Status.java 658 2008-06-14 08:11:41Z gregluck $
 */
@XmlRootElement
public enum StatisticsAccuracy {

    /**
     * Fast but not accurate setting.
     */
    STATISTICS_ACCURACY_NONE,

    /**
     * Best efforts accuracy setting.
     */
    STATISTICS_ACCURACY_BEST_EFFORT,

    /**
     * Guaranteed accuracy setting.
     */
    STATISTICS_ACCURACY_GUARANTEED;

    private static final StatisticsAccuracy[] ENUM_ARRAY =
            {STATISTICS_ACCURACY_NONE, STATISTICS_ACCURACY_BEST_EFFORT, STATISTICS_ACCURACY_GUARANTEED};

    /**
     * The code enum matching an int code
     * @param code an int status code
     * @return the enum matching
     */
    public static StatisticsAccuracy fromCode(int code) {
        return ENUM_ARRAY[code];
    }
}