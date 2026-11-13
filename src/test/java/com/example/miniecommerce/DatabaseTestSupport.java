package com.example.miniecommerce;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Opt-in MySQL execution is restricted to the disposable local audit database. */
abstract class DatabaseTestSupport {
    @DynamicPropertySource
    static void temporaryMysql(DynamicPropertyRegistry registry) {
        String url = System.getenv("GAMEHUB_TEST_MYSQL_URL");
        if (url == null) throw new IllegalStateException("Run tools/Run-IsolatedMysqlTests.ps1: a disposable MySQL URL is required.");
        if (!url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/gamehub_cart_tests\\?.*")) {
            throw new IllegalStateException("Only the explicit temporary localhost cart-test database is accepted.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.username", () -> "gamehub_audit");
        registry.add("spring.datasource.password", () -> System.getenv("GAMEHUB_TEST_MYSQL_PASSWORD"));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
