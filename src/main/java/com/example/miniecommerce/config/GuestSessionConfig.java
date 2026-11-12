package com.example.miniecommerce.config;

import java.util.Set;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.Session;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.HttpSessionMutexListener;

@Configuration
public class GuestSessionConfig {
    @Bean
    ServletListenerRegistrationBean<HttpSessionMutexListener> guestSessionMutex() {
        return new ServletListenerRegistrationBean<>(new HttpSessionMutexListener());
    }

    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> guestSessionCookies() {
        return factory -> {
            factory.getSettings().getSession().getCookie().setHttpOnly(true);
            factory.getSettings().getSession().getCookie().setSameSite(Cookie.SameSite.LAX);
            factory.getSettings().getSession().setTrackingModes(Set.of(
                    Session.SessionTrackingMode.COOKIE));
        };
    }
}
