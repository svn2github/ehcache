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

package net.sf.ehcache.management.sampled;

/**
 * <p>A utility class for sampled ehcache constructs.</p>
 *
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
class Utils {
    /**
     * Create a standard RuntimeException from the input, if necessary.
     *
     * @param e the runtime exception to act on
     * @return either the original input or a new, standard RuntimeException
     */
    static RuntimeException newPlainException(RuntimeException e) {
        String type = e.getClass().getName();
        if (type.startsWith("java.") || type.startsWith("javax.")) {
            return e;
        } else {
            RuntimeException result = new RuntimeException(e.getMessage());
            result.setStackTrace(e.getStackTrace());
            return result;
        }
    }
}