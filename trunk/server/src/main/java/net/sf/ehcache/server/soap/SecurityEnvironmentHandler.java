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

package net.sf.ehcache.server.soap;

import com.sun.xml.wss.impl.callback.PasswordCallback;
import com.sun.xml.wss.impl.callback.PasswordValidationCallback;
import com.sun.xml.wss.impl.callback.UsernameCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class SecurityEnvironmentHandler implements CallbackHandler {

    /**
     * Creates a new instance of SecurityEnvironmentHandler
     */
    public SecurityEnvironmentHandler(String arg) {
    }

    private String readLine() throws IOException {
        return new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    /**
     * Handle callbacks. This one asks for a username and password. todo this is just a sample
     * @param callbacks
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof PasswordValidationCallback) {
                PasswordValidationCallback callback = (PasswordValidationCallback) callbacks[i];
                if (callback.getRequest() instanceof PasswordValidationCallback.PlainTextPasswordRequest) {
                    callback.setValidator(new PlainTextPasswordValidator());

                } else if (callback.getRequest() instanceof PasswordValidationCallback.DigestPasswordRequest) {
                    PasswordValidationCallback.DigestPasswordRequest request =
                            (PasswordValidationCallback.DigestPasswordRequest) callback.getRequest();
                    String username = request.getUsername();
                    if ("Greg".equals(username)) {
                        request.setPassword("Luck");
                        callback.setValidator(new PasswordValidationCallback.DigestPasswordValidator());
                    }
                }
            } else if (callbacks[i] instanceof UsernameCallback) {
                UsernameCallback cb = (UsernameCallback) callbacks[i];
                System.out.println("Username: ");
                String username = readLine();
                if (username != null) {
                    cb.setUsername(username);
                }

            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback cb = (PasswordCallback) callbacks[i];
                System.out.println("Password: ");
                String password = readLine();
                if (password != null) {
                    cb.setPassword(password);
                }
            } else {
                throw new UnsupportedCallbackException(null, "Valid callbacks are username and password");
            }
        }
    }

    /**
     * Hardcoded sample validattor todo fix
     */
    private class PlainTextPasswordValidator implements PasswordValidationCallback.PasswordValidator {
        public boolean validate(PasswordValidationCallback.Request request)
                throws PasswordValidationCallback.PasswordValidationException {

            PasswordValidationCallback.PlainTextPasswordRequest plainTextRequest =
                    (PasswordValidationCallback.PlainTextPasswordRequest) request;
            if ("Greg".equals(plainTextRequest.getUsername()) &&
                    "Luck".equals(plainTextRequest.getPassword())) {
                return true;
            }
            return false;
        }
    }
}
