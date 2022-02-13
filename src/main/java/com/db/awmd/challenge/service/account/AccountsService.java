package com.db.awmd.challenge.service.account;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.NotEnoughAccountBalanceException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.account.events.AccountBalanceTransferredEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, ApplicationEventPublisher applicationEventPublisher) {
    this.accountsRepository = accountsRepository;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transfer(String senderId, String receiverId, BigDecimal amount) {
    if (BigDecimal.ZERO.compareTo(amount) >= 0) {
      throw new IllegalArgumentException("Transfer amount should be a positive value.");
    }

    final Account sender = getAccountSafe(senderId);
    final Account receiver = getAccountSafe(receiverId);

    final AccountSyncOrderResolver resolver = AccountSyncOrderResolver.resolve(sender, receiver);
    synchronized (resolver.getFirst()) {
      synchronized (resolver.getSecond()) {
        addBalance(sender, amount.negate());
        addBalance(receiver, amount);

        onTransferred(sender, receiver, amount);
      }
    }
  }

  private void onTransferred(Account sender, Account receiver, BigDecimal amount) {
    applicationEventPublisher.publishEvent(new AccountBalanceTransferredEvent(sender, receiver, amount));
  }

  private void addBalance(Account account, BigDecimal amount) {
    final BigDecimal initialBalance = account.getBalance();
    final BigDecimal updatedBalance = initialBalance.add(amount);
    if (updatedBalance.signum() < 0) {
      throw new NotEnoughAccountBalanceException(account, amount);
    }

    account.setBalance(updatedBalance);
  }

  private Account getAccountSafe(String accountId) {
    final Account account = accountsRepository.getAccount(accountId);
    return Optional.ofNullable(account).orElseThrow(() -> new AccountNotFoundException(accountId));
  }

  @Getter
  @RequiredArgsConstructor
  private static class AccountSyncOrderResolver {
    private final Account first;
    private final Account second;

    public static AccountSyncOrderResolver resolve(Account a1, Account a2) {
      return a1.getAccountId().compareTo(a2.getAccountId()) > 0
              ? new AccountSyncOrderResolver(a1, a2)
              : new AccountSyncOrderResolver(a2, a1);
    }
  }
}
