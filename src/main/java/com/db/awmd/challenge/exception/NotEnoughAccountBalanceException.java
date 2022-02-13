package com.db.awmd.challenge.exception;

import com.db.awmd.challenge.domain.Account;

import java.math.BigDecimal;

public class NotEnoughAccountBalanceException extends AccountException {

  public NotEnoughAccountBalanceException(Account account, BigDecimal requiredBalance) {
    super("Account #" + account.getAccountId() + " has not enough balance: " +
            "required " + requiredBalance + " but available " + account.getBalance() + ".");
  }
}
