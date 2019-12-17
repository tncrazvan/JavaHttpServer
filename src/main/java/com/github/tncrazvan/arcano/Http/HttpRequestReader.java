package com.github.tncrazvan.arcano.Http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import com.github.tncrazvan.arcano.SharedObject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.ArrayList;
import javax.net.ssl.SSLSocket;

/**
 *
 * @author Razvan
 */
public abstract class HttpRequestReader extends SharedObject implements Runnable{
    public final Socket client;
    public SSLSocket secureClient=null;
    public final BufferedReader bufferedReader;
    public final BufferedWriter bufferedWriter;
    public final DataOutputStream output;
    public final DataInputStream input;
    public final StringBuilder outputString = new StringBuilder();
    public HttpRequest request = null;
    public final StringBuilder location = new StringBuilder();
    public final SharedObject so;
    public HttpRequestReader(SharedObject so, Socket client) throws NoSuchAlgorithmException, IOException {
        this.so = so;
        this.client=client;
        bufferedReader = new BufferedReader(
                new InputStreamReader(
                        client
                                .getInputStream()));
        bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(
                        client
                                .getOutputStream()));
        output = new DataOutputStream(client.getOutputStream());
        input = new DataInputStream(client.getInputStream());
        
    }
    
    @Override
    public void run(){
        try {
            byte[] chain = new byte[]{0,0,0,0};
            boolean keepReading = true, EOFException = false;
            while (keepReading) {
                try{
                    chain[3] = chain[2];
                    chain[2] = chain[1];
                    chain[1] = chain[0];
                    chain[0] = input.readByte();
                    outputString.append((char)chain[0]);
                    if((char)chain[3] == '\r' && (char)chain[2] == '\n' && (char)chain[1] == '\r' && (char)chain[0] == '\n'){
                        keepReading = false;
                    }
                }catch(EOFException ex){
                    keepReading = false;
                    EOFException = true;
                    //ex.printStackTrace();
                }
            }
            if(outputString.length() == 0){
                client.close();
            }else{
                HttpHeaders clientHeader = HttpHeaders.fromString(outputString.toString().trim());
                //outputString = new StringBuilder();
                ArrayList<byte[]> inputList = new ArrayList<>();
                int length = 0;
                if(!EOFException){
                    int chunkSize = 0;
                    if(clientHeader.isDefined("Content-Length")){
                        chunkSize = Integer.parseInt(clientHeader.get("Content-Length"));
                    }

                    if(chunkSize > 0){
                        chain = new byte[chunkSize];
                        input.readFully(chain);
                        inputList.add(chain);
                        length += chain.length;
                        //outputString.append(new String(chain,charset));
                    }else{
                        int offset = 0;
                        chain = new byte[httpMtu];
                        try{
                            if(input.available() > 0)
                            while(input.read(chain)>0){
                                if(offset < httpMtu){
                                    offset++;
                                }else{
                                    //outputString.append(new String(chain,charset));
                                    inputList.add(chain);
                                    length += chain.length;
                                    offset = 0;
                                    chain = new byte[httpMtu];
                                }
                            }
                        }catch(SocketTimeoutException | EOFException e){
                            //outputString.append(new String(chain,charset));
                            length += chain.length;
                            inputList.add(chain);
                        }
                    }
                }
                byte[] inputBytes = new byte[length];
                int pos = 0;
                for(byte[] bytes : inputList){
                    for(int i=0;i<bytes.length;i++,pos++){
                        inputBytes[pos] = bytes[i];
                    }
                }
                this.request = new HttpRequest(clientHeader,inputBytes);
                String uri = request.headers.get("@Resource");
                try{
                    uri = URLDecoder.decode(uri,so.charset);
                }catch(IllegalArgumentException ex){
                    return;
                }
                String[] uriParts = uri.split("\\?|\\&",2);
                location.append(uriParts[0].replaceAll("^\\/", ""));
                this.onRequest();
            }
            
        } catch (IOException ex) {
            try {
                client.close();
            } catch (IOException ex1) {
                LOGGER.log(Level.SEVERE,null,ex);
            }
        }
    }
    
    
    public abstract void onRequest();
    
}