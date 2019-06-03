package net.demelor.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.demelor.accounting.model.account.AccountInMemoryStorage;
import net.demelor.accounting.model.account.AccountRepository;
import net.demelor.accounting.model.account.AccountService;
import net.demelor.accounting.model.account.AccountServiceImpl;
import net.demelor.accounting.exception.AccountingException;
import net.demelor.accounting.util.JsonTransformer;
import net.demelor.accounting.view.request.CreateAccountRequest;
import net.demelor.accounting.view.request.DoTransfer;
import net.demelor.accounting.view.response.AccountInfo;
import net.demelor.accounting.view.response.AllAccountsResponse;
import net.demelor.accounting.view.response.Message;
import spark.Response;
import spark.ResponseTransformer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static spark.Spark.*;

public class Application {
    public static void main(String[] args) {
        AccountRepository accountRepository = new AccountInMemoryStorage();
        AccountService<AccountInfo> accountService = new AccountServiceImpl<>(accountRepository, AccountInfo::new);

        ResponseTransformer responseTransformer = new JsonTransformer();
        ObjectMapper mapper = new ObjectMapper();

        port(8080);

        before("*", (req, res) -> {
            String path = req.pathInfo();
            if (path.endsWith("/") && path.length() > 1) {
               res.redirect(path.substring(0, path.length() - 1));
           }
        });

        path("/accounts", () -> {
            get("", (req, res) -> {
                List<AccountInfo> accounts = accountService.findAllAccounts();

                json(res, 200);
                return new AllAccountsResponse(accounts);
            }, responseTransformer);

            get("/:id", (req, res) -> {
                long id;
                try {
                    id = Long.parseLong(req.params("id"));
                } catch (NumberFormatException e) {
                    json(res, 400);
                    return new Message("Invalid account id format");
                }

                Optional<AccountInfo> account = accountService.findAccount(id);

                if (account.isPresent()) {
                    json(res, 200);
                    return account.get();
                } else {
                    json(res, 404);
                    return new Message("Cannot find account by provided id");
                }
            }, responseTransformer);

            post("/create", (req, res) -> {
                CreateAccountRequest createAccountRequest;
                BigDecimal initialFunds;
                String name;

                try {
                    createAccountRequest = mapper.readValue(req.body(), CreateAccountRequest.class);
                    initialFunds = BigDecimal.valueOf(Double.parseDouble(createAccountRequest.initialFunds))
                            .setScale(2, RoundingMode.HALF_UP);
                    name = createAccountRequest.name;
                } catch (Exception e) {
                    json(res, 400);
                    return new Message("Invalid request data format");
                }

                try {
                    AccountInfo account = accountService.createAccount(name, initialFunds);

                    json(res, 200);
                    return account;
                } catch (AccountingException e) {
                    json(res, 400);
                    return new Message(e.getMessage());
                }

            }, responseTransformer);
        });

        post("/transfer", (req, res) -> {
            DoTransfer doTransfer;
            BigDecimal amount;

            try {
                doTransfer = mapper.readValue(req.body(), DoTransfer.class);
                amount = BigDecimal.valueOf(Double.parseDouble(doTransfer.amount))
                        .setScale(2, RoundingMode.HALF_UP);
            } catch (Exception e) {
                json(res, 400);
                return new Message("Invalid request data format");
            }

            try {
                AccountInfo account = accountService.transferFunds(doTransfer.sourceId, doTransfer.targetId, amount);

                json(res, 200);
                return account;
            } catch (AccountingException e) {
                json(res, 400);
                return new Message(e.getMessage());
            }
        }, responseTransformer);

        notFound((req, res) -> {
            json(res, 404);
            return mapper.writeValueAsString(
                    new Message("Unknown requested method"));
        });

        internalServerError((req, res) -> {
            json(res, 500);
            return mapper.writeValueAsString(
                    new Message("Service error, contact support team"));
        });
    }

    private static void json(Response response, int status) {
        response.type("application/json");
        response.status(status);
    }
}
