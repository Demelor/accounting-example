package net.demelor.accounting.model.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Account {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    long id;
    String name;
    volatile BigDecimal funds;

    Account(long id, BigDecimal initialFunds, String name) {
        this.id = id;
        this.funds = initialFunds.setScale(2, RoundingMode.HALF_UP);
        this.name = name;
    }

    /**
     * View interface to avoid reference leaks from model layer.
     * @param <T> resulting view type
     */
    public interface ViewTransform<T> {
        T createView(long id, String name, BigDecimal funds);
    }

    /**
     * Because all fields of Account are immutable we could just pass them
     * to ViewTransform implementation. In other cases defensive copy should
     * be used.
     */
    <V> V toView(ViewTransform<V> viewTransform) {
        return viewTransform.createView(this.id, this.name, this.funds);
    }

    void lockRead() {
        lock.readLock().lock();
    }

    void unlockRead() {
        lock.readLock().unlock();
    }

    void lockWrite() {
        lock.writeLock().lock();
    }

    void unlockWrite() {
        lock.writeLock().unlock();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id == account.id &&
                name.equals(account.name) &&
                funds.equals(account.funds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, funds);
    }
}
