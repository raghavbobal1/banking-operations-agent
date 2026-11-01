package com.bobal.banking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {
    @Autowired MockMvc mvc;

    private org.springframework.test.web.servlet.ResultActions ask(String message) throws Exception {
        return mvc.perform(post("/api/agent/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"" + message + "\"}"));
    }

    @Test void investigationCallsThreeReadOnlyTools() throws Exception {
        ask("Investigate TX-2002").andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("deterministic-structured-tools"))
            .andExpect(jsonPath("$.syntheticData").value(true))
            .andExpect(jsonPath("$.trace.length()").value(3))
            .andExpect(jsonPath("$.trace[0].tool").value("transaction.lookup"))
            .andExpect(jsonPath("$.trace[1].tool").value("account.lookup"))
            .andExpect(jsonPath("$.trace[2].tool").value("case.by_transaction"))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("no underlying fraud score")));
    }

    @Test void accountActivityUsesTwoTools() throws Exception {
        ask("Show transactions for ACC-1001").andExpect(status().isOk())
            .andExpect(jsonPath("$.trace.length()").value(2))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("TX-2002")));
    }

    @Test void writeAttemptIsDeniedWithoutToolCalls() throws Exception {
        ask("Transfer funds from ACC-1001").andExpect(status().isOk())
            .andExpect(jsonPath("$.trace.length()").value(0))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.startsWith("Denied.")));
    }

    @Test void missingIdsAreNotFabricated() throws Exception {
        ask("Investigate TX-9999").andExpect(status().isOk())
            .andExpect(jsonPath("$.trace[0].outcome").value("not_found"))
            .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("No synthetic transaction")));
    }

    @Test void invalidRequestsAreRejected() throws Exception {
        ask(" ").andExpect(status().isBadRequest());
        ask("x".repeat(241)).andExpect(status().isBadRequest());
    }

    @Test void toolRegistryListsOnlyReadPermissions() throws Exception {
        mvc.perform(get("/api/agent/tools")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].permission").value("read:accounts"));
    }
}
