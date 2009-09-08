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
import javax.xml.ws.BindingProvider;
import java.io.IOException;


/**
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class SecurityEnvironmentHandler implements CallbackHandler {

    private static final UnsupportedCallbackException UNSUPPORTED =
            new UnsupportedCallbackException(null, "Unsupported Callback Type Encountered");


    /**
     * Creates a new instance of SecurityEnvironmentHandler
     */
    public SecurityEnvironmentHandler() {
    }


    /**
     * Handle a security callback
     *
     * @param callbacks
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof PasswordValidationCallback) {
                PasswordValidationCallback cb = (PasswordValidationCallback) callback;
                if (cb.getRequest() instanceof PasswordValidationCallback.PlainTextPasswordRequest) {
                    cb.setValidator(new PlainTextPasswordValidator());

                } else if (cb.getRequest() instanceof PasswordValidationCallback.DigestPasswordRequest) {
                    PasswordValidationCallback.DigestPasswordRequest request =
                            (PasswordValidationCallback.DigestPasswordRequest) cb.getRequest();
                    String username = request.getUsername();
                    if ("Ron".equals(username)) {
                        request.setPassword("noR");
                        cb.setValidator(new PasswordValidationCallback.DigestPasswordValidator());
                    }
                }
            } else if (callback instanceof UsernameCallback) {
                UsernameCallback cb = (UsernameCallback) callback;
                String username = (String) cb.getRuntimeProperties().get(BindingProvider.USERNAME_PROPERTY);
                System.out.println("Got Username......... : " + username);
                cb.setUsername(username);

            } else if (callback instanceof PasswordCallback) {
                PasswordCallback cb = (PasswordCallback) callback;
                String password = (String) cb.getRuntimeProperties().get(BindingProvider.PASSWORD_PROPERTY);
                System.out.println("Got Password......... : " + password);
                cb.setPassword(password);

            } else {
                throw UNSUPPORTED;
            }
        }
    }

    /**
     * Plain text validator
     */
    private class PlainTextPasswordValidator implements PasswordValidationCallback.PasswordValidator {

        /**
         * Validate password
         * @param request
         * @return
         * @throws PasswordValidationCallback.PasswordValidationException
         */
        public boolean validate(PasswordValidationCallback.Request request)
                throws PasswordValidationCallback.PasswordValidationException {

            PasswordValidationCallback.PlainTextPasswordRequest plainTextRequest =
                    (PasswordValidationCallback.PlainTextPasswordRequest) request;

            String password = System.getProperty(plainTextRequest.getUsername());
            return plainTextRequest.getPassword().equals(password);
        }
    }


}