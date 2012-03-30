/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.distribution;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

public class SocketReusingRMISocketFactory extends RMISocketFactory {

    private final RMISocketFactory delegate;

    public SocketReusingRMISocketFactory(RMISocketFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = delegate.createSocket(host, port);
        socket.setReuseAddress(true);
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        ServerSocket socket = delegate.createServerSocket(port);
        socket.setReuseAddress(true);
        return socket;
    }
}
