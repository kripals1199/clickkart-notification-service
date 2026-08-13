// src/main/java/com/clickkart/notification/service/EmailSender.java
package com.clickkart.notification.service;

/**
 * Transport for outbound email. Two implementations exist and exactly one is active, chosen by
 * configuration at startup:
 *
 * <ul>
 *   <li>{@code SmtpEmailSenderImpl} - real delivery via JavaMailSender. Active when {@code
 *       spring.mail.host} is set.</li>
 *   <li>{@code LoggingEmailSenderImpl} - writes the message to the DISPATCH log instead of sending.
 *       Active when no SMTP host is configured, so the service still runs credential-free in
 *       dev.</li>
 * </ul>
 *
 * Implementations throw on failure rather than swallowing it - the caller records the failure as
 * a {@code FAILED} notification row and propagates, so a service that promised to email a reset
 * link never reports success when nothing was sent.
 */
public interface EmailSender {

    /**
     * @param to      recipient address
     * @param subject message subject
     * @param body    plain-text body (no HTML templating yet)
     * @throws org.springframework.mail.MailException when delivery fails
     */
    void send(String to, String subject, String body);

    /** True when this implementation actually delivers mail, false when it only logs. */
    boolean isRealDelivery();
}
