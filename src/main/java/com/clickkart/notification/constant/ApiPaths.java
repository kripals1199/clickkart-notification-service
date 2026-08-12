// src/main/java/com/clickkart/notification/constant/ApiPaths.java
package com.clickkart.notification.constant;

/** Single source of truth for this service's route strings - used in controller mappings. */
public final class ApiPaths {

    private ApiPaths() {}

    public static final String BASE = "/api/v1/notifications";

    public static final String PASSWORD_RESET = BASE + "/password-reset";
    public static final String OTP = BASE + "/otp";

    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_HEALTH_WILDCARD = "/actuator/health/**";
    public static final String ACTUATOR_PROMETHEUS = "/actuator/prometheus";
    public static final String SWAGGER_UI = "/swagger-ui.html";
    public static final String SWAGGER_UI_WILDCARD = "/swagger-ui/**";
    public static final String API_DOCS_WILDCARD = "/v3/api-docs/**";
}
