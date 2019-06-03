package net.demelor.accounting.model.account;

import net.demelor.accounting.exception.AccountingException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountService<V> {
    List<V> findAllAccounts();

    Optional<V> findAccount(long id);

    V createAccount(String name, BigDecimal funds) throws AccountingException;

    V transferFunds(long sourceId, long targetId, BigDecimal amount) throws AccountingException;
}
