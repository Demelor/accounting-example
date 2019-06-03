package net.demelor.accounting.model.account;

import net.demelor.accounting.exception.AccountingException;
import net.demelor.accounting.view.response.AccountInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AccountServiceImplTest {

    @Test
    void findAllAccounts_shouldReturnAllAccounts() {
        List<Account> mockAccounts = Arrays.asList(
                new Account(1, BigDecimal.valueOf(12.50).setScale(2, RoundingMode.HALF_UP), "Alice"),
                new Account(2, BigDecimal.valueOf(24.33).setScale(2, RoundingMode.HALF_UP), "Bob"),
                new Account(3, BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP), "Carol"));

        List<AccountInfo> accountViews = mockAccounts.stream()
                .map(account -> account.toView(AccountInfo::new))
                .collect(Collectors.toList());

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.findAll()).thenReturn(mockAccounts);

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);

        List<AccountInfo> result = service.findAllAccounts();

        assertIterableEquals(accountViews, result);
    }

    @Test
    void findAccount_shouldReturnAccountOrEmpty() {
        Account mockAccount = new Account(1, BigDecimal.valueOf(12.50).setScale(2, RoundingMode.HALF_UP), "Alice");
        AccountInfo mockAccountView = mockAccount.toView(AccountInfo::new);

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.find(1)).thenReturn(Optional.of(mockAccount));
        when(mockRepo.find(2)).thenReturn(Optional.empty());

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);

        assertEquals(service.findAccount(1), Optional.of(mockAccountView));
        assertEquals(service.findAccount(2), Optional.empty());

    }

    @Test
    void createAccount_shouldCreateNewAccountInRepository_andReturnCreated() throws Exception {
        String mockName = "Henry";
        BigDecimal mockAmount = BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP);

        Account mockAccount = new Account(1, mockAmount, mockName);
        AccountInfo mockAccountView = mockAccount.toView(AccountInfo::new);

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.create(mockName, mockAmount)).thenReturn(mockAccount);

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);
        AccountInfo account = service.createAccount(mockName, mockAmount);
        verify(mockRepo, times(1)).create(mockName, mockAmount);
        assertEquals(account, mockAccountView);
    }

    @Test
    void createAccount_shouldFail_withEmptyName() {
        BigDecimal mockAmount = BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP);
        AccountRepository mockRepo = mock(AccountRepository.class);

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);
        assertThrows(AccountingException.class, () -> service.createAccount(null, mockAmount));
        assertThrows(AccountingException.class, () -> service.createAccount("", mockAmount));
        assertThrows(AccountingException.class, () -> service.createAccount("   ", mockAmount));
    }

    @Test
    void createAccount_shouldFail_withNegativeFunds() {
        BigDecimal mockAmount = BigDecimal.valueOf(-30).setScale(2, RoundingMode.HALF_UP);
        AccountRepository mockRepo = mock(AccountRepository.class);

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);
        assertThrows(AccountingException.class, () -> service.createAccount("Jack", mockAmount));
    }

    @Test
    void transferFunds_shouldComplete_andPersistChanges_andReturnSourceAccountState() throws Exception {
        long mockSourceId = 1;
        long mockTargetId = 2;

        Account mockSourceAccount = new Account(1, BigDecimal.valueOf(70).setScale(2, RoundingMode.HALF_UP), "Alice");
        Account mockTargetAccount = new Account(2, BigDecimal.valueOf(10).setScale(2, RoundingMode.HALF_UP), "Bob");

        BigDecimal transferAmount = BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedSourceAmount = BigDecimal.valueOf(40).setScale(2, RoundingMode.HALF_UP);

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.find(mockSourceId)).thenReturn(Optional.of(mockSourceAccount));
        when(mockRepo.find(mockTargetId)).thenReturn(Optional.of(mockTargetAccount));

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);

        AccountInfo accountInfo = service.transferFunds(mockSourceId, mockTargetId, transferAmount);
        assertEquals(accountInfo, mockSourceAccount.toView(AccountInfo::new));
        assertEquals(accountInfo.funds, expectedSourceAmount.toString());
        verify(mockRepo, times(1)).persist(mockSourceAccount);
        verify(mockRepo, times(1)).persist(mockTargetAccount);
    }

    @Test
    void transferFunds_shouldFail_onInsufficientFunds() {
        long mockSourceId = 1;
        long mockTargetId = 2;

        Account mockSourceAccount = new Account(1, BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP), "Alice");
        Account mockTargetAccount = new Account(2, BigDecimal.valueOf(10).setScale(2, RoundingMode.HALF_UP), "Bob");

        BigDecimal transferAmount = BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP);

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.find(mockSourceId)).thenReturn(Optional.of(mockSourceAccount));
        when(mockRepo.find(mockTargetId)).thenReturn(Optional.of(mockTargetAccount));

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);

        assertThrows(AccountingException.class, () -> service.transferFunds(mockSourceId, mockTargetId, transferAmount));

        verify(mockRepo, never()).persist(mockSourceAccount);
        verify(mockRepo, never()).persist(mockTargetAccount);
    }

    @Test
    void transferFunds_shouldFail_onSameAccount() {
        BigDecimal transferAmount = BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP);

        long mockAccountId = 1;

        Account mockAccount = new Account(1, BigDecimal.valueOf(70).setScale(2, RoundingMode.HALF_UP), "Alice");

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.find(mockAccountId)).thenReturn(Optional.of(mockAccount));

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);

        assertThrows(AccountingException.class, () -> service.transferFunds(mockAccountId, mockAccountId, transferAmount));
    }

    @Test
    void transferFunds_shouldFail_onNonPositiveAmount() {
        BigDecimal negativeAmount = BigDecimal.valueOf(-30).setScale(2, RoundingMode.HALF_UP);
        BigDecimal zeroAmount = BigDecimal.valueOf(-30).setScale(2, RoundingMode.HALF_UP);

        long mockSourceId = 1;
        long mockTargetId = 2;

        Account mockSourceAccount = new Account(1, BigDecimal.valueOf(70).setScale(2, RoundingMode.HALF_UP), "Alice");
        Account mockTargetAccount = new Account(2, BigDecimal.valueOf(10).setScale(2, RoundingMode.HALF_UP), "Bob");

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.find(mockSourceId)).thenReturn(Optional.of(mockSourceAccount));
        when(mockRepo.find(mockTargetId)).thenReturn(Optional.of(mockTargetAccount));

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);

        assertThrows(AccountingException.class, () -> service.transferFunds(mockSourceId, mockTargetId, negativeAmount));
        assertThrows(AccountingException.class, () -> service.transferFunds(mockSourceId, mockTargetId, zeroAmount));
    }

    @Test
    void transferFunds_shouldFail_onUnknownAccountId() {
        BigDecimal transferAmount = BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP);

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.find(anyLong())).thenReturn(Optional.empty());

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);

        assertThrows(AccountingException.class, () -> service.transferFunds(1, 2, transferAmount));
    }

    @Test
    void transferFunds_shouldBeThreadSafe() throws Exception {
        long mockSourceId = 1;
        long mockTargetId = 2;

        Account mockSourceAccount = new Account(1, BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP), "Alice");
        Account mockTargetAccount = new Account(2, BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP), "Bob");

        BigDecimal transferAmount = BigDecimal.valueOf(1).setScale(2, RoundingMode.HALF_UP);

        BigDecimal expectedSourceAmount = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTargetAmount = BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP);

        AccountRepository mockRepo = mock(AccountRepository.class);
        when(mockRepo.find(mockSourceId)).thenReturn(Optional.of(mockSourceAccount));
        when(mockRepo.find(mockTargetId)).thenReturn(Optional.of(mockTargetAccount));

        AccountServiceImpl<AccountInfo> service = new AccountServiceImpl<>(mockRepo, AccountInfo::new);

        Runnable transferTask = () -> {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(10));
                service.transferFunds(mockSourceId, mockTargetId, transferAmount);
            } catch (Exception e) {
                fail(e);
            }
        };

        List<Thread> taskThreads = new LinkedList<>();
        for (int i = 0; i < 10000; i++) {
            Thread thread = new Thread(transferTask);
            taskThreads.add(thread);
            thread.start();
        }

        for (Thread thread : taskThreads) {
            thread.join();
        }

        assertEquals(expectedSourceAmount, mockSourceAccount.funds);
        assertEquals(expectedTargetAmount, mockTargetAccount.funds);

        verify(mockRepo, times(1000)).persist(mockSourceAccount);
        verify(mockRepo, times(1000)).persist(mockTargetAccount);
    }
}
