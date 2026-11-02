package com.bobal.banking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Only these seven read operations are exposed to the model.
 * Each request receives a new instance so audit traces and tool budgets cannot cross sessions.
 */
public final class BankingReadTools {
    private static final int MAX_CALLS = 8;
    private static final Pattern ACCOUNT_ID = Pattern.compile("ACC-[0-9]{4}");
    private static final Pattern TRANSACTION_ID = Pattern.compile("TX-[0-9]{4}");
    private static final Pattern CASE_ID = Pattern.compile("CASE-[0-9]{3}");
    private final OperationsController data;
    private final List<AgentController.Trace> trace = new ArrayList<>();

    public BankingReadTools(OperationsController data) { this.data = data; }

    public synchronized List<AgentController.Trace> trace() { return List.copyOf(trace); }

    private synchronized String record(String tool, String id, String permission, String result, boolean found) {
        if (trace.size() >= MAX_CALLS) throw new IllegalStateException("Tool-call budget exceeded");
        trace.add(new AgentController.Trace(tool, id, found ? "ok" : "not_found", permission));
        return result;
    }

    private static String validated(String id, Pattern expected) {
        String normalized = id == null ? "" : id.trim().toUpperCase(Locale.ROOT);
        if (!expected.matcher(normalized).matches()) throw new IllegalArgumentException("Invalid synthetic record ID");
        return normalized;
    }

    @Tool(description = "Retrieve one synthetic banking account by ID. Read only. Valid account IDs look like ACC-1001.")
    public String lookupAccount(@ToolParam(description = "Synthetic account ID, for example ACC-1001") String id) {
        String key = validated(id, ACCOUNT_ID);
        return data.accounts().stream().filter(a -> a.id().equals(key)).findFirst()
            .map(a -> record("account.lookup", key, "read:accounts",
                a.id() + " | " + a.type() + " | " + a.balance() + " " + a.currency(), true))
            .orElseGet(() -> record("account.lookup", key, "read:accounts", "NOT_FOUND", false));
    }

    @Tool(description = "Retrieve one synthetic transaction and its recorded status. Read only. Valid IDs look like TX-2002.")
    public String lookupTransaction(@ToolParam(description = "Synthetic transaction ID, for example TX-2002") String id) {
        String key = validated(id, TRANSACTION_ID);
        return data.transactions().stream().filter(t -> t.id().equals(key)).findFirst()
            .map(t -> record("transaction.lookup", key, "read:transactions",
                t.id() + " | account " + t.accountId() + " | " + t.merchant()
                    + " | amount " + t.amount() + " CAD | status " + t.status(), true))
            .orElseGet(() -> record("transaction.lookup", key, "read:transactions", "NOT_FOUND", false));
    }

    @Tool(description = "List synthetic transactions for an account, including status and merchant. Read only.")
    public String transactionsForAccount(@ToolParam(description = "Synthetic account ID, for example ACC-1001") String id) {
        String key = validated(id, ACCOUNT_ID);
        var transactions = data.transactions().stream().filter(t -> t.accountId().equals(key))
            .sorted(Comparator.comparing(OperationsController.Transaction::id)).toList();
        String result = transactions.isEmpty() ? "NONE" : String.join("\n", transactions.stream()
            .map(t -> t.id() + " | " + t.merchant() + " | " + t.amount() + " CAD | " + t.status()).toList());
        return record("transaction.by_account", key, "read:transactions", result, !transactions.isEmpty());
    }

    @Tool(description = "Find synthetic support cases for a transaction. Read only.")
    public String casesForTransaction(@ToolParam(description = "Synthetic transaction ID, for example TX-2002") String id) {
        String key = validated(id, TRANSACTION_ID);
        var cases = data.cases().stream().filter(c -> c.transactionId().equals(key)).toList();
        String result = cases.isEmpty() ? "NONE" : String.join("\n", cases.stream()
            .map(c -> c.id() + " | " + c.status() + " | " + c.transactionId()).toList());
        return record("case.by_transaction", key, "read:cases", result, !cases.isEmpty());
    }

    @Tool(description = "Retrieve a synthetic support case by its case ID. Read only.")
    public String lookupCase(@ToolParam(description = "Synthetic case ID, for example CASE-001") String id) {
        String key = validated(id, CASE_ID);
        return data.cases().stream().filter(c -> c.id().equals(key)).findFirst()
            .map(c -> record("case.lookup", key, "read:cases",
                c.id() + " | " + c.status() + " | transaction " + c.transactionId(), true))
            .orElseGet(() -> record("case.lookup", key, "read:cases", "NOT_FOUND", false));
    }

    @Tool(description = "List all synthetic support cases. Read only.")
    public String listCases() {
        var cases = data.cases().stream().sorted(Comparator.comparing(OperationsController.SupportCase::id)).toList();
        return record("case.list", "all", "read:cases",
            cases.isEmpty() ? "NONE" : String.join("\n", cases.stream()
                .map(c -> c.id() + " | " + c.status() + " | " + c.transactionId()).toList()), !cases.isEmpty());
    }

    @Tool(description = "List synthetic bank accounts without changing them. Read only.")
    public String listAccounts() {
        var accounts = data.accounts().stream().sorted(Comparator.comparing(OperationsController.Account::id)).toList();
        return record("account.list", "all", "read:accounts",
            accounts.isEmpty() ? "NONE" : String.join("\n", accounts.stream()
                .map(a -> a.id() + " | " + a.type()).toList()), !accounts.isEmpty());
    }
}
