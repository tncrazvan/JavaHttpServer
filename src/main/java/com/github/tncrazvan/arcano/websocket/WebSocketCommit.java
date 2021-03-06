/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.tncrazvan.arcano.websocket;

import static com.github.tncrazvan.arcano.SharedObject.LOGGER;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

/**
 *
 * @author Razvan Tanase
 */
public final class WebSocketCommit {
    
    private boolean binary = false;
    
    public boolean isBinary(){
        return binary;
    }

    private WebSocketGroup group = null;
    
    public WebSocketGroup getWebSocketGroup(){
        return group;
    }
    /**
     * Contains the payload of the message.
     * You can send this back to the current client or a different web socket client.
     */
    public byte[] data;

    /**
     * Get the payload of the message as a String. The String is encoded to UTF-8 by default.
     * @return the payload of the message as a String.
     */
    @Override
    public final String toString(){
        try {
            return new String(data,"UTF-8");
        } catch (final UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return "";
        }
    }

    /**
     * Get the payload of the message as a String.
     * 
     * @param charset charset to use when decoding the String.
     * @return the payload of the message as a String.
     * @throws java.io.UnsupportedEncodingException
     */
    public final String toString(final String charset) throws UnsupportedEncodingException {
        return new String(data, charset);
    }

    /**
     * Identifies a WebSocketMessage. This object contains the payload of the
     * message.
     */
    public WebSocketCommit() {
    }

    /**
     * Set the payload of the message. The server will read and send this data to
     * the client.
     * 
     * @param data bytes to set
     */
    public final void setBytes(final byte[] data) {
        this.data = data;
    }

    /**
     * Get the first byte of the payload.
     * 
     * @return the first byte of the payload.
     */
    public final byte toByte() {
        return data[0];
    }

    /**
     * Set the payload of the message.
     * 
     * @param data payload to set.
     */
    public final void setByte(final byte data) {
        this.data = new byte[] { data };
    }

    /**
     * Identifies a WebSocketMessage.
     * 
     * @param data the payload of the message.
     */
    public WebSocketCommit(final byte[] data) {
        this.data = data;
    }
    
    /**
     * Identifies a WebSocketMessage.
     * 
     * @param data the payload of the message.
     * @param group  the group of clients that should receive the payload.
     */
    public WebSocketCommit(final byte[] data, WebSocketGroup group) {
        this.data = data;
        this.group = group;
    }
    
    /**
     * Identifies a WebSocketMessage.
     * 
     * @param data the payload of the message.
     * @param group  the group of clients that should receive the payload.
     * @param binary the WebSocket standard requires the server to specify when the
     *               content of the message should be trated as binary or not. If
     *               this value is true, the server will set the binary flag to 0x82
     *               otherwise it will be set to 0x81. Note that this won't encode
     *               or convert your data in any way.
     */
    public WebSocketCommit(final byte[] data, WebSocketGroup group, boolean binary) {
        this.data = data;
        this.group = group;
        this.binary = binary;
    }

    /**
     * Set the payload of the message.
     * 
     * @param data    the payload of the message.
     * @param charset the charset used to encode the payload.
     * @throws java.io.UnsupportedEncodingException
     */
    public final void setString(final String data, final String charset) throws UnsupportedEncodingException {
        this.data = data.getBytes(charset);
    }

    /**
     * Identifies a WebSocketMessage.
     * 
     * @param data    the payload of the message.
     * @param charset the charset used to encode the payload.
     * @throws java.io.UnsupportedEncodingException
     */
    public WebSocketCommit(final String data, final String charset) throws UnsupportedEncodingException {
        this.data = data.getBytes(charset);
    }

    /**
     * Set the payload of the message.
     * 
     * @param data the payload of the message. The payload will be encoded to
     *             charset UTF-8 by default.
     */
    public final void setString(final String data) {
        try {
            this.data = data.getBytes("UTF-8");
        } catch (final UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Identifies a WebSocketMessage.
     * 
     * @param data the payload of the message. The payload will be encoded to
     *             charset UTF-8 by default.
     */
    public WebSocketCommit(final String data) {
        this.data = data.getBytes();
    }
}
