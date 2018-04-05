/**
 * ElkServer is a Java library that makes it easier
 * to program and manage a Java servlet by providing different tools
 * such as:
 * 1) An MVC (Model-View-Controller) alike design pattern to manage 
 *    client requests without using any URL rewriting rules.
 * 2) A WebSocket Manager, allowing the server to accept and manage 
 *    incoming WebSocket connections.
 * 3) Direct access to every socket bound to every client application.
 * 4) Direct access to the headers of the incomming and outgoing Http messages.
 * Copyright (C) 2016-2018  Tanase Razvan Catalin
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.razshare.elkserver;

import com.google.gson.JsonObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import com.razshare.elkserver.Http.HttpEventListener;
import com.razshare.elkserver.SmtpServer.SmtpServer;
import java.io.File;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author Razvan
 */
public abstract class ElkServer extends Elk{
    private SmtpServer smtpServer;
    private static final String message = "\n\n"+
"  ElkServer is a Java library that makes it easier\n" +
"  to program and manage a Java servlet by providing different tools\n" +
"  such as:\n" +
"  1) An MVC (Model-View-Controller) alike design pattern to manage \n" +
"     client requests without using any URL rewriting rules.\n" +
"  2) A WebSocket Manager, allowing the server to accept and manage \n" +
"     incoming WebSocket connections.\n" +
"  3) Direct access to every socket bound to every client application.\n" +
"  4) Direct access to the headers of the incomming and outgoing Http messages.\n" +
"  Copyright (C) 2016-2018  Tanase Razvan Catalin\n" +
"  \n" +
"  This program is free software: you can redistribute it and/or modify\n" +
"  it under the terms of the GNU Affero General Public License as\n" +
"  published by the Free Software Foundation, either version 3 of the\n" +
"  License, or (at your option) any later version.\n" +
"  \n" +
"  This program is distributed in the hope that it will be useful,\n" +
"  but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
"  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
"  GNU Affero General Public License for more details.\n" +
"  \n" +
"  You should have received a copy of the GNU Affero General Public License\n" +
"  along with this program.  If not, see <https://www.gnu.org/licenses/>.\n\n";
    
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.security.NoSuchAlgorithmException
     */
    private static ElkServer server;
    
    public static ElkServer getServer(){
        return server;
    }
    
    public static void main (String[] args) throws IOException, NoSuchAlgorithmException{
        class ConsoleServlet extends ElkServer{
            @Override
            public void init() {
                System.out.println(message);
            }
        }
        
        
        server = new ConsoleServlet();
        server.listen(args);
        
    }
    
    public abstract void init();
    
    public SmtpServer getSmtpServer(){
        return smtpServer;
    }
    
    
    
    public void listen(String[] args) throws IOException, NoSuchAlgorithmException {
        String logLineSeparator = "\n=================================";

        Settings.parse(args[0]);
        System.out.println(logLineSeparator+"\n###Reading port");
        if(Settings.isset("port"))
            port = Settings.getInt("port");
        System.out.println("\t>>>port:"+port+" [OK]");
        
        System.out.println(logLineSeparator+"\n###Reading bindAddress");
        if(Settings.isset("bindAddress"))
            bindAddress = Settings.getString("bindAddress");
        System.out.println("\t>>>bindAddress:"+bindAddress+" [OK]");
        
        System.out.println(logLineSeparator+"\n###Reading webRoot");
        if(Settings.isset("webRoot"))
            webRoot = new File(args[0]).getParent()+"/"+Settings.getString("webRoot");
        System.out.println("\t>>>webRoot:"+webRoot+" [OK]");
        
        System.out.println(logLineSeparator+"\n###Reading charset");
        if(Settings.isset("charset"))
            charset = Settings.getString("charset");
        System.out.println("\t>>>charset:"+charset+" [OK]");
        
        System.out.println(logLineSeparator+"\n###Reading controllers");
        JsonObject controllers = Settings.get("controllers").getAsJsonObject();
        System.out.println("\t>>>controllers:[object] [OK]");
        
        System.out.println(logLineSeparator+"\n###Reading controllers.http");
        httpControllerPackageName = controllers.get("http").getAsString();
        System.out.println("\t>>>controllers.http:"+httpControllerPackageName+" [OK]");
        
        System.out.println(logLineSeparator+"\n###Reading controllers.websocket");
        wsControllerPackageName = controllers.get("websocket").getAsString();
        System.out.println("\t>>>controllers.websocket:"+wsControllerPackageName+" [OK]");

        //checking for SMTP server
        if(Settings.isset("smtp")){
            System.out.println(logLineSeparator+"\n###Reading smtp");
            JsonObject smtp = Settings.get("smtp").getAsJsonObject();
            System.out.println("\t>>>controllers:[object] [OK]");
            if(smtp.has("allow")){
                smtpAllowed = smtp.get("allow").getAsBoolean();
                System.out.println(logLineSeparator+"\t\n###Reading smtp.allow");
                System.out.println("\t\t>>>smtp.allow:"+smtpAllowed);
                if(smtpAllowed){
                    String smtpBindAddress = bindAddress;
                    if(smtp.has("bindAddress")){
                        smtpBindAddress = smtp.get("bindAddress").getAsString();
                    }
                    if(smtp.has("hostname")){
                        smtpServer = new SmtpServer(new ServerSocket(),smtpBindAddress,25,smtp.get("hostname").getAsString());
                        new Thread(smtpServer).start();
                        System.out.println("###Smtp server started.");
                    }else{
                        System.err.println("[WARNING] smtp.hostname is not defined. Smtp server won't start. [WARNING]");
                    }
                    
                }
            }
        }
        
        if(port == 443){
            System.out.println(logLineSeparator+"\n###Reading tls");
            JsonObject tls = Settings.get("tls").getAsJsonObject();
            
            System.out.println(logLineSeparator+"\t\n###Reading tls.certificate");
            String tls_certificate = tls.get("certificate").getAsString();
            System.out.println("\t\t>>>tls.certificate:"+tls_certificate+" [OK]");
            
            System.out.println(logLineSeparator+"\t\n###Reading tls.certificateType");
            String certificate_type = tls.get("certificateType").getAsString();
            System.out.println("\t\t>>>tls.certificate_type:"+certificate_type+" [OK]");
            
            System.out.println(logLineSeparator+"\t\n###Reading tls.password");
            String password = tls.get("password").getAsString();
            System.out.println("\t\t>>>tls.password:***[OK]");
            
            SSLContext sslContext = createSSLContext(webRoot+"../"+tls_certificate,certificate_type,password);
            
            
            // Create server socket factory
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

            // Create server socket
            SSLServerSocket ssl = (SSLServerSocket) sslServerSocketFactory.createServerSocket();
            ssl.bind(new InetSocketAddress(bindAddress, port));
            init();
            System.out.println("===== SERVER LISTENING =====");
            while(listen){
                new Thread(new HttpEventListener(ssl.accept())).start();
            }
        }else{
            ServerSocket ss = new ServerSocket();
            ss.bind(new InetSocketAddress(bindAddress, port));
            System.out.println("===== SERVER LISTENING =====");
            init();
            while(listen){
                new Thread(new HttpEventListener(ss.accept())).start();
            }
        }
        
    }
    
    private SSLContext createSSLContext(String tlsCertificate, String certificateType, String tlsPassword){
        System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
        try{
            KeyStore keyStore = KeyStore.getInstance(certificateType);
            keyStore.load(new FileInputStream(tlsCertificate),tlsPassword.toCharArray());
             
            // Create key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, tlsPassword.toCharArray());
            KeyManager[] km = keyManagerFactory.getKeyManagers();
             
            // Create trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();
             
            // Initialize SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(km,  tm, null);
             
            return sslContext;
        } catch (IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException ex){
            ex.printStackTrace();
        }
         
        return null;
    }
}
