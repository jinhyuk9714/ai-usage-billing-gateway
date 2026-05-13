package io.github.sungjh.aiusagebillinggateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class ApplicationContextIT extends IntegrationTestSupport {

    @Test
    void contextLoadsAndFlywayMigrationsRun() {
    }

    @Test
    void healthEndpointIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
