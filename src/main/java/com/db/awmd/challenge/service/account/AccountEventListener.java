package com.db.awmd.challenge.service.account;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.NotificationService;
import com.db.awmd.challenge.service.account.events.AccountBalanceTransferredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AccountEventListener {

  private final NotificationService notificationService;

  @EventListener
  public void onAccountBalanceTransferred(AccountBalanceTransferredEvent event) {
    final AccountBalanceTransferredEvent.Payload payload = event.payload();
    sendTransferNotifications(payload.getSender(), payload.getReceiver(), payload.getAmount());
  }

  private void sendTransferNotifications(Account sender, Account receiver, BigDecimal amount) {
    sendTransferNotification(sender, "You've just sent %s to account #%s.",
            amount, receiver.getAccountId());
    sendTransferNotification(receiver, "You've just received %s from account #%s.",
            amount, sender.getAccountId());
  }

  private void sendTransferNotification(Account account, String msgTemplate, Object... msgArgs) {
    String msg = String.format(msgTemplate, msgArgs);
    notificationService.notifyAboutTransfer(account, msg);
  }
}
