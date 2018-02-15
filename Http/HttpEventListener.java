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
package elkserver.Http;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import elkserver.ELK;
import elkserver.WebSocket.WebSocketEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 *
 * @author Razvan
 */
public class HttpEventListener extends HttpRequestReader{
    private final String sessionId;
    private Matcher matcher;
    private static final Pattern 
            upgradePattern = Pattern.compile("Upgrade"),
            keepAlivePattern = Pattern.compile("keep-alive"),
            websocketPattern = Pattern.compile("websocket"),
            http2Pattern = Pattern.compile("h2c");
    public HttpEventListener(Socket client) throws IOException, NoSuchAlgorithmException{
        super(client);
        sessionId = ELK.getSha1String(System.identityHashCode(client)+"::"+System.currentTimeMillis());
    }
    
    @Override
    public void onRequest(HttpHeader clientHeader, JsonObject post) {
        if(clientHeader != null && clientHeader.get("Connection")!=null){
            matcher = upgradePattern.matcher(clientHeader.get("Connection"));
            if(matcher.find()){
                matcher = websocketPattern.matcher(clientHeader.get("Upgrade"));
                //WebSocket connection
                if(matcher.find()){
                    try {
                        new WebSocketEvent(reader, client, clientHeader, sessionId).execute();
                    }catch(IOException e){
                        try {
                            client.close();
                        } catch (IOException ex) {
                            Logger.getLogger(HttpEventListener.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
                        Logger.getLogger(HttpEventListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }else{
                    matcher = http2Pattern.matcher(clientHeader.get("Upgrade"));
                    // Http 2.x connection
                    if(matcher.find()){
                        System.out.println("Http 2.0 connection detected. Not yet implemented.");
                    }
                }
            }else{
                //default connection, assuming it's Http 1.x
                try {
                    new HttpEvent(output,clientHeader,client,post).execute();
                } catch (IOException ex) {
                    try {
                        client.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(HttpEventListener.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
                
            } 
        }
    }
}
