package com.bobal.banking;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BankingReadToolsTest {
    @Test void investigationsCanJoinReadOnlyRecordsAndTraceEveryTool() {
        var tools = new BankingReadTools(new OperationsController());
        assertThat(tools.lookupTransaction("tx-2002")).contains("Review required", "ACC-1001");
        assertThat(tools.lookupAccount("ACC-1001")).contains("Everyday Chequing");
        assertThat(tools.casesForTransaction("TX-2002")).contains("CASE-001");
        assertThat(tools.trace()).extracting(AgentController.Trace::tool)
            .containsExactly("transaction.lookup", "account.lookup", "case.by_transaction");
        assertThat(tools.trace()).allMatch(t -> t.permission().startsWith("read:"));
    }

    @Test void malformedAndUnknownIdsAreNotInvented() {
        var tools = new BankingReadTools(new OperationsController());
        assertThatThrownBy(() -> tools.lookupAccount("ACC-1001; DROP TABLE"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(tools.lookupTransaction("TX-9999")).isEqualTo("NOT_FOUND");
        assertThat(tools.trace().get(0).outcome()).isEqualTo("not_found");
    }

    @Test void capsToolUsePerRequestAndKeepsTracesIsolated() {
        var data = new OperationsController();
        var tools = new BankingReadTools(data);
        for (int i = 0; i < 8; i++) tools.listAccounts();
        assertThatThrownBy(tools::listAccounts).isInstanceOf(IllegalStateException.class);
        assertThat(tools.trace()).hasSize(8);
        assertThat(new BankingReadTools(data).trace()).isEmpty();
    }
}
