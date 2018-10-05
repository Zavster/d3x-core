/*
 * Copyright (C) 2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.mail;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import com.d3x.core.util.IO;

/**
 * A mailer client used to send email messages using the javax.mail library.
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class Mailer implements Closeable {

    private boolean debug;
    private Session session;
    private Transport transport;
    private MailSettings settings;

    /**
     * Constructor
     * @param settings  the mail settings
     */
    public Mailer(MailSettings settings) {
        this(settings, false);
    }

    /**
     * Constructor
     * @param settings  the mail settings
     * @param debug     true to run in debug mode
     */
    public Mailer(MailSettings settings, boolean debug) {
        Objects.requireNonNull(settings, "The mails settings cannot be null");
        this.settings = settings;
        this.debug = debug;
    }

    /**
     * Returns the settings for this mail component
     * @return  the settings
     */
    public MailSettings getSettings() {
        return settings;
    }

    /**
     * Returns true if this mailer is enabled
     * @return      true if mailer is enabled
     */
    public boolean isEnabled() {
        return settings.isEnabled();
    }


    /**
     * Sends the kronos message via mail server
     * @param message       the message to end
     * @throws MailException  if this operation fails
     */
    public void send(MailMessage message) throws MailException {
        Objects.requireNonNull(message, "The mail message cannot be null");
        try {
            final Session session = getSession();
            final Address[] recipients = getRecipients(message);
            final Message mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(settings.getFromAddress(), settings.getFromName()));
            mimeMessage.setRecipients(Message.RecipientType.TO, recipients);
            mimeMessage.setSubject(message.getSubject());
            mimeMessage.setContent(message.getBody(), message.getContentType());
            final Transport transport = getTransport();
            log.info("Sending mail message to " + message.getRecipients());
            final long t1 = System.currentTimeMillis();
            transport.sendMessage(mimeMessage, recipients);
            final long t2 = System.currentTimeMillis();
            log.info("Sent mail message in " + (t2-t1) + " millis");
        } catch (Exception ex) {
            throw new MailException("Failed to send mail message: " + ex.getMessage(), ex);
        }
    }


    /**
     * Returns the recipient addresses for the Kronos message
     * @param message   the message reference
     * @return          the array of addresses
     */
    private Address[] getRecipients(MailMessage message) {
        try {
            final List<String> recipients = message.getRecipients();
            if (recipients.size() == 0) {
                throw new MailException("No mail recipients specified in message");
            } else {
                final Address[] addresses = new Address[recipients.size()];
                for (int i=0; i<recipients.size(); ++i) {
                    final String recipient = recipients.get(i);
                    addresses[i] = new InternetAddress(recipient, recipient, "UTF-8");
                }
                return addresses;
            }
        } catch (MailException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new MailException("Malformed recipients for Kronos mail message", t);
        }
    }


    /**
     * Returns the mail transport, initializing it if necessary
     * @return      the mail transport
     * @throws MessagingException   if fails to initialize
     */
    private synchronized Transport getTransport() throws MessagingException {
        if (transport == null) {
            this.transport = getSession().getTransport();
        }
        if (!transport.isConnected()) {
            final long t1 = System.currentTimeMillis();
            log.info("Connecting to SMTP mail server at " + settings.getHost() + " as " + settings.getUsername());
            final String host = settings.getHost();
            final int port = settings.getPort();
            final String username = settings.getUsername();
            final String password = new String(settings.getPassword().getValue());
            this.transport.connect(host, port, username, password);
            final long t2 = System.currentTimeMillis();
            log.info("Connected to SMTP server " + host + " in " + (t2-t1) + " millis");
        }
        return transport;
    }


    /**
     * Returns a reference to the mail server session, initializing if necessary
     * @return  the mail server session object
     */
    private synchronized Session getSession() {
        if (session == null) {
            final Properties properties = new Properties();
            properties.put("mail.debug", debug ? "true" : "false");
            properties.put("mail.transport.protocol", "smtp");
            properties.put("mail.smtp.port", String.valueOf(settings.getPort()));
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.auth", "true");
            this.session = Session.getDefaultInstance(properties);
        }
        return session;
    }


    @Override
    public void close() {
        if (transport != null) {
            IO.close(transport);
            this.transport = null;
        }
    }


}
