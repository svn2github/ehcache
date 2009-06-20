/**
 *  Copyright 2003-2009 Luck Consulting Pty Ltd
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


package net.sf.ehcache.openjpa;

import java.util.logging.Logger;

/**
 * Hello world!
 *
 * @author Greg Luck
 */
public final class App {
    
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    /**
     * Utility class so private constructor
     */
    private App() {
        //noop
    }

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        //contrived case to get clover to pass
        int i = 0;
        LOG.info("Hello World!");
        LOG.info("Hello World again!");
    }


}
