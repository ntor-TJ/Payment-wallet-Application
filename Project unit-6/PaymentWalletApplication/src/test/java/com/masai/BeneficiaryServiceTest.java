package com.masai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.entities.BeneficiaryDetails;
import com.masai.entities.Customer;
import com.masai.entities.Wallet;
import com.masai.exception.BeneficiaryException;
import com.masai.repository.BeneficiaryDao;
import com.masai.repository.WalletDao;
import com.masai.service.BeneficiaryServiceImpl;

@ExtendWith(MockitoExtension.class)
public class BeneficiaryServiceTest {

	@Mock
	private BeneficiaryDao bDao;

	@Mock
	private WalletDao wDao;

	@InjectMocks
	private BeneficiaryServiceImpl beneficiaryService;

	private Wallet userWallet;
	private BeneficiaryDetails sampleBd;

	@BeforeEach
	void setUp() {
		userWallet = new Wallet();
		userWallet.setWalletId(1);
		userWallet.setBalance(1500.0);
		userWallet.setBd(new ArrayList<>());

		sampleBd = new BeneficiaryDetails();
		sampleBd.setMobileNumber("9876543210");
		sampleBd.setName("Sandeep");
		Wallet inputWallet = new Wallet();
		inputWallet.setWalletId(1);
		sampleBd.setWallet(inputWallet);
	}

	@Test
	void testAddBeneficiary_DoesNotChangeWalletBalance() {
		when(bDao.existsById("9876543210")).thenReturn(false);
		when(wDao.findById(1)).thenReturn(Optional.of(userWallet));
		when(bDao.save(any(BeneficiaryDetails.class))).thenAnswer(invocation -> invocation.getArgument(0));

		double originalBalance = userWallet.getBalance();
		BeneficiaryDetails result = beneficiaryService.addBeneficiary(sampleBd);

		assertNotNull(result);
		assertEquals(originalBalance, userWallet.getBalance(), "Wallet balance must remain unchanged when adding beneficiary");
		assertEquals(1500.0, userWallet.getBalance());
		verify(wDao, never()).save(any(Wallet.class));
	}

	@Test
	void testDeleteBeneficiary_DoesNotChangeWalletBalance() {
		sampleBd.setWallet(userWallet);
		userWallet.getBd().add(sampleBd);
		when(bDao.findById("9876543210")).thenReturn(Optional.of(sampleBd));

		double originalBalance = userWallet.getBalance();
		BeneficiaryDetails deleted = beneficiaryService.deleteBeneficiary(sampleBd);

		assertNotNull(deleted);
		assertEquals(originalBalance, userWallet.getBalance(), "Wallet balance must remain unchanged when deleting beneficiary");
		assertEquals(1500.0, userWallet.getBalance());
	}

	@Test
	void testDeleteBeneficiary_DoesNotDeleteWallet() {
		sampleBd.setWallet(userWallet);
		when(bDao.findById("9876543210")).thenReturn(Optional.of(sampleBd));

		beneficiaryService.deleteBeneficiary(sampleBd);

		verify(bDao, times(1)).delete(sampleBd);
		verify(wDao, never()).delete(any(Wallet.class));
		verify(wDao, never()).deleteById(anyInt());
	}

	@Test
	void testBeneficiary_RemainsLinkedToCorrectWallet() {
		when(bDao.existsById("9876543210")).thenReturn(false);
		when(wDao.findById(1)).thenReturn(Optional.of(userWallet));
		when(bDao.save(any(BeneficiaryDetails.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BeneficiaryDetails result = beneficiaryService.addBeneficiary(sampleBd);

		assertNotNull(result.getWallet(), "Beneficiary must be linked to a wallet");
		assertEquals(1, result.getWallet().getWalletId(), "Beneficiary must be linked to wallet ID 1");
		assertSame(userWallet, result.getWallet());
	}

	@Test
	void testAddBeneficiary_DuplicateRejected() {
		when(bDao.existsById("9876543210")).thenReturn(true);

		BeneficiaryException exception = assertThrows(BeneficiaryException.class,
				() -> beneficiaryService.addBeneficiary(sampleBd));
		assertEquals("Beneficiary already exists", exception.getMessage());
		verify(bDao, never()).save(any());
	}

	@Test
	void testAddBeneficiary_InvalidWalletIdRejected() {
		sampleBd.getWallet().setWalletId(999);
		when(bDao.existsById("9876543210")).thenReturn(false);
		when(wDao.findById(999)).thenReturn(Optional.empty());

		BeneficiaryException exception = assertThrows(BeneficiaryException.class,
				() -> beneficiaryService.addBeneficiary(sampleBd));
		assertEquals("Wallet not found", exception.getMessage());
		verify(bDao, never()).save(any());
	}

	@Test
	void testViewAllBeneficiary_DoesNotChangeWalletData() {
		sampleBd.setWallet(userWallet);
		userWallet.getBd().add(sampleBd);

		Customer customer = new Customer();
		customer.setWallet(userWallet);

		when(wDao.findById(1)).thenReturn(Optional.of(userWallet));

		List<BeneficiaryDetails> beneficiaries = beneficiaryService.viewAllBeneficiary(customer);

		assertEquals(1, beneficiaries.size());
		assertEquals(1500.0, userWallet.getBalance(), "Viewing beneficiaries must never modify wallet balance");
		verify(wDao, never()).save(any());
	}

	@Test
	void testAddBeneficiary_DistinctMergedInstanceHandling() {
		BeneficiaryDetails mergedInstance = new BeneficiaryDetails();
		mergedInstance.setMobileNumber("9876543213");
		mergedInstance.setName("FinalTest");
		mergedInstance.setWallet(userWallet);

		BeneficiaryDetails inputBd = new BeneficiaryDetails();
		inputBd.setMobileNumber("9876543213");
		inputBd.setName("FinalTest");
		Wallet inputWallet = new Wallet();
		inputWallet.setWalletId(1);
		inputBd.setWallet(inputWallet);

		when(bDao.existsById("9876543213")).thenReturn(false);
		when(wDao.findById(1)).thenReturn(Optional.of(userWallet));
		when(bDao.save(inputBd)).thenReturn(mergedInstance);

		BeneficiaryDetails result = beneficiaryService.addBeneficiary(inputBd);

		assertNotNull(result);
		assertEquals("9876543213", result.getMobileNumber());
		assertSame(mergedInstance, result);
		assertTrue(userWallet.getBd().contains(mergedInstance));
		assertEquals(1, userWallet.getBd().size());
	}
}
