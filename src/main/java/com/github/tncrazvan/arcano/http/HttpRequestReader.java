package com.github.tncrazvan.arcano.http;

import static com.github.tncrazvan.arcano.SharedObject.LOGGER;
import static com.github.tncrazvan.arcano.SharedObject.londonTimezone;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocket;

import com.github.tncrazvan.arcano.SharedObject;
import com.github.tncrazvan.arcano.websocket.WebSocketEvent;

/**
 *
 * @author Razvan Tanase
 */
public class HttpRequestReader implements Runnable {
    public Socket client;
    public SSLSocket secureClient = null;
    public final BufferedReader bufferedReader;
    public final BufferedWriter bufferedWriter;
    public final DataOutputStream output;
    public final DataInputStream input;
    public final StringBuilder outputString = new StringBuilder();
    public HttpContent content = null;
    public String[] location = new String[0];
    public String[] args = new String[0];
    private Matcher matcher;
    private SharedObject so;

    public static final DateTimeFormatter formatHttpDefaultDate = DateTimeFormatter
            .ofPattern("EEE, d MMM y HH:mm:ss z", Locale.US).withZone(londonTimezone);
    private static final Pattern UPGRADE_PATTERN = Pattern.compile("Upgrade"),
            WEB_SOCKET_PATTERN = Pattern.compile("websocket"), HTTP2_PATTERN = Pattern.compile("h2c");

    public HttpRequestReader(final SharedObject so, final Socket client) throws NoSuchAlgorithmException, IOException {
        this.so = so;
        this.client = client;
        bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        output = new DataOutputStream(client.getOutputStream());
        input = new DataInputStream(client.getInputStream());
    }

    @Override
    public final void run() {
        try {
            byte[] chain = new byte[] { 0, 0, 0, 0 };
            boolean keepReading = true, EOFException = false;
            while (keepReading) {
                try {
                    chain[3] = chain[2];
                    chain[2] = chain[1];
                    chain[1] = chain[0];
                    chain[0] = input.readByte();
                    outputString.append((char) chain[0]);
                    if ((char) chain[3] == '\r' && (char) chain[2] == '\n' && (char) chain[1] == '\r'
                            && (char) chain[0] == '\n') {
                        keepReading = false;
                    }
                } catch (final EOFException ex) {
                    keepReading = false;
                    EOFException = true;
                    // ex.printStackTrace();
                }
            }
            if (outputString.length() == 0) {
                client.close();
            } else {
                final HttpHeaders clientHeader = HttpHeaders.requestFromString(outputString.toString().trim());
                // outputString = new StringBuilder();
                final ArrayList<byte[]> inputList = new ArrayList<>();
                int length = 0;
                if (!EOFException) {
                    int chunkSize = 0;
                    if (clientHeader.isDefined("Content-Length")) {
                        chunkSize = Integer.parseInt(clientHeader.get("Content-Length"));
                    }

                    if (chunkSize > 0) {
                        chain = new byte[chunkSize];
                        input.readFully(chain);
                        inputList.add(chain);
                        length += chain.length;
                        // outputString.append(new String(chain,charset));
                    } else {
                        int offset = 0;
                        chain = new byte[so.config.http.mtu];
                        try {
                            if (input.available() > 0)
                                while (input.read(chain) > 0) {
                                    if (offset < so.config.http.mtu) {
                                        offset++;
                                    } else {
                                        // outputString.append(new String(chain,charset));
                                        inputList.add(chain);
                                        length += chain.length;
                                        offset = 0;
                                        chain = new byte[so.config.http.mtu];
                                    }
                                }
                        } catch (SocketTimeoutException | EOFException e) {
                            // outputString.append(new String(chain,charset));
                            length += chain.length;
                            inputList.add(chain);
                        }
                    }
                }
                final byte[] inputBytes = new byte[length];
                int pos = 0;
                for (final byte[] bytes : inputList) {
                    for (int i = 0; i < bytes.length; i++, pos++) {
                        inputBytes[pos] = bytes[i];
                    }
                }
                this.content = new HttpContent(clientHeader, inputBytes);
                String uri = content.headers.getResource();
                if (uri == null) {
                    output.write(SharedObject.HTTP_RESPONSE_NOT_FOUND.toString().getBytes(so.config.charset));
                    System.out.println("Invalid resource requsted: " + content.headers.toString());
                    output.close();
                    input.close();
                    client.close();
                    return;
                }
                try {
                    uri = URLDecoder.decode(uri, so.config.charset);
                } catch (final IllegalArgumentException ex) {
                    return;
                }
                final String[] uriParts = uri.split("\\?|\\&", 2);

                if(uriParts[0].equals("/")){
                    this.location = so.config.entryPoint.split("/");
                }else{
                    this.location = uriParts[0].split("/");
                }
                this.onRequest();
            }

        } catch (final IOException ex) {
            try {
                client.close();
            } catch (final IOException ex1) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public final void onRequest() throws UnsupportedEncodingException {
        if (content.headers != null && content.headers.get("Connection") != null) {
            matcher = UPGRADE_PATTERN.matcher(content.headers.get("Connection"));
            if (matcher.find()) {
                matcher = WEB_SOCKET_PATTERN.matcher(content.headers.get("Upgrade"));
                // Upgrade connection
                upgrade();
            } else {
                http();
                try {
                    this.client.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void http() throws UnsupportedEncodingException {
        try {
            client.setSoTimeout(so.config.timeout);
            HttpEvent.serve(this,so);
        } catch (SocketException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    private void upgrade(){
        if (matcher.find()) {
            try{
                WebSocketEvent.serve(this,so);
            }catch(Exception e){
                LOGGER.log(Level.SEVERE, null, e);
            }
        } else {
            matcher = HTTP2_PATTERN.matcher(content.headers.get("Upgrade"));
            // Http 2.x connection
            if (matcher.find()) {
                System.out.println("Http 2.0 connection detected. Not yet implemented.");
            }
        }
    }
}