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
package com.razshare.elkserver.Controller.WebSocket;

import com.razshare.elkserver.WebSocket.WebSocketController;
import com.razshare.elkserver.WebSocket.WebSocketEvent;
import java.util.ArrayList;

/**
 *
 * @author razvan
 */
public class ControllerNotFound extends WebSocketController{

    @Override
    public void onOpen(WebSocketEvent e, ArrayList<String> args) {
        e.send("Elk server error: Controller not found");
        e.close();
    }

    @Override
    public void onMessage(WebSocketEvent e, byte[] data, ArrayList<String> args) {}

    @Override
    public void onClose(WebSocketEvent e, ArrayList<String> args) {}
    
}