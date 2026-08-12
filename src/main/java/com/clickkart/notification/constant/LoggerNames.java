// src/main/java/com/clickkart/notification/constant/LoggerNames.java
package com.clickkart.notification.constant;

/**
 * Named loggers routed to their own dedicated appender/file by {@code logback-spring.xml}
 * (ACCESS -> access.log, DISPATCH -> dispatch.log). Kept as constants so every
 * {@code @Slf4j(topic=...)} call site matches the logback configuration exactly.
 */
public final class LoggerNames {

    private LoggerNames() {}

    public static final String ACCESS = "ACCESS";
    public static final String DISPATCH = "DISPATCH";
}
