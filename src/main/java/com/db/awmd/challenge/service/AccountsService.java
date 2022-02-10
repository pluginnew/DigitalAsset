package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InvalidTransferDetailsException;
import com.db.awmd.challenge.exception.MissingTransferDetailsException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    @Getter
    private final EmailNotificationService emailNotificationService;



    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    /*public void debitAccount(String accountId) {
         this.accountsRepository.getAccount(accountId);
    }

    public void creditAccount(String accountId) {
         this.accountsRepository.getAccount(accountId);
    }
*/


    public void initiateTransfer(Map<String, Object> transferDetails) {
        try {

            String fromAccount = (String) getTransferDetails(transferDetails, "fromAccount");
            String toAccount = (String) getTransferDetails(transferDetails, "toAccount");
            BigDecimal transferAmount = new BigDecimal((String) getTransferDetails(transferDetails, "transferAmount"));

            if (!(transferAmount.compareTo(new BigDecimal(0)) == 1)) {
                throw new InvalidTransferDetailsException("Invalid Transfer Amount " + transferAmount);

            }
            accountsRepository.debitAccount(fromAccount, transferAmount);
            accountsRepository.creditAccount(toAccount, transferAmount);
            String debitTransactionDescription = "Amount:" + transferAmount + " debited from account:" + fromAccount;
            String creditTransactionDescription = "Amount:" + transferAmount + " credited to account:" + toAccount;
            emailNotificationService.notifyAboutTransfer(accountsRepository.getAccount(fromAccount), creditTransactionDescription);
            emailNotificationService.notifyAboutTransfer(accountsRepository.getAccount(toAccount), debitTransactionDescription);
        } catch (Exception excp) {
            throw excp;
        }
    }

    private Object getTransferDetails(Map<String, Object> transferDetails, String transferKey) {
        if (transferDetails.get(transferKey) != null) {
            return transferDetails.get(transferKey);
        } else {
            throw new MissingTransferDetailsException("Transfer details " + transferKey + " missing");
        }
    }
}
