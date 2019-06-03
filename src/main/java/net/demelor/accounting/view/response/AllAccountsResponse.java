package net.demelor.accounting.view.response;

import java.util.List;

public class AllAccountsResponse {
    public final List<AccountInfo> accounts;

    public AllAccountsResponse(List<AccountInfo> accounts) {
        this.accounts = accounts;
    }
}
