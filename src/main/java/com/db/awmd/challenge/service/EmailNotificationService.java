package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.web.AccountsController;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationService implements NotificationService {
    private Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Override
    public void notifyAboutTransfer(Account account, String transferDescription) {
        //THIS METHOD SHOULD NOT BE CHANGED - ASSUME YOUR COLLEAGUE WILL IMPLEMENT IT
        log
                .info("Sending notification to owner of {}: {}", account.getAccountId(), transferDescription);
    }

}
