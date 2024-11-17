package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;
  
  @Mock
  private AccountsRepository accountRepository;

  @Mock
  private NotificationService notificationService;
  
  @InjectMocks
  private AccountsService accountService;
  
  private Account fromAccount;
  private Account toAccount;

  @BeforeEach
  void setUp() {
      fromAccount = new Account("fromAccountId", new BigDecimal("1000"));
      toAccount = new Account("toAccountId", new BigDecimal("500"));
  }

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }
  
  @Test
  void testTransfer_Success() {
      when(accountRepository.getAccount("fromAccountId")).thenReturn(fromAccount);
      when(accountRepository.getAccount("toAccountId")).thenReturn(toAccount);

      accountsService.transfer("fromAccountId", "toAccountId", new BigDecimal("200"));

      assertThat( fromAccount.getBalance()).isEqualTo(new BigDecimal("800"));
      assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal("700"));
      verify(notificationService).notifyAboutTransfer(fromAccount, "Transferred 200 to account toAccountId");
      verify(notificationService).notifyAboutTransfer(toAccount, "Received 200 from account fromAccountId");
  }

  @Test
  void testTransfer_InvalidAccountFrom() {
      when(accountRepository.getAccount("fromAccountId")).thenReturn(new Account(null));

      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.transfer("fromAccountId", "toAccountId", new BigDecimal("200"));
      });

      assertEquals("Invalid accountFrom ID", exception.getMessage());
  }

  @Test
  void testTransfer_InvalidAccountTo() {
      when(accountRepository.getAccount("fromAccountId")).thenReturn(fromAccount);
      when(accountRepository.getAccount("toAccountId")).thenReturn(new Account(null));

      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.transfer("fromAccountId", "toAccountId", new BigDecimal("200"));
      });

      assertEquals("Invalid accountTo ID", exception.getMessage());
  }

  @Test
  void testTransfer_InsufficientFunds() {
      when(accountRepository.getAccount("fromAccountId")).thenReturn(fromAccount);
      when(accountRepository.getAccount("toAccountId")).thenReturn(toAccount);

      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.transfer("fromAccountId", "toAccountId", new BigDecimal("2000"));
      });

      assertEquals("Insufficient funds", exception.getMessage());
  }

  @Test
  void testTransfer_NegativeAmount() {
      when(accountRepository.getAccount("fromAccountId")).thenReturn(fromAccount);
      when(accountRepository.getAccount("toAccountId")).thenReturn(toAccount);

      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.transfer("fromAccountId", "toAccountId", new BigDecimal("-200"));
      });

      assertEquals("Amount must be positive", exception.getMessage());
  }

  @Test
  void testTransfer_NullOrEmptyAccountFrom() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.transfer(null, "toAccountId", new BigDecimal("200"));
      });

      assertEquals("Both accounts must be valid and not null or empty", exception.getMessage());

      exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.transfer("", "toAccountId", new BigDecimal("200"));
      });

      assertEquals("Both accounts must be valid and not null or empty", exception.getMessage());
  }

  @Test
  void testTransfer_NullOrEmptyAccountTo() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.transfer("fromAccountId", null, new BigDecimal("200"));
      });

      assertEquals("Both accounts must be valid and not null or empty", exception.getMessage());

      exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.transfer("fromAccountId", "", new BigDecimal("200"));
      });

      assertEquals("Both accounts must be valid and not null or empty", exception.getMessage());
  }
  
  @Test
  void testDeposit_Success() {
	  accountsService.deposit(new BigDecimal("200"), toAccount);
      assertEquals(new BigDecimal("700"), toAccount.getBalance());
  }

  @Test
  void testDeposit_NegativeAmount() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.deposit(new BigDecimal("-200"), toAccount);
      });
      assertEquals("Amount must be positive", exception.getMessage());
  }

  @Test
  void testWithdraw_Success() {
	  accountsService.withdraw(new BigDecimal("200"), fromAccount);
      assertEquals(new BigDecimal("800"), fromAccount.getBalance());
  }

  @Test
  void testWithdraw_NegativeAmount() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.withdraw(new BigDecimal("-200"), fromAccount);
      });
      assertEquals("Amount must be positive", exception.getMessage());
  }

  @Test
  void testWithdraw_InsufficientFunds() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
    	  accountsService.withdraw(new BigDecimal("2000"), fromAccount);
      });
      assertEquals("Insufficient funds", exception.getMessage());
  }
}
