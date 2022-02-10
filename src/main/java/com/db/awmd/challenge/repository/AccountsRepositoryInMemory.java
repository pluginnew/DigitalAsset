package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.db.awmd.challenge.exception.InvalidAccountException;
import com.db.awmd.challenge.exception.InvalidWithdrawalAmountException;
import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public void debitAccount(String accountId, BigDecimal amount) {
        Account account = getAccountDetails(accountId);
        if (account.getBalance().compareTo(amount) == -1) {
            throw new InvalidWithdrawalAmountException(
                    "Account id " + account.getAccountId() + " has insufficient balance(" + account.getBalance() + ") than transfer amount(" + amount+")");
        }
        synchronized (account) {
            account.setBalance(account.getBalance().subtract(amount));
        }
    }

    @Override
    public void creditAccount(String accountId, BigDecimal amount) {
        Account account = getAccountDetails(accountId);
        synchronized (account) {
            account.setBalance(account.getBalance().add(amount));
        }
    }

    @Override
    public void clearAccounts() {
        accounts.clear();
    }
    private Account getAccountDetails(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new InvalidAccountException(
                    "Account with id " + account.getAccountId() + " does not exists");
        }
        return account;
    }
}
