package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.NotEnoughAccountBalanceException;
import com.db.awmd.challenge.service.NotificationService;
import com.db.awmd.challenge.service.account.AccountsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;
  @MockBean
  private NotificationService notificationService;

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() {
    Account account = createAccount();
    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Test
  public void transfer() {
    final BigDecimal balance = BigDecimal.TEN;
    final BigDecimal amount = BigDecimal.ONE;
    final Account sender = createAccount(balance);
    final Account receiver = createAccount();

    assertBalanceEquals(sender.getAccountId(), balance);
    assertBalanceEmpty(receiver.getAccountId());

    accountsService.transfer(sender.getAccountId(), receiver.getAccountId(), amount);

    assertBalanceEquals(sender.getAccountId(), balance.subtract(amount));
    assertBalanceEquals(receiver.getAccountId(), amount);
  }

  @Test
  public void transfer_wholeBalance() {
    final BigDecimal balance = BigDecimal.TEN;
    final Account sender = createAccount(balance);
    final Account receiver = createAccount();

    assertBalanceEquals(sender.getAccountId(), balance);
    assertBalanceEmpty(receiver.getAccountId());

    accountsService.transfer(sender.getAccountId(), receiver.getAccountId(), sender.getBalance());

    assertBalanceEmpty(sender.getAccountId());
    assertBalanceEquals(receiver.getAccountId(), balance);
  }

  @Test(expected = NotEnoughAccountBalanceException.class)
  public void transfer_failsOnNotEnoughBalance() {
    final BigDecimal balance = BigDecimal.ONE;
    final BigDecimal amount = BigDecimal.TEN;
    final Account sender = createAccount(balance);
    final Account receiver = createAccount();

    assertBalanceEquals(sender.getAccountId(), balance);
    accountsService.transfer(sender.getAccountId(), receiver.getAccountId(), amount);
  }

  @Test
  public void transfer_failsOnAbsentSenderAccount() {
    final String senderId = generateAccountId();
    try {
      final Account receiver = createAccount();
      accountsService.transfer(senderId, receiver.getAccountId(), BigDecimal.TEN);
      fail("Should have failed if sender account is not exists.");
    } catch (AccountNotFoundException err) {
      assertThat(err.getMessage()).isEqualTo("Account #" + senderId + " is not found.");
    }
  }

  @Test
  public void transfer_failsOnAbsentReceiverAccount() {
    final String receiverId = generateAccountId();
    try {
      final Account sender = createAccount();
      accountsService.transfer(sender.getAccountId(), receiverId, BigDecimal.TEN);
      fail("Should have failed if receiver account is not exists.");
    } catch (AccountNotFoundException err) {
      assertThat(err.getMessage()).isEqualTo("Account #" + receiverId + " is not found.");
    }
  }

  @Test
  public void transfer_concurrentSeveralSenders() {
    final BigDecimal senderBalance = BigDecimal.TEN;
    final int senderCount = 10;
    final Set<String> senders = createAccounts(senderCount, () -> createAccount(senderBalance))
            .map(Account::getAccountId)
            .collect(toSet());
    final String receiver = createAccount().getAccountId();

    final CompletableFuture<?>[] futures = senders.stream()
            .flatMap(sender -> Stream.generate(() -> transferAsync(sender, receiver, BigDecimal.ONE)).limit(10))
            .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(futures).join();

    assertBalanceEquals(receiver, senderBalance.multiply(new BigDecimal(senderCount)));
    senders.forEach(this::assertBalanceEmpty);
  }

  @Test
  public void transfer_concurrentSeveralReceivers() {
    final String sender = createAccount(BigDecimal.TEN).getAccountId();
    final Set<String> receivers = createAccounts(10, this::createAccount)
            .map(Account::getAccountId)
            .collect(toSet());

    final CompletableFuture<?>[] futures = receivers.stream()
            .map(receiver -> transferAsync(sender, receiver, BigDecimal.ONE))
            .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(futures).join();

    assertBalanceEmpty(sender);
    receivers.forEach(receiver -> assertBalanceEquals(receiver, BigDecimal.ONE));
  }

  @Test
  public void transfer_concurrentBothDirections() {
    final Set<String> senders = createAccounts(10, () -> createAccount(BigDecimal.TEN))
            .map(Account::getAccountId)
            .collect(toSet());
    final Set<String> receivers = createAccounts(10, this::createAccount)
            .map(Account::getAccountId)
            .collect(toSet());
    final String intermediate = createAccount().getAccountId();

    final CompletableFuture<?>[] sending = senders.stream()
            .flatMap(sender -> Stream.generate(() -> transferAsync(sender, intermediate, BigDecimal.ONE)
                            .thenCompose(__ -> transferAsync(intermediate, receivers.iterator().next(), BigDecimal.ONE)))
                    .limit(10))
            .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(sending).join();

    assertBalanceEmpty(intermediate);
    senders.forEach(this::assertBalanceEmpty);
    final BigDecimal receivedTotalAmount = receivers.stream()
            .map(receiverId -> accountsService.getAccount(receiverId).getBalance())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    assertEquals(new BigDecimal(100), receivedTotalAmount);
  }

  @Test
  public void transfer_sendNotifications() {
    final Account sender = createAccount(BigDecimal.TEN);
    final Account receiver = createAccount();

    accountsService.transfer(sender.getAccountId(), receiver.getAccountId(), BigDecimal.ONE);

    ArgumentCaptor<Account> accArg = ArgumentCaptor.forClass(Account.class);
    ArgumentCaptor<String> msgArg = ArgumentCaptor.forClass(String.class);
    verify(notificationService, times(2)).notifyAboutTransfer(accArg.capture(), msgArg.capture());

    final List<Account> accounts = accArg.getAllValues();
    assertTrue(accounts.contains(sender));
    assertTrue(accounts.contains(receiver));
    msgArg.getAllValues().forEach(msg -> assertFalse(msg.isEmpty()));
  }

  private CompletableFuture<Void> transferAsync(String senderId, String receiverId, BigDecimal amount) {
    return runAsync(() -> accountsService.transfer(senderId, receiverId, amount));
  }

  private void assertBalanceEquals(String accountId, BigDecimal expectedBalance) {
    final Account account = accountsService.getAccount(accountId);
    assertNotNull(accountId);
    assertEquals(expectedBalance, account.getBalance());
  }

  private void assertBalanceEmpty(String accountId) {
    assertBalanceEquals(accountId, BigDecimal.ZERO);
  }

  private Account createAccount(Function<String, Account> factory) {
    final String accountId = generateAccountId();
    final Account account = factory.apply(accountId);
    accountsService.createAccount(account);
    return account;
  }

  private Account createAccount(BigDecimal balance) {
    return createAccount(accountId -> new Account(accountId, balance));
  }

  private Account createAccount() {
    return createAccount(Account::new);
  }

  private Stream<Account> createAccounts(int count, Supplier<Account> factory) {
    return Stream.generate(factory).limit(count);
  }

  public static String generateAccountId() {
    return "Id-" + UUID.randomUUID();
  }

}
