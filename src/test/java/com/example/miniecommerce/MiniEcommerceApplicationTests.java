package com.example.miniecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"gamehub.admin.email=", "gamehub.admin.password.hash="})
@ActiveProfiles("test")
class MiniEcommerceApplicationTests extends DatabaseTestSupport {

    @Test
    void contextLoads() {
    }

}
