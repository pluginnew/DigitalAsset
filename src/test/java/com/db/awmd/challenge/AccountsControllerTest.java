package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.account.AccountsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static com.db.awmd.challenge.AccountsServiceTest.generateAccountId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
            .andExpect(status().isOk())
            .andExpect(
                    content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  public void transfer() throws Exception {
    Account sender = new Account(generateAccountId(), BigDecimal.TEN);
    accountsService.createAccount(sender);
    Account receiver = new Account(generateAccountId());
    accountsService.createAccount(receiver);

    BigDecimal amount = BigDecimal.ONE;
    mockMvc.perform(post("/v1/accounts/" + sender.getAccountId() + "/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"receiverId\":\"" + receiver.getAccountId() + "\", \"amount\":" + amount + "}"))
            .andExpect(status().isOk());
  }

  @Test
  public void transferNotEnoughBalance() throws Exception {
    Account sender = new Account(generateAccountId(), BigDecimal.ONE);
    accountsService.createAccount(sender);
    Account receiver = new Account(generateAccountId());
    accountsService.createAccount(receiver);

    BigDecimal amount = BigDecimal.TEN;
    mockMvc.perform(post("/v1/accounts/" + sender.getAccountId() + "/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"receiverId\":\"" + receiver.getAccountId() + "\", \"amount\":" + amount + "}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void transferNegativeAmount() throws Exception {
    Account sender = new Account(generateAccountId(), BigDecimal.ONE);
    accountsService.createAccount(sender);
    Account receiver = new Account(generateAccountId());
    accountsService.createAccount(receiver);

    BigDecimal amount = BigDecimal.TEN.negate();
    mockMvc.perform(post("/v1/accounts/" + sender.getAccountId() + "/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"receiverId\":\"" + receiver.getAccountId() + "\", \"amount\":" + amount + "}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void transferReceiverNotFound() throws Exception {
    Account sender = new Account(generateAccountId(), BigDecimal.TEN);
    accountsService.createAccount(sender);

    BigDecimal amount = BigDecimal.ONE;
    mockMvc.perform(post("/v1/accounts/" + sender.getAccountId() + "/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"receiverId\":\"" + generateAccountId() + "\", \"amount\":" + amount + "}"))
            .andExpect(status().isNotFound());
  }

  @Test
  public void transferSenderNotFound() throws Exception {
    Account receiver = new Account(generateAccountId(), BigDecimal.TEN);
    accountsService.createAccount(receiver);

    BigDecimal amount = BigDecimal.ONE;
    mockMvc.perform(post("/v1/accounts/" + generateAccountId() + "/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"receiverId\":\"" + receiver.getAccountId() + "\", \"amount\":" + amount + "}"))
            .andExpect(status().isNotFound());
  }
}
