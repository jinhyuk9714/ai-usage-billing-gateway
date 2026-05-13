package io.github.sungjh.aiusagebillinggateway;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthTenantSecurityIT extends IntegrationTestSupport {

    @Test
    void duplicateEmailIsRejected() throws Exception {
        signup("owner@example.com");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"owner@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void jwtIsRequiredForOrganizationApis() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessAnotherOrganizationsData() throws Exception {
        String ownerToken = signup("owner@example.com");
        UUID organizationId = createOrganization(ownerToken, "Owner Org");
        String outsiderToken = signup("outsider@example.com");

        mockMvc.perform(get("/api/organizations/{orgId}", organizationId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCannotPerformAdminOnlyOperationButOwnerAndAdminCan() throws Exception {
        String ownerToken = signup("owner@example.com");
        UUID organizationId = createOrganization(ownerToken, "Billing Org");
        signup("member@example.com");
        signup("admin@example.com");

        mockMvc.perform(post("/api/organizations/{orgId}/members", organizationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","role":"MEMBER"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/organizations/{orgId}/members", organizationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated());

        String memberToken = login("member@example.com");
        mockMvc.perform(put("/api/organizations/{orgId}/subscription", organizationId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planCode":"PRO"}
                                """))
                .andExpect(status().isForbidden());

        String adminToken = login("admin@example.com");
        mockMvc.perform(put("/api/organizations/{orgId}/subscription", organizationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planCode":"PRO"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/organizations/{orgId}/subscription", organizationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planCode":"BUSINESS"}
                                """))
                .andExpect(status().isOk());
    }
}
