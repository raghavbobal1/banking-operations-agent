package com.bobal.banking;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class OperationsController {
 private final Map<String,Account> accounts=Map.of(
  "ACC-1001",new Account("ACC-1001","Everyday Chequing",new BigDecimal("4820.50"),"CAD"),
  "ACC-1002",new Account("ACC-1002","High Interest Savings",new BigDecimal("12840.75"),"CAD"),
  "ACC-1003",new Account("ACC-1003","Business Operating",new BigDecimal("38750.00"),"CAD"));
 private final Map<String,Transaction> transactions=Map.of(
  "TX-2001",new Transaction("TX-2001","ACC-1001",new BigDecimal("-48.25"),"Transit Pass","Settled"),
  "TX-2002",new Transaction("TX-2002","ACC-1001",new BigDecimal("-1250.00"),"Online Transfer","Review required"),
  "TX-2003",new Transaction("TX-2003","ACC-1002",new BigDecimal("250.00"),"Interest Credit","Settled"),
  "TX-2004",new Transaction("TX-2004","ACC-1003",new BigDecimal("-380.00"),"Office Supplies","Settled"),
  "TX-2005",new Transaction("TX-2005","ACC-1003",new BigDecimal("1920.00"),"Invoice Payment","Pending"));
 private final Map<String,SupportCase> cases=Map.of("TX-2002",new SupportCase("CASE-001","TX-2002","OPEN"));
 @GetMapping("/health") public Map<String,String> health(){return Map.of("status","UP","mode","synthetic-demo");}
 @GetMapping("/accounts") public Collection<Account> accounts(){return accounts.values();}
 @GetMapping("/accounts/{id}") public Account account(@PathVariable String id){Account a=accounts.get(id);if(a==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found");return a;}
 @GetMapping("/transactions") public Collection<Transaction> transactions(){return transactions.values();}
 @GetMapping("/transactions/{id}") public Transaction transaction(@PathVariable String id){Transaction t=transactions.get(id);if(t==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Transaction not found");return t;}
 @GetMapping("/cases") public Collection<SupportCase> cases(){return cases.values();}
 public record Account(String id,String type,BigDecimal balance,String currency){}
 public record Transaction(String id,String accountId,BigDecimal amount,String merchant,String status){}
 public record SupportCase(String id,String transactionId,String status){}
}