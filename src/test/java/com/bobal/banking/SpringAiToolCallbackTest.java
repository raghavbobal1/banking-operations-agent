package com.bobal.banking;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import static org.assertj.core.api.Assertions.*;

class SpringAiToolCallbackTest {
    @Test void springAiRegistersOnlyReadOnlyToolsAndExecutesViaJson() {
        var tools = new BankingReadTools(new OperationsController());
        ToolCallback[] callbacks = ToolCallbacks.from(tools);
        assertThat(callbacks).hasSize(7);
        assertThat(Arrays.stream(callbacks).map(t -> t.getToolDefinition().name()).toList())
            .containsExactlyInAnyOrder("lookupAccount", "lookupTransaction", "transactionsForAccount",
                "casesForTransaction", "lookupCase", "listCases", "listAccounts");

        var lookup = Arrays.stream(callbacks)
            .filter(t -> t.getToolDefinition().name().equals("lookupTransaction"))
            .findFirst().orElseThrow();
        assertThat(lookup.call("{\"id\":\"TX-2002\"}")).contains("Review required");
        assertThat(tools.trace()).extracting(AgentController.Trace::tool)
            .containsExactly("transaction.lookup");
        assertThat(tools.trace().get(0).permission()).isEqualTo("read:transactions");
    }
}
