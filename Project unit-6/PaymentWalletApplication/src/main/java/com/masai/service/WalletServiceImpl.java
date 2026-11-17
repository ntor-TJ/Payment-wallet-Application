package com.masai.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.masai.entities.BankAccount;
import com.masai.entities.Customer;
import com.masai.entities.Transaction;
import com.masai.entities.TransactionType;
import com.masai.entities.Wallet;
import com.masai.exception.BankAccountNotFound;
import com.masai.exception.InsuficientBalance;
import com.masai.exception.InvalidAccountException;
import com.masai.exception.InvalidDetailsException;
import com.masai.exception.WalletNotFound;
import com.masai.repository.BankAccountDao;
import com.masai.repository.CustomerDao;
import com.masai.repository.TransactionDao;
import com.masai.repository.WalletDao;
import com.masai.userSessions.UserSessionsImpl;

@Service
public class WalletServiceImpl implements WalletService{

	@Autowired
	private BankAccountDao bankDao;
	
	@Autowired
	private WalletDao walletDao;
	
	@Autowired
	private TransactionDao transactionDao;
	
	@Autowired
	private CustomerDao customerDao;
	
	
	@Autowired
	private UserSessionsImpl userSessionsImpl;
	
	@Override
	@Transactional
	public String fundTransfer(String sourceMobileNo, String targetMobileNo, double amount, String key) {
		if (amount <= 0) {
			throw new InvalidDetailsException("Amount must be greater than zero");
		}

		if (key == null || key.trim().isEmpty()) {
			throw new InvalidDetailsException("Session key cannot be null or blank");
		}

		if (sourceMobileNo == null || targetMobileNo == null || sourceMobileNo.equals(targetMobileNo)) {
			throw new InvalidDetailsException("Source and target mobile numbers cannot be the same");
		}

		Wallet sourceWallet = userSessionsImpl.getCustomerWallet(key);
		Customer sourceCustomer = userSessionsImpl.getCustomer(key);

		if (sourceCustomer == null || !sourceCustomer.getMobileNumber().equals(sourceMobileNo)) {
			throw new InvalidDetailsException("Source mobile number does not match the logged-in customer");
		}

		if (sourceWallet == null) {
			throw new WalletNotFound("Source wallet does not exist");
		}

		Optional<Customer> optionalTargetCustomer = customerDao.findByMobileNumber(targetMobileNo);
		if (!optionalTargetCustomer.isPresent() || optionalTargetCustomer.get().getWallet() == null) {
			throw new WalletNotFound("Target wallet does not exist for mobile number: " + targetMobileNo);
		}

		Customer targetCustomer = optionalTargetCustomer.get();
		Wallet targetWallet = targetCustomer.getWallet();

		if (sourceWallet.getWalletId() != null && sourceWallet.getWalletId().equals(targetWallet.getWalletId())) {
			throw new InvalidDetailsException("Cannot transfer funds to the same wallet");
		}

		if (sourceWallet.getBalance() < amount) {
			throw new InsuficientBalance("Balance is not Sufficient in Source Wallet");
		}

		sourceWallet.setBalance(sourceWallet.getBalance() - amount);
		targetWallet.setBalance(targetWallet.getBalance() + amount);

		Transaction sourceTransaction = new Transaction();
		sourceTransaction.setTransactionType("WALLETTOWALLETFOUNDTRANSFER");
		sourceTransaction.setTransactionDate(LocalDate.now());
		sourceTransaction.setAmount(amount);
		sourceTransaction.setDescription("Fund Transfer from " + sourceMobileNo + " To " + targetMobileNo);
		sourceTransaction.setWallet(sourceWallet);

		Transaction targetTransaction = new Transaction();
		targetTransaction.setTransactionType("WALLETTOWALLETFOUNDTRANSFER");
		targetTransaction.setTransactionDate(LocalDate.now());
		targetTransaction.setAmount(amount);
		targetTransaction.setDescription("Fund Transfer from " + sourceMobileNo + " To " + targetMobileNo);
		targetTransaction.setWallet(targetWallet);

		if (sourceWallet.getTransactions() == null) {
			sourceWallet.setTransactions(new ArrayList<>());
		}
		if (targetWallet.getTransactions() == null) {
			targetWallet.setTransactions(new ArrayList<>());
		}

		sourceWallet.getTransactions().add(sourceTransaction);
		targetWallet.getTransactions().add(targetTransaction);

		transactionDao.save(sourceTransaction);
		transactionDao.save(targetTransaction);

		walletDao.save(sourceWallet);
		walletDao.save(targetWallet);

		return "Fund is Transferred from " + sourceCustomer.getName() + " To " + targetCustomer.getName();
	}
	
	@Override
	public double showBalance(String key) {
		if (key == null || key.trim().isEmpty()) {
			throw new InvalidDetailsException("Session key cannot be null or blank");
		}

		Wallet wallet = userSessionsImpl.getCustomerWallet(key);
		if (wallet == null) {
			throw new WalletNotFound("Wallet does not exist");
		}

		return wallet.getBalance();
	}


	@Override
	@Transactional
	public String addMoney(double amount, String key, Integer Accno) {
		if (amount <= 0) {
			throw new InvalidDetailsException("Amount must be greater than zero");
		}

		if (key == null || key.trim().isEmpty()) {
			throw new InvalidDetailsException("Session key cannot be null or blank");
		}

		Wallet wallet = userSessionsImpl.getCustomerWallet(key);
		if (wallet == null) {
			throw new WalletNotFound("Wallet does not exist");
		}

		Optional<BankAccount> optBank = bankDao.findByAccountNo(Accno);
		if (!optBank.isPresent()) {
			throw new BankAccountNotFound("Account does not exist");
		}

		BankAccount bankAccount = optBank.get();
		if (bankAccount.getWallet() == null || !bankAccount.getWallet().getWalletId().equals(wallet.getWalletId())) {
			throw new BankAccountNotFound("Bank account does not belong to the logged-in customer's wallet");
		}

		if (bankAccount.getBalance() < amount) {
			throw new InsuficientBalance("Balance is not Sufficient in Bank");
		}

		bankAccount.setBalance(bankAccount.getBalance() - amount);
		wallet.setBalance(wallet.getBalance() + amount);

		bankDao.save(bankAccount);

		Transaction transaction = new Transaction();
		transaction.setTransactionType("BankToWallet");
		transaction.setTransactionDate(LocalDate.now());
		transaction.setAmount(amount);
		transaction.setDescription("Fund Transfer from Bank to Wallet");
		transaction.setWallet(wallet);

		if (wallet.getTransactions() == null) {
			wallet.setTransactions(new ArrayList<>());
		}
		wallet.getTransactions().add(transaction);

		transactionDao.save(transaction);
		walletDao.save(wallet);

		return amount + " Rupee is Credited into Wallet";
	}



	@Override
	public List<BankAccount> bankAccountByWalletId(Integer walletId) {
		//Wallet wallet = userSessionsImpl.getCustomerWallet(key);
		
		Optional<Wallet> opt =  walletDao.findByWalletId(walletId);

		if(opt==null) {
			throw new WalletNotFound("wallet not found with given wallet id "+walletId);
		}
		
		if(!opt.isPresent()) {
		
		throw new BankAccountNotFound("bank account not found with given wallet id "+walletId);
		
		}
		
		return opt.get().getBankAccount();
		
	}


	@Override
	@Transactional
	public String depositAmount(double amount, String key, Integer Accno) {
		if (amount <= 0) {
			throw new InvalidDetailsException("Amount must be greater than zero");
		}

		if (key == null || key.trim().isEmpty()) {
			throw new InvalidDetailsException("Session key cannot be null or blank");
		}

		Wallet wallet = userSessionsImpl.getCustomerWallet(key);
		if (wallet == null) {
			throw new WalletNotFound("Wallet does not exist");
		}

		Optional<BankAccount> optBank = bankDao.findByAccountNo(Accno);
		if (!optBank.isPresent()) {
			throw new BankAccountNotFound("Account does not exist");
		}

		BankAccount bankAccount = optBank.get();
		if (bankAccount.getWallet() == null || !bankAccount.getWallet().getWalletId().equals(wallet.getWalletId())) {
			throw new BankAccountNotFound("Bank account does not belong to the logged-in customer's wallet");
		}

		if (wallet.getBalance() < amount) {
			throw new InsuficientBalance("Balance is not Sufficient in Wallet");
		}

		bankAccount.setBalance(bankAccount.getBalance() + amount);
		wallet.setBalance(wallet.getBalance() - amount);

		bankDao.save(bankAccount);

		Transaction transaction = new Transaction();
		transaction.setTransactionType("WalletToBank");
		transaction.setTransactionDate(LocalDate.now());
		transaction.setAmount(amount);
		transaction.setDescription("Fund Transfer from Wallet to Bank");
		transaction.setWallet(wallet);

		if (wallet.getTransactions() == null) {
			wallet.setTransactions(new ArrayList<>());
		}
		wallet.getTransactions().add(transaction);

		transactionDao.save(transaction);
		walletDao.save(wallet);

		return amount + " Rupee is Credited into Bank";
	}

	@Override
	public Customer getCustomerbyWalletId(Integer wlletId) {
		if (wlletId == null) {
			throw new WalletNotFound("Wallet id cannot be null");
		}

		Optional<Wallet> walletOpt = walletDao.findById(wlletId);
		if (!walletOpt.isPresent()) {
			throw new WalletNotFound("Wallet does not exist with id: " + wlletId);
		}

		Wallet wallet = walletOpt.get();
		Customer customer = wallet.getCustomer();
		
		return customer;
	}


	

	
	
	
	

	
}
