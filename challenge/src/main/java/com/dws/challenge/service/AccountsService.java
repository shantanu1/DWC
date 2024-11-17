package com.dws.challenge.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;

import lombok.Getter;

@Service
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	@Getter
	private final NotificationService notificationService;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	public synchronized void deposit(BigDecimal amount, Account accountTo) {
		if (amount.compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("Amount must be positive");
		accountTo.getBalance().add(amount);
	}

	public synchronized void withdraw(BigDecimal amount, Account accountFrom) {
		if (amount.compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("Amount must be positive");
		if (accountFrom.getBalance().compareTo(amount) < 0)
			throw new IllegalArgumentException("Insufficient funds");
		accountFrom.getBalance().subtract(amount);
	}

	@Transactional
	public void transfer(String accountFrom, String accountTo, BigDecimal amount) {
		try {
			if (StringUtils.isEmpty(accountFrom) || StringUtils.isEmpty(accountTo)) {
				throw new IllegalArgumentException("Both accounts must be valid and not null or empty");
			}

			Account fromAccount = accountsRepository.getAccount(accountFrom);

			Account toAccount = accountsRepository.getAccount(accountTo);

			if (ObjectUtils.isEmpty(fromAccount) || ObjectUtils.isEmpty(toAccount)) {
				throw new IllegalArgumentException("Both accounts must be valid ");
			}

			if (amount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Amount must be positive");
			}

			// Ensure locks are always acquired in the same order to prevent deadlock
			synchronized (fromAccount.getAccountId().compareTo(toAccount.getAccountId()) < 0 ? fromAccount : toAccount) {
				synchronized (fromAccount.getAccountId().compareTo(toAccount.getAccountId()) < 0 ? toAccount : fromAccount) {
					if (fromAccount.getBalance().compareTo(amount) < 0) {
						throw new IllegalArgumentException("Insufficient funds");
					}

					withdraw(amount, fromAccount);
					deposit(amount, toAccount);

					notificationService.notifyAboutTransfer(fromAccount,
							"Transferred " + amount + " to account " + toAccount.getAccountId());
					notificationService.notifyAboutTransfer(toAccount,
							"Received " + amount + " from account " + fromAccount.getAccountId());
				}
			}
		} catch (IllegalArgumentException e) {

			System.err.println("Transfer failed: " + e.getMessage());
			throw e;
		} catch (Exception e) {

			System.err.println("An unexpected error occurred during the transfer: " + e.getMessage());
			throw new RuntimeException("Transfer failed due to an unexpected error", e);
		}
	}

}
