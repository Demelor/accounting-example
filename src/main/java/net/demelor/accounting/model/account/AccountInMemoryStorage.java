package net.demelor.accounting.model.account;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Account data access with in-memory storage.
 */
public class AccountInMemoryStorage implements AccountRepository {
    public static AtomicLong sequence = new AtomicLong();

    public final Map<Long, Account> storage = new HashMap<>();

    @Override
    public Account create(String name, BigDecimal funds) {
        long id = sequence.incrementAndGet();
        Account account = new Account(id, funds, name);
        storage.put(id, account);
        return account;
    }

    @Override
    public Optional<Account> find(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void persist(Account account) {
        // Do nothing, we're in-memory after all
    }
}
