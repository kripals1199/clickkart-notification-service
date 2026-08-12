// src/main/java/com/clickkart/notification/web/RequestMetadata.java
package com.clickkart.notification.web;

/** HTTP-request-derived context bundled into one record instead of a growing parameter list. */
public record RequestMetadata(String ipAddress, String userAgent) {}
