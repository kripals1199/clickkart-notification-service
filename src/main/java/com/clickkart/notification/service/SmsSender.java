// src/main/java/com/clickkart/notification/service/SmsSender.java
package com.clickkart.notification.service;

/**
 * Transport for outbound SMS. Same active-one-of-two arrangement as {@link EmailSender}:
 *
 * <ul>
 *   <li>{@code Msg91SmsSenderImpl} - real delivery via MSG91's v5 flow API. Active when {@code
 *       clickkart.notification.sms.msg91.auth-key} is set.</li>
 *   <li>{@code LoggingSmsSenderImpl} - writes the message to the DISPATCH log instead of sending.
 *       Active otherwise.</li>
 * </ul>
 *
 * MSG91 was chosen over Twilio because this platform validates Indian mobile numbers (10 digits
 * starting 6-9) and Indian transactional SMS requires a DLT-registered template, which Indian
 * providers handle natively.
 */
public interface SmsSender {

    /**
     * @param toMobileNumber 10-digit Indian mobile number, without country code
     * @param message        message body
     * @throws RuntimeException when delivery fails
     */
    void send(String toMobileNumber, String message);

    /** True when this implementation actually delivers SMS, false when it only logs. */
    boolean isRealDelivery();
}
