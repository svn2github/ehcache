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

import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.FileInputStream;

import com.sun.xml.wss.ProcessingContext;
import com.sun.xml.wss.SubjectAccessor;
import com.sun.xml.wss.XWSSProcessorFactory;
import com.sun.xml.wss.XWSSProcessor;
import com.sun.xml.wss.XWSSecurityException;

/**
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class SecurityHandler implements SOAPHandler<SOAPMessageContext> {

    private String clientOrServer;
    private XWSSProcessor xwssServerProcessor;
    private XWSSProcessor xwssClientProcessor;


    /**
     * Creates a new instance of SecurityHandler
     */
    public SecurityHandler(String clientOrServer) {
        FileInputStream serverConfig = null;
        FileInputStream clientConfig = null;
        this.clientOrServer = clientOrServer;
        try {
            if ("client".equals(this.clientOrServer)) {
                //read client side security config
                clientConfig = new java.io.FileInputStream(
                        new java.io.File("user-pass-authenticate-client.xml"));
                //Create a XWSSProcessFactory.
                XWSSProcessorFactory factory = XWSSProcessorFactory.newInstance();
                xwssClientProcessor = factory.createProcessorForSecurityConfiguration(
                        clientConfig, new SecurityEnvironmentHandler("client"));
                clientConfig.close();
            } else {
                //read server side security configuration
                String serverConfigurationName = this.getClass().getClassLoader()
                        .getResource("user-pass-authenticate-server.xml").getFile();
                serverConfig = new java.io.FileInputStream(
                        new java.io.File(serverConfigurationName));
                //Create a XWSSProcessFactory.
                XWSSProcessorFactory factory = XWSSProcessorFactory.newInstance();
                xwssServerProcessor = factory.createProcessorForSecurityConfiguration(
                        serverConfig, new SecurityEnvironmentHandler("server"));
                serverConfig.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    /**
     *
     * @return
     */
    public Set<QName> getHeaders() {
        return null;
    }

    /**
     *
     * @param messageContext
     * @return
     */
    public boolean handleFault(SOAPMessageContext messageContext) {
        return true;
    }

    /**
     *
     * @param messageContext
     * @return
     */
    public boolean handleMessage(SOAPMessageContext messageContext) {
        if ("client".equals(this.clientOrServer)) {
            secureClient(messageContext);
        } else {
            secureServer(messageContext);
        }
        return true;
    }

    /**
     * 
     * @param messageContext
     */
    public void close(MessageContext messageContext) {
    }

    private void secureServer(SOAPMessageContext messageContext) {
        Boolean outMessageIndicator = (Boolean)
                messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        SOAPMessage message = messageContext.getMessage();

        if (outMessageIndicator.booleanValue()) {
            System.out.println("\nOutbound SOAP:");
            // do nothing....
            return;
        } else {
            System.out.println("\nInbound SOAP:");
            //verify the secured message.
            try {
                ProcessingContext context = xwssServerProcessor.createProcessingContext(message);
                context.setSOAPMessage(message);
                SOAPMessage verifiedMsg = null;
                verifiedMsg = xwssServerProcessor.verifyInboundMessage(context);
                System.out.println("\nRequester Subject " + SubjectAccessor.getRequesterSubject(context));
                messageContext.setMessage(verifiedMsg);
            } catch (XWSSecurityException ex) {
                //create a Message with a Fault in it
                //messageContext.setMessage(createFaultResponse(ex));
                ex.printStackTrace();
                throw new WebServiceException(ex);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new WebServiceException(ex);
            }
        }
    }

    /**
     * todo add code here
     * @param ex
     * @return
     */
    private SOAPMessage createFaultResponse(XWSSecurityException ex) {

        return null;
    }

    private void secureClient(SOAPMessageContext messageContext) {
        Boolean outMessageIndicator = (Boolean)
                messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        SOAPMessage message = messageContext.getMessage();
        if (outMessageIndicator.booleanValue()) {
            System.out.println("\nOutbound SOAP:");
            ProcessingContext context;
            try {
                context = xwssClientProcessor.createProcessingContext(message);
                context.setSOAPMessage(message);
                SOAPMessage secureMsg = xwssClientProcessor.secureOutboundMessage(context);
                secureMsg.writeTo(System.out);
                messageContext.setMessage(secureMsg);
            } catch (XWSSecurityException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return;
        } else {
            System.out.println("\nInbound SOAP:");
            //do nothing
            return;
        }
    }
}