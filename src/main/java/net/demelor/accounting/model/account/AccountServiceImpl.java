package net.demelor.accounting.model.account;

import net.demelor.accounting.exception.AccountingException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountServiceImpl<V> implements AccountService<V> {

    private final AccountRepository repository;
    private final Account.ViewTransform<V> viewTransform;

    public AccountServiceImpl(AccountRepository repository, Account.ViewTransform<V> viewTransform) {
        if (repository == null || viewTransform == null) {
            throw new NullPointerException("Injected dependency is null");
        }
        this.repository = repository;
        this.viewTransform = viewTransform;
    }

    @Override
    public List<V> findAllAccounts() {
        return repository.findAll().stream()
                .map(account -> {
                    account.lockRead();
                    try {
                        return account.toView(viewTransform);
                    } finally {
                        account.unlockRead();
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<V> findAccount(long id) {
        return repository.find(id)
                .map(account -> {
                    account.lockRead();
                    try {
                        return account.toView(viewTransform);
                    } finally {
                        account.unlockRead();
                    }
                });
    }

    @Override
    public V createAccount(String name, BigDecimal funds) throws AccountingException {
        if (name == null || name.trim().isEmpty()) {
            throw new AccountingException("New account name must be non-empty");
        } else if (funds == null || funds.signum() < 0) {
            throw new AccountingException("New account initial funds must be a non-negative value");
        }

        Account account = repository.create(name, funds);
        return account.toView(viewTransform);
    }

    @Override
    public V transferFunds(long sourceId, long targetId, BigDecimal amount) throws AccountingException {
        if (sourceId == targetId) {
            throw new AccountingException("Cannot transfer funds within same account");
        } else if (amount == null || amount.signum() <= 0) {
            throw new AccountingException("Transfer amount must be a positive value");
        }

        Optional<Account> sourceAccount = repository.find(sourceId);
        Optional<Account> targetAccount = repository.find(targetId);

        if (sourceAccount.isPresent() && targetAccount.isPresent()) {
            Account source = sourceAccount.get();
            Account target = targetAccount.get();

            lockInOrder(source, target);
            try {
                if (source.funds.compareTo(amount) < 0) {
                    throw new AccountingException("Insufficient funds on source account");
                }

                source.funds = source.funds.subtract(amount);
                target.funds = target.funds.add(amount);

                repository.persist(source);
                repository.persist(target);

                return source.toView(viewTransform);
            } finally {
                unlockInOrder(source, target);
            }

        } else {
            throw new AccountingException("No account found for provided id");
        }
    }

    private static void lockInOrder(Account one, Account two) {
        if (one.id > two.id) {
            one.lockWrite();
            two.lockWrite();
        } else {
            two.lockWrite();
            one.lockWrite();
        }
    }

    private static void unlockInOrder(Account one, Account two) {
        if (one.id > two.id) {
            one.unlockWrite();
            two.unlockWrite();
        } else {
            two.unlockWrite();
            one.unlockWrite();
        }
    }
}
