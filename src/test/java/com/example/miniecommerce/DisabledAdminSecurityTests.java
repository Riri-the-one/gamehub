package com.example.miniecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"gamehub.admin.email=", "gamehub.admin.password.hash=",
    })
@ActiveProfiles("test")
class DisabledAdminSecurityTests extends DatabaseTestSupport {
    @Autowired WebApplicationContext context;
    @Test void noFallbackLoginExists() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(post("/admin/login").with(csrf()).param("email", "admin").param("password", "admin"))
            .andExpect(redirectedUrl("/admin/login?error"));
        mvc.perform(get("/admin")).andExpect(redirectedUrl("/admin/login"));
    }
}
