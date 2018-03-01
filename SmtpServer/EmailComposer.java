/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package elkserver.SmtpServer;

import elkserver.Elk;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author razvan
 */
public class EmailComposer extends SmtpMessageManager{
    private final Email email;
    public EmailComposer(Email email,SmtpServer server,String clientHostname,int port,boolean ssl) throws IOException {
        super(server, (ssl?SSLSocketFactory.getDefault().createSocket(InetAddress.getByName(clientHostname),port):new Socket(clientHostname,port)));
        this.email = email;
    }

    public EmailComposer(Email email,SmtpServer server,String clientHostname,int port) throws IOException {
        super(server, new Socket(clientHostname,port));
        this.email = email;
    }
    
    
    
    public boolean submit() throws IOException{
        String error, eodLine, eodSequence;
        if(!isReady(read())) return false;
        sayHelo();
        if(!isOk(read())) return false;
        sayMailFrom(email.getSender());
        if(!isOk(read())) return false;
        
        for(String recipient : email.getRecipients()){
            sayRecipient(recipient);
            
            if(!isOk((error = read()))){
                System.err.println("[WARNING] Server replied with: "+error);
            }
        }
        sayData();
        if(!isEndDataWith((eodLine = read()))) return false;
        eodSequence = getEndDataWithValue(eodLine);
        
        sayDataFrom(email.getSender());
        sayDataDate(Elk.calendar.getTime().getTime());
        sayDataSubject(email.getSubject());
        sayDataTo((String[]) email.getRecipients().toArray());
        setMultipartBoundaryId(Elk.generateMultipartBoundary());
        sayDataContentType(); // Content-Type:multipart/alternative
        sayNothing();
        sayDataFrames(email.getFrames());
        say(".");
        if(!isOk(read())) return false;
        sayQuitAndClose();
        return true;
    }
}