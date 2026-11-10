package com.bobal.banking;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Conservative guard before any paid model request. Only read-only tools exist
 * even if a write request bypasses this text filter.
 */
public final class WritePolicy {
    private static final Pattern WRITE_VERB = Pattern.compile(
        "\\b(CREATE|DELETE|UPDATE|TRANSFER|SEND|REFUND|FREEZE|UNFREEZE|BLOCK|CLOSE|APPROVE|REJECT|ESCALATE|MODIFY|CHANGE|WITHDRAW|DEPOSIT|WIRE|PAY|PAYOUT|REVERSE|CANCEL|VOID|SETTLE|DISBURSE)\\b");
    private static final Pattern WRITE_PHRASE = Pattern.compile(
        "\\b(MOVE|SHIFT|ROUTE)\\s+(?:\\S+\\s+){0,3}(?:MONEY|FUNDS|CASH)\\b"
        + "|\\bMAKE\\s+(?:A\\s+)?(?:PAYMENT|TRANSFER|WITHDRAWAL|DEPOSIT)\\b"
        + "|\\bISSUE\\s+(?:A\\s+)?(?:REFUND|PAYMENT|PAYOUT)\\b"
        + "|\\bMARK\\s+(?:\\S+\\s+){0,3}AS\\s+(?:RESOLVED|CLOSED|APPROVED|PAID)\\b");

    private WritePolicy() {}

    public static boolean isWriteRequest(String input) {
        if (input == null) return false;
        String normalized = input.toUpperCase(Locale.ROOT);
        return WRITE_VERB.matcher(normalized).find() || WRITE_PHRASE.matcher(normalized).find();
    }
}
