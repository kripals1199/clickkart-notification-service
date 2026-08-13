// src/main/java/com/clickkart/notification/config/WebConfig.java
package com.clickkart.notification.config;

import com.clickkart.notification.constant.ApiPaths;
import com.clickkart.notification.filter.AccessLogFilter;
import com.clickkart.notification.filter.CorrelationIdFilter;
import com.clickkart.notification.filter.MdcCleanupFilter;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class WebConfig {

    private static final List<String> CORRELATION_ID_EXEMPT_PATHS = List.of(
            ApiPaths.ACTUATOR_HEALTH,
            ApiPaths.ACTUATOR_HEALTH_WILDCARD,
            ApiPaths.ACTUATOR_PROMETHEUS,
            ApiPaths.SWAGGER_UI,
            ApiPaths.SWAGGER_UI_WILDCARD,
            ApiPaths.API_DOCS_WILDCARD);

    @Bean
    public FilterRegistrationBean<MdcCleanupFilter> mdcCleanupFilter() {
        FilterRegistrationBean<MdcCleanupFilter> registration = new FilterRegistrationBean<>(new MdcCleanupFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AccessLogFilter> accessLogFilter() {
        FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>(new AccessLogFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(HandlerExceptionResolver handlerExceptionResolver) {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter(handlerExceptionResolver, CORRELATION_ID_EXEMPT_PATHS));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
