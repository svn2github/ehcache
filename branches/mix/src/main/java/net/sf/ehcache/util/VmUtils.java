/**
 *  Copyright Terracotta, Inc.
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

/**
 *
 */
package net.sf.ehcache.util;

/**
 * Misc. VM utilities.
 * 
 * @author Ludovic Orban
 */
public class VmUtils {

    private static boolean inGoogleAppEngine;

    static {
        try {
            Class.forName("com.google.apphosting.api.DeadlineExceededException");
            inGoogleAppEngine = true;
        } catch (ClassNotFoundException cnfe) {
            inGoogleAppEngine = false;
        }
    }

    /**
     * @return true if the code is being executed by Google's App Engine, false otherwise.
     */
    public static boolean isInGoogleAppEngine() {
        return inGoogleAppEngine;
    }
}
