package com.masai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.entities.BankAccount;
import com.masai.entities.Customer;
import com.masai.entities.Transaction;
import com.masai.entities.Wallet;
import com.masai.exception.BankAccountNotFound;
import com.masai.exception.InsuficientBalance;
import com.masai.exception.InvalidDetailsException;
import com.masai.exception.UserNotFoundException;
import com.masai.exception.WalletNotFound;
import com.masai.repository.BankAccountDao;
import com.masai.repository.CustomerDao;
import com.masai.repository.TransactionDao;
import com.masai.repository.WalletDao;
import com.masai.service.WalletServiceImpl;
import com.masai.userSessions.UserSessionsImpl;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

	@Mock
	private BankAccountDao bankDao;

	@Mock
	private WalletDao walletDao;

	@Mock
	private TransactionDao transactionDao;

	@Mock
	private CustomerDao customerDao;

	@Mock
	private UserSessionsImpl userSessionsImpl;

	@InjectMocks
	private WalletServiceImpl walletService;

	private Wallet userWallet;
	private BankAccount userBank;
	private Customer sourceCustomer;
	private String validKey = "testUuid10";
	private String sourceMobile = "9876543210";
	private String targetMobile = "9123456780";

	@BeforeEach
	void setUp() {
		userWallet = new Wallet();
		userWallet.setWalletId(1);
		userWallet.setBalance(1000.0);
		userWallet.setTransactions(null);

		sourceCustomer = new Customer();
		sourceCustomer.setCustomerId(101);
		sourceCustomer.setName("Rahul Sharma");
		sourceCustomer.setMobileNumber(sourceMobile);
		sourceCustomer.setWallet(userWallet);

		userBank = new BankAccount();
		userBank.setAccountNo(123456);
		userBank.setBalance(5000.0);
		userBank.setWallet(userWallet);
	}

	@Test
	void testAddMoney_RejectZeroOrNegativeAmount() {
		assertThrows(InvalidDetailsException.class, () -> walletService.addMoney(0.0, validKey, 123456));
		assertThrows(InvalidDetailsException.class, () -> walletService.addMoney(-100.0, validKey, 123456));
	}

	@Test
	void testDepositMoney_RejectZeroOrNegativeAmount() {
		assertThrows(InvalidDetailsException.class, () -> walletService.depositAmount(0.0, validKey, 123456));
		assertThrows(InvalidDetailsException.class, () -> walletService.depositAmount(-50.0, validKey, 123456));
	}

	@Test
	void testAddMoney_NonexistentBankAccount() {
		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(bankDao.findByAccountNo(999999)).thenReturn(Optional.empty());

		assertThrows(BankAccountNotFound.class, () -> walletService.addMoney(500.0, validKey, 999999));
	}

	@Test
	void testAddMoney_BankAccountDoesNotBelongToCustomer() {
		Wallet otherWallet = new Wallet();
		otherWallet.setWalletId(2);

		BankAccount otherBank = new BankAccount();
		otherBank.setAccountNo(654321);
		otherBank.setWallet(otherWallet);

		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(bankDao.findByAccountNo(654321)).thenReturn(Optional.of(otherBank));

		assertThrows(BankAccountNotFound.class, () -> walletService.addMoney(500.0, validKey, 654321));
	}

	@Test
	void testAddMoney_InsufficientBankBalance() {
		userBank.setBalance(200.0);
		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(bankDao.findByAccountNo(123456)).thenReturn(Optional.of(userBank));

		assertThrows(InsuficientBalance.class, () -> walletService.addMoney(500.0, validKey, 123456));
	}

	@Test
	void testAddMoney_Success() {
		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(bankDao.findByAccountNo(123456)).thenReturn(Optional.of(userBank));

		String result = walletService.addMoney(500.0, validKey, 123456);

		assertEquals("500.0 Rupee is Credited into Wallet", result);
		assertEquals(4500.0, userBank.getBalance());
		assertEquals(1500.0, userWallet.getBalance());
		assertNotNull(userWallet.getTransactions());
		assertEquals(1, userWallet.getTransactions().size());
		assertEquals("BankToWallet", userWallet.getTransactions().get(0).getTransactionType());
		assertEquals(userWallet, userWallet.getTransactions().get(0).getWallet());

		verify(bankDao).save(userBank);
		verify(walletDao).save(userWallet);
		verify(transactionDao).save(any(Transaction.class));
	}

	@Test
	void testDepositAmount_InsufficientWalletBalance() {
		userWallet.setBalance(100.0);
		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(bankDao.findByAccountNo(123456)).thenReturn(Optional.of(userBank));

		assertThrows(InsuficientBalance.class, () -> walletService.depositAmount(500.0, validKey, 123456));
	}

	@Test
	void testDepositAmount_Success() {
		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(bankDao.findByAccountNo(123456)).thenReturn(Optional.of(userBank));

		String result = walletService.depositAmount(400.0, validKey, 123456);

		assertEquals("400.0 Rupee is Credited into Bank", result);
		assertEquals(5400.0, userBank.getBalance());
		assertEquals(600.0, userWallet.getBalance());
		assertNotNull(userWallet.getTransactions());
		assertEquals(1, userWallet.getTransactions().size());
		assertEquals("WalletToBank", userWallet.getTransactions().get(0).getTransactionType());
		assertEquals(userWallet, userWallet.getTransactions().get(0).getWallet());

		verify(bankDao).save(userBank);
		verify(walletDao).save(userWallet);
		verify(transactionDao).save(any(Transaction.class));
	}

	// ==================== Fund Transfer Tests ====================

	@Test
	void testFundTransfer_RejectZeroOrNegativeAmount() {
		assertThrows(InvalidDetailsException.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 0.0, validKey));
		assertThrows(InvalidDetailsException.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, -50.0, validKey));
	}

	@Test
	void testFundTransfer_RejectSameMobileNumber() {
		assertThrows(InvalidDetailsException.class, () -> walletService.fundTransfer(sourceMobile, sourceMobile, 200.0, validKey));
	}

	@Test
	void testFundTransfer_SourceCustomerDoesNotMatchSourceMobile() {
		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(userSessionsImpl.getCustomer(validKey)).thenReturn(sourceCustomer);

		assertThrows(InvalidDetailsException.class, () -> walletService.fundTransfer("9999999999", targetMobile, 200.0, validKey));
	}

	@Test
	void testFundTransfer_TargetCustomerNotFound() {
		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(userSessionsImpl.getCustomer(validKey)).thenReturn(sourceCustomer);
		when(customerDao.findByMobileNumber(targetMobile)).thenReturn(Optional.empty());

		assertThrows(WalletNotFound.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 200.0, validKey));
	}

	@Test
	void testFundTransfer_TargetWalletNotFound() {
		Customer targetCustomer = new Customer();
		targetCustomer.setCustomerId(102);
		targetCustomer.setMobileNumber(targetMobile);
		targetCustomer.setWallet(null);

		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(userSessionsImpl.getCustomer(validKey)).thenReturn(sourceCustomer);
		when(customerDao.findByMobileNumber(targetMobile)).thenReturn(Optional.of(targetCustomer));

		assertThrows(WalletNotFound.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 200.0, validKey));
	}

	@Test
	void testFundTransfer_RejectSameWallet() {
		Customer targetCustomer = new Customer();
		targetCustomer.setCustomerId(102);
		targetCustomer.setMobileNumber(targetMobile);
		targetCustomer.setWallet(userWallet); // Same wallet instance / ID

		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(userSessionsImpl.getCustomer(validKey)).thenReturn(sourceCustomer);
		when(customerDao.findByMobileNumber(targetMobile)).thenReturn(Optional.of(targetCustomer));

		assertThrows(InvalidDetailsException.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 200.0, validKey));
	}

	@Test
	void testFundTransfer_InsufficientSourceBalance() {
		userWallet.setBalance(100.0);

		Wallet targetWallet = new Wallet();
		targetWallet.setWalletId(2);
		targetWallet.setBalance(500.0);

		Customer targetCustomer = new Customer();
		targetCustomer.setCustomerId(102);
		targetCustomer.setMobileNumber(targetMobile);
		targetCustomer.setWallet(targetWallet);

		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(userSessionsImpl.getCustomer(validKey)).thenReturn(sourceCustomer);
		when(customerDao.findByMobileNumber(targetMobile)).thenReturn(Optional.of(targetCustomer));

		assertThrows(InsuficientBalance.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 500.0, validKey));
	}

	@Test
	void testFundTransfer_Success() {
		userWallet.setBalance(1000.0);

		Wallet targetWallet = new Wallet();
		targetWallet.setWalletId(2);
		targetWallet.setBalance(500.0);
		targetWallet.setTransactions(null);

		Customer targetCustomer = new Customer();
		targetCustomer.setCustomerId(102);
		targetCustomer.setName("Priya Verma");
		targetCustomer.setMobileNumber(targetMobile);
		targetCustomer.setWallet(targetWallet);

		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(userSessionsImpl.getCustomer(validKey)).thenReturn(sourceCustomer);
		when(customerDao.findByMobileNumber(targetMobile)).thenReturn(Optional.of(targetCustomer));

		String result = walletService.fundTransfer(sourceMobile, targetMobile, 300.0, validKey);

		assertEquals("Fund is Transferred from Rahul Sharma To Priya Verma", result);
		assertEquals(700.0, userWallet.getBalance());
		assertEquals(800.0, targetWallet.getBalance());

		assertNotNull(userWallet.getTransactions());
		assertEquals(1, userWallet.getTransactions().size());
		assertEquals("WALLETTOWALLETFOUNDTRANSFER", userWallet.getTransactions().get(0).getTransactionType());
		assertEquals(userWallet, userWallet.getTransactions().get(0).getWallet());

		assertNotNull(targetWallet.getTransactions());
		assertEquals(1, targetWallet.getTransactions().size());
		assertEquals("WALLETTOWALLETFOUNDTRANSFER", targetWallet.getTransactions().get(0).getTransactionType());
		assertEquals(targetWallet, targetWallet.getTransactions().get(0).getWallet());

		verify(transactionDao, times(2)).save(any(Transaction.class));
		verify(walletDao).save(userWallet);
		verify(walletDao).save(targetWallet);
	}

	@Test
	void testShowBalance_MissingOrBlankSessionKey() {
		assertThrows(InvalidDetailsException.class, () -> walletService.showBalance(null));
		assertThrows(InvalidDetailsException.class, () -> walletService.showBalance("   "));
	}

	@Test
	void testShowBalance_InvalidSessionKey() {
		String invalidKey = "invalidKey";
		when(userSessionsImpl.getCustomerWallet(invalidKey)).thenThrow(new UserNotFoundException("Unauthorized key"));
		assertThrows(UserNotFoundException.class, () -> walletService.showBalance(invalidKey));
	}

	@Test
	void testAddMoney_MissingOrBlankSessionKey() {
		assertThrows(InvalidDetailsException.class, () -> walletService.addMoney(100.0, null, 123456));
		assertThrows(InvalidDetailsException.class, () -> walletService.addMoney(100.0, "   ", 123456));
	}

	@Test
	void testAddMoney_InvalidSessionKey() {
		String invalidKey = "invalidKey";
		when(userSessionsImpl.getCustomerWallet(invalidKey)).thenThrow(new UserNotFoundException("Unauthorized key"));
		assertThrows(UserNotFoundException.class, () -> walletService.addMoney(100.0, invalidKey, 123456));
	}

	@Test
	void testDepositAmount_MissingOrBlankSessionKey() {
		assertThrows(InvalidDetailsException.class, () -> walletService.depositAmount(100.0, null, 123456));
		assertThrows(InvalidDetailsException.class, () -> walletService.depositAmount(100.0, "   ", 123456));
	}

	@Test
	void testDepositAmount_InvalidSessionKey() {
		String invalidKey = "invalidKey";
		when(userSessionsImpl.getCustomerWallet(invalidKey)).thenThrow(new UserNotFoundException("Unauthorized key"));
		assertThrows(UserNotFoundException.class, () -> walletService.depositAmount(100.0, invalidKey, 123456));
	}

	@Test
	void testFundTransfer_MissingOrBlankSessionKey() {
		assertThrows(InvalidDetailsException.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 100.0, null));
		assertThrows(InvalidDetailsException.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 100.0, "   "));
	}

	@Test
	void testFundTransfer_InvalidSessionKey() {
		String invalidKey = "invalidKey";
		when(userSessionsImpl.getCustomerWallet(invalidKey)).thenThrow(new UserNotFoundException("Unauthorized key"));
		assertThrows(UserNotFoundException.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 100.0, invalidKey));
	}

	@Test
	void testFundTransfer_FailedTransferRollsBack_NeitherWalletSaved() {
		userWallet.setBalance(100.0);

		Wallet targetWallet = new Wallet();
		targetWallet.setWalletId(2);
		targetWallet.setBalance(500.0);

		Customer targetCustomer = new Customer();
		targetCustomer.setCustomerId(102);
		targetCustomer.setName("Priya Verma");
		targetCustomer.setMobileNumber(targetMobile);
		targetCustomer.setWallet(targetWallet);

		when(userSessionsImpl.getCustomerWallet(validKey)).thenReturn(userWallet);
		when(userSessionsImpl.getCustomer(validKey)).thenReturn(sourceCustomer);
		when(customerDao.findByMobileNumber(targetMobile)).thenReturn(Optional.of(targetCustomer));

		// Transfer amount exceeds balance: 500 > 100
		assertThrows(InsuficientBalance.class, () -> walletService.fundTransfer(sourceMobile, targetMobile, 500.0, validKey));

		// Verify neither wallet was saved and balances are untouched
		assertEquals(100.0, userWallet.getBalance());
		assertEquals(500.0, targetWallet.getBalance());
		verify(walletDao, never()).save(any(Wallet.class));
		verify(transactionDao, never()).save(any(Transaction.class));
	}
}
