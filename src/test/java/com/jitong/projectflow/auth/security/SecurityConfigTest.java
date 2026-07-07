package com.jitong.projectflow.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void protectedEndpointWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/system/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
