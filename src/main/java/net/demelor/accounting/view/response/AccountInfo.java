package net.demelor.accounting.view.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class AccountInfo {
    public final long id;
    public final String name;
    public final String funds;

    public AccountInfo(long id, String name, BigDecimal funds) {
        this.id = id;
        this.name = name;
        this.funds = funds.setScale(2, RoundingMode.HALF_UP).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountInfo that = (AccountInfo) o;
        return id == that.id &&
                name.equals(that.name) &&
                funds.equals(that.funds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, funds);
    }
}
