package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.account.AccountsService;
import com.db.awmd.challenge.web.dto.AccountTransferBalanceRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
      this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  @Data
  public static class TransferRequest {
    @NotNull
    private String receiverId;
    @NotNull
    @Min(value = 0, message = "Transfer amount must be positive.")
    private BigDecimal amount;
  }

  @PostMapping(value = "/{accountId}/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> transferBalance(@PathVariable String accountId,
                                           @RequestBody @Valid AccountTransferBalanceRequest request) {

    log.info("Transferring funds {} from account #{} to account #{}", request.getAmount(), accountId, request.getReceiverId());
    Exception error = null;
    HttpStatus status = HttpStatus.OK;
    try {
      this.accountsService.transfer(accountId, request.getReceiverId(), request.getAmount());
    } catch (AccountNotFoundException ex) {
      error = ex;
      status = HttpStatus.NOT_FOUND;
    } catch (Exception ex) {
      error = ex;
      status = HttpStatus.BAD_REQUEST;
    }

    return error == null
            ? new ResponseEntity<>(status)
            : new ResponseEntity<>(error.getMessage(), status);
  }
}
