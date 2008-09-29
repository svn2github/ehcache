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


package net.sf.ehcache.distribution;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.*;
import java.rmi.*;


/**
 * Enables manual playing around with RMI ports.
 */
public class RMIPortUsage extends UnicastRemoteObject {

    private static RMIPortUsage rmi;

    protected RMIPortUsage(int port) throws RemoteException {
        super(port);
    }

    public static void main(String[] args) throws Exception {
        int port = 10001;
        String service = "rmi://127.0.0.1" + ':' + port + '/' +
                "test";
        rmi = new RMIPortUsage(port);
        LocateRegistry.createRegistry(port);

        //Ports being listend to: 
        //Sep 26 21:29:52 Greg-Lucks-Laptop Firewall[57]: java is listening from ::ffff:0.0.0.0:10001 uid = 501 proto=6
        Naming.rebind(service, rmi);


        //Ports being listened to:
        //Sep 26 21:29:52 Greg-Lucks-Laptop Firewall[57]: java is listening from ::ffff:0.0.0.0:10001 uid = 501 proto=6
        //Sep 26 21:30:36 Greg-Lucks-Laptop Firewall[57]: java is listening from ::ffff:127.0.0.1:57915 uid = 501 proto=6
        Thread.sleep(100000);

    }

}


