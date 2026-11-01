package com.bobal.banking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Deterministic structured tool runner over fabricated data.
 * No LLM, bank connection or write-capable tools are present.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private static final Pattern ACCOUNT = Pattern.compile("\\bACC-\\d{4}\\b");
    private static final Pattern TRANSACTION = Pattern.compile("\\bTX-\\d{4}\\b");
    private static final Pattern CASE = Pattern.compile("\\bCASE-\\d{3}\\b");
    private static final Pattern WRITE = Pattern.compile(
        "\\b(CREATE|DELETE|UPDATE|TRANSFER|SEND|REFUND|FREEZE|UNFREEZE|BLOCK|CLOSE|APPROVE|REJECT|ESCALATE|MODIFY|CHANGE)\\b");
    private static final List<String> PERMISSIONS =
        List.of("read:accounts", "read:transactions", "read:cases");
    private final OperationsController data;
    private final Map<String, Window> limits = new ConcurrentHashMap<>();
    private record Window(long minute, int count) {}
    public record ChatRequest(@NotBlank @Size(max = 240) String message) {}
    public record Trace(String tool, String input, String outcome, String permission) {}
    public record ChatResponse(String requestId, String mode, String answer,
                               List<Trace> trace, List<String> permissions, boolean syntheticData) {}
    public record Tool(String name, String permission, String description) {}

    public AgentController(OperationsController data) { this.data = data; }

    @GetMapping("/tools")
    public List<Tool> tools() {
        return List.of(
            new Tool("account.lookup", "read:accounts", "Find a synthetic account"),
            new Tool("account.list", "read:accounts", "List synthetic accounts"),
            new Tool("transaction.lookup", "read:transactions", "Find a synthetic transaction"),
            new Tool("transaction.by_account", "read:transactions", "Account activity"),
            new Tool("case.lookup", "read:cases", "Find a synthetic case"),
            new Tool("case.by_transaction", "read:cases", "Related support cases"),
            new Tool("case.list", "read:cases", "List synthetic cases"));
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpServletRequest http) {
        rateLimit(http.getRemoteAddr());
        String input = request.message().trim().toUpperCase(Locale.ROOT);
        List<Trace> trace = new ArrayList<>();
        String answer;
        if (WRITE.matcher(input).find()) {
            answer = "Denied. This agent has read-only permissions. It cannot create cases, move money or change records.";
        } else {
            String accountId = match(ACCOUNT, input);
            String txId = match(TRANSACTION, input);
            String caseId = match(CASE, input);
            if (txId != null) {
                OperationsController.Transaction tx = tool(trace, "transaction.lookup", txId,
                    "read:transactions", () -> data.transactions().stream()
                        .filter(t -> t.id().equals(txId)).findFirst().orElse(null));
                if (tx == null) {
                    answer = "No synthetic transaction exists with ID " + txId + ".";
                } else if (contains(input, "INVESTIGATE", "WHY", "REVIEW", "FLAGGED", "DETAIL", "EXPLAIN")) {
                    OperationsController.Account account = tool(trace, "account.lookup", tx.accountId(),
                        "read:accounts", () -> data.accounts().stream()
                            .filter(a -> a.id().equals(tx.accountId())).findFirst().orElse(null));
                    List<OperationsController.SupportCase> cases = tool(trace, "case.by_transaction", txId,
                        "read:cases", () -> data.cases().stream()
                            .filter(c -> c.transactionId().equals(txId)).toList());
                    answer = "Investigation of " + tx.id() + "\n" + describe(tx)
                        + "\nAccount: " + (account == null ? "Not found" : account.id() + " (" + account.type() + ")")
                        + "\nRelated cases: " + (cases.isEmpty() ? "None" :
                            String.join(", ", cases.stream().map(OperationsController.SupportCase::id).toList()))
                        + "\nAssessment: " + (tx.status().equals("Review required")
                            ? "The synthetic record is marked Review required. The dataset has no underlying fraud score or rule evidence, so the reason cannot be established."
                            : "The synthetic status is " + tx.status() + ". No risk scoring is available.")
                        + "\nNo banking action was taken.";
                } else {
                    answer = describe(tx);
                }
            } else if (accountId != null) {
                OperationsController.Account account = tool(trace, "account.lookup", accountId,
                    "read:accounts", () -> data.accounts().stream()
                        .filter(a -> a.id().equals(accountId)).findFirst().orElse(null));
                if (account == null) {
                    answer = "No synthetic account exists with ID " + accountId + ".";
                } else if (contains(input, "TRANSACTION", "ACTIVITY", "PAYMENT")) {
                    List<OperationsController.Transaction> transactions = tool(trace, "transaction.by_account", accountId,
                        "read:transactions", () -> data.transactions().stream()
                            .filter(t -> t.accountId().equals(accountId))
                            .sorted(Comparator.comparing(OperationsController.Transaction::id)).toList());
                    answer = account.id() + " (" + account.type() + ") has " + transactions.size()
                        + " synthetic transactions:\n"
                        + String.join("\n", transactions.stream().map(this::describe).toList());
                } else {
                    answer = account.id() + " | " + account.type() + " | Sample balance "
                        + account.balance().toPlainString() + " " + account.currency();
                }
            } else if (caseId != null) {
                OperationsController.SupportCase found = tool(trace, "case.lookup", caseId,
                    "read:cases", () -> data.cases().stream()
                        .filter(c -> c.id().equals(caseId)).findFirst().orElse(null));
                answer = found == null ? "No synthetic case exists with ID " + caseId + "."
                    : found.id() + " | " + found.status() + " | Related transaction: " + found.transactionId();
            } else if (contains(input, "CASE", "TICKET")) {
                List<OperationsController.SupportCase> cases = tool(trace, "case.list", "all",
                    "read:cases", () -> data.cases().stream()
                        .sorted(Comparator.comparing(OperationsController.SupportCase::id)).toList());
                answer = "Synthetic support cases:\n" +
                    String.join("\n", cases.stream().map(c ->
                        c.id() + " | " + c.status() + " | " + c.transactionId()).toList());
            } else if (contains(input, "ACCOUNT", "BALANCE")) {
                List<OperationsController.Account> accounts = tool(trace, "account.list", "all",
                    "read:accounts", () -> data.accounts().stream()
                        .sorted(Comparator.comparing(OperationsController.Account::id)).toList());
                answer = "Synthetic accounts:\n" +
                    String.join("\n", accounts.stream().map(a -> a.id() + " | " + a.type()).toList());
            } else {
                answer = "Try: Investigate TX-2002, Show transactions for ACC-1001, Show account ACC-1002 or List support cases. This is a deterministic structured-tool demo, not an LLM.";
            }
        }
        return new ChatResponse(UUID.randomUUID().toString(), "deterministic-structured-tools",
            answer, List.copyOf(trace), PERMISSIONS, true);
    }

    private String describe(OperationsController.Transaction tx) {
        return tx.id() + " | " + tx.merchant() + " | " + tx.amount().toPlainString()
            + " CAD | " + tx.status() + " | " + tx.accountId();
    }
    private static String match(Pattern p, String input) {
        Matcher m = p.matcher(input);
        return m.find() ? m.group() : null;
    }
    private static boolean contains(String input, String... words) {
        for (String word : words) if (input.contains(word)) return true;
        return false;
    }
    private static <T> T tool(List<Trace> trace, String name, String input,
                              String permission, Supplier<T> action) {
        if (!PERMISSIONS.contains(permission))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tool permission denied");
        T result = action.get();
        String outcome = result == null ? "not_found"
            : result instanceof List<?> list ? "ok (" + list.size() + " records)" : "ok";
        trace.add(new Trace(name, input, outcome, permission));
        return result;
    }
    private void rateLimit(String ip) {
        long minute = System.currentTimeMillis() / 60000;
        Window window = limits.compute(ip == null ? "unknown" : ip, (key, old) ->
            old == null || old.minute() != minute
                ? new Window(minute, 1) : new Window(minute, old.count() + 1));
        if (window.count() > 40)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demo limit: 40 requests per minute");
        if (limits.size() > 5000)
            limits.entrySet().removeIf(e -> e.getValue().minute() < minute);
    }
}
