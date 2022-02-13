package com.db.awmd.challenge.web.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class AccountTransferBalanceRequest {
  @NotNull
  private String receiverId;
  @NotNull
  @Min(value = 0, message = "Transfer amount must be positive.")
  private BigDecimal amount;
}
