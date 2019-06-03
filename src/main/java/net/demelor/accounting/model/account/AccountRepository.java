package net.demelor.accounting.model.account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account create(String name, BigDecimal funds);

    Optional<Account> find(long id);

    List<Account> findAll();

    void persist(Account account);
}
