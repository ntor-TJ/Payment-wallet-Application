package com.masai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.DTO.CustomerDTO;
import com.masai.entities.Customer;
import com.masai.entities.Wallet;
import com.masai.repository.CustomerDao;
import com.masai.service.CustomerServiceImpl;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

	@Mock
	private CustomerDao customerDao;

	@InjectMocks
	private CustomerServiceImpl customerService;

	private Customer sampleCustomer;

	@BeforeEach
	void setUp() {
		sampleCustomer = new Customer();
		sampleCustomer.setName("Amit Sharma");
		sampleCustomer.setMobileNumber("9876543210");
		sampleCustomer.setPassword("Pass@1234");
		sampleCustomer.setEmail("amit@example.com");
	}

	@Test
	void testCreateCustomer_CreatesAndAssociatesNewWallet() {
		when(customerDao.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
		when(customerDao.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerDTO result = customerService.createCustomer(sampleCustomer);

		assertNotNull(result);
		assertEquals("9876543210", result.getMobileNumber());
		assertEquals("Pass@1234", result.getPassword());

		ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
		verify(customerDao, times(1)).save(customerCaptor.capture());

		Customer savedCustomer = customerCaptor.getValue();
		assertNotNull(savedCustomer.getWallet(), "Wallet should be created and associated with customer");
		assertEquals(0.0, savedCustomer.getWallet().getBalance(), "New wallet should have 0 initial balance");
		assertSame(savedCustomer, savedCustomer.getWallet().getCustomer(), "Wallet should refer back to customer");
	}

	@Test
	void testCreateCustomer_ExistingWalletPreservedAndAssociated() {
		Wallet customWallet = new Wallet();
		customWallet.setBalance(100.0);
		sampleCustomer.setWallet(customWallet);

		when(customerDao.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
		when(customerDao.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerDTO result = customerService.createCustomer(sampleCustomer);

		assertNotNull(result);
		ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
		verify(customerDao, times(1)).save(customerCaptor.capture());

		Customer savedCustomer = customerCaptor.getValue();
		assertNotNull(savedCustomer.getWallet());
		assertEquals(100.0, savedCustomer.getWallet().getBalance());
		assertSame(savedCustomer, savedCustomer.getWallet().getCustomer());
	}
}

