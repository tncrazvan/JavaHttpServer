/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.razshare.elkserver.SmtpServer;

/**
 *
 * @author razvan
 */
public interface SmtpListener {
    public void onEmailReceived(Email email);
}
