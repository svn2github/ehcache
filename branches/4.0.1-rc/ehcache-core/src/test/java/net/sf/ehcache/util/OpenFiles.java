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

package net.sf.ehcache.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class OpenFiles {

    public static void dumpOpenFiles() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // in we wanted to we could use HANDLE.EXE for windows here someday
            return;
        }

        String[] paths = new String[] {"/usr/bin/lsof", "/usr/sbin/lsof", "/bin/lsof"};
        for (String path : paths) {
            if (new File(path).exists()) {
                run(path);
                return;
            }
        }

        run("lsof");
    }

    private static void run(String cmd) {
        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream();
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                System.err.println("[OpenFiles] " + line);
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
