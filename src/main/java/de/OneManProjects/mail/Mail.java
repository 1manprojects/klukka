package de.OneManProjects.mail;

/*-
 * #%L
 * Klukka
 * %%
 * Copyright (C) 2025 Nikolai Reed reed@1manprojects.de
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import de.OneManProjects.utils.Util;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class Mail {

    private static void sendMail(final String messageBody, final String subject, final String receiver) throws IOException, MessagingException {
        final Optional<String> smptHost = Util.getEnvVar("SMTP_HOST", s->s, true);
        final Optional<String> smptPort = Util.getEnvVar("SMTP_PORT", s->s, true);
        final Optional<String> smptAuth = Util.getEnvVar("SMTP_AUTH", s->s, true);
        final Optional<String> smptssl = Util.getEnvVar("SMTP_SSL", s->s, true);
        final Optional<String> smptUser = Util.getEnvVar("SMTP_USER", s->s, true);
        final Optional<String> stmpPass = Util.getEnvVar("SMTP_PASSWORD", s->s, true);

        final Properties props = new Properties();
        props.put("mail.smtp.auth", smptAuth.get());
        props.put("mail.smtp.host", smptHost.get());
        props.put("mail.smtp.port", smptPort.get());
        props.put("mail.smtp.ssl.trust", smptHost.get());
        props.put("mail.smtp.ssl.enable", smptssl.get());

        final Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smptUser.get(), stmpPass.get());
            }
        });
        final Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("noreply@1manprojects.de"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(receiver));

        final MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(messageBody, "text/html; charset=utf-8");
        message.setSubject(subject);
        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    public static void sendInvite(final String newUserMail, final String userPassword) throws MessagingException, IOException {
        final Optional<String> url = Util.getEnvVar("APPLICATION_URL", s->s, false);
        final String inviteMessage = """
                <html>
                <head>
                  <title>Invite</title>
                </head>
                
                <body>
                  <h1>Welcome to Klukka</h1>
                  <p>A new account was created for you at <a href="{{domain}}">Klukka</a></p>
                  <p>You can login with the following credentials</p>
                  </br>
                  <p>username:  <b>{{user.mail}}</b>
                  <p>password:  <b>{{user.pass}}</b>
                  </br>
                  </br>
                  <p>Please change your password after first login</p>
                </body>
                
                </html>\s""";
        final String userInviteMessage = inviteMessage.replace("{{user.mail}}", newUserMail)
        .replace("{{user.pass}}", userPassword)
        .replace("{{domain}}", url.orElse("http//127.0.0.1"));
        sendMail(userInviteMessage, "Someone invited you to Klukka", newUserMail);
    }

    public static void sendPasswordReset(final String userMail, final String token) throws MessagingException, IOException {
        final Optional<String> url = Util.getEnvVar("APPLICATION_URL", s->s, false);
        final String resetMessage = """
                <html>
                <head>
                  <title>Password reset</title>
                </head>
                
                <body>
                  <h1>Reset you're Klukka password</h1>
                  <p>Someone or You has requested a password reset for your account {{user.mail}}</p>
                  <p>You can reset you're password with the following link</p>
                  </br>
                  <p><b><a href="{{domain}}/reset/{{user.resetLink}}">RESET PASSWORD</a></b>
                  </br>
                </body>
                
                </html>\s""";
        final String userResetMessage = resetMessage.replace("{{user.mail}}", userMail)
        .replace("{{user.resetLink}}", token)
        .replace("{{domain}}", url.orElse("http//127.0.0.1"));
        sendMail(userResetMessage,"Reset your password", userMail);
    }

    public static void sendGroupInvite(final String newUserMail, final String groupTitle) throws MessagingException, IOException {
        final Optional<String> url = Util.getEnvVar("APPLICATION_URL", s->s, false);
        final String inviteMessage = """
                <html>
                <head>
                  <title>You have been invited to a Group</title>
                </head>
                
                <body>
                  <h1>You have been added to the Group: {{group.title}}</h1>
                  <p>Projects of the Group are now available for you to track your time on</p>
                  <p>You can manage your group access online at <a href="{{domain}}">Klukka</a></p>
                  </br>
                  </br>
                </body>
                
                </html>\s""";
        final String userGroupInvite = inviteMessage.replace("{{group.title}}", groupTitle)
        .replace("{{domain}}", url.orElse("http//127.0.0.1"));
        sendMail(userGroupInvite,"Group Invite", newUserMail);
    }

}
