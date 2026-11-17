package com.masai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.DTO.CustomerDTO;
import com.masai.entities.Customer;
import com.masai.entities.UserSession;
import com.masai.entities.Wallet;
import com.masai.exception.InvalidDetailsException;
import com.masai.exception.UserNotFoundException;
import com.masai.exception.WalletNotFound;
import com.masai.repository.CustomerDao;
import com.masai.repository.UserSessionDao;
import com.masai.service.CurrentUserSessionDetailsImpl;
import com.masai.service.CustomerAuthenticationImpl;
import com.masai.userSessions.UserSessionsImpl;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

	@Mock
	private CustomerDao customerDao;

	@Mock
	private UserSessionDao sessionDao;

	@InjectMocks
	private CustomerAuthenticationImpl authService;

	@InjectMocks
	private UserSessionsImpl userSessions;

	@InjectMocks
	private CurrentUserSessionDetailsImpl currentUserSessionDetails;

	private Customer sampleCustomer;
	private CustomerDTO sampleDto;
	private Wallet sampleWallet;

	@BeforeEach
	void setUp() {
		sampleWallet = new Wallet();
		sampleWallet.setWalletId(1);
		sampleWallet.setBalance(500.0);

		sampleCustomer = new Customer();
		sampleCustomer.setCustomerId(10);
		sampleCustomer.setName("Aman Kumar");
		sampleCustomer.setMobileNumber("9876543210");
		sampleCustomer.setPassword("Pass@1234");
		sampleCustomer.setWallet(sampleWallet);

		sampleDto = new CustomerDTO("9876543210", "Pass@1234");
	}

	@Test
	void testLogin_Successful() {
		when(customerDao.findByMobileNumber("9876543210")).thenReturn(Optional.of(sampleCustomer));
		when(sessionDao.findByCustomerId(10)).thenReturn(Optional.empty());
		when(sessionDao.findByUuid(any())).thenReturn(Optional.empty());

		String response = authService.login(sampleDto);

		assertNotNull(response);
		assertTrue(response.contains("customerId=10") || response.contains("10"));
		verify(sessionDao, times(1)).save(any(UserSession.class));
	}

	@Test
	void testLogin_InvalidMobileNumber_NullOrBlank() {
		CustomerDTO nullMobile = new CustomerDTO(null, "Pass@1234");
		CustomerDTO blankMobile = new CustomerDTO("   ", "Pass@1234");

		assertThrows(InvalidDetailsException.class, () -> authService.login(nullMobile));
		assertThrows(InvalidDetailsException.class, () -> authService.login(blankMobile));
		assertThrows(InvalidDetailsException.class, () -> authService.login(null));
	}

	@Test
	void testLogin_InvalidPassword_NullOrBlankOrMismatch() {
		CustomerDTO nullPass = new CustomerDTO("9876543210", null);
		CustomerDTO blankPass = new CustomerDTO("9876543210", "   ");
		CustomerDTO wrongPass = new CustomerDTO("9876543210", "Wrong@999");

		assertThrows(InvalidDetailsException.class, () -> authService.login(nullPass));
		assertThrows(InvalidDetailsException.class, () -> authService.login(blankPass));

		when(customerDao.findByMobileNumber("9876543210")).thenReturn(Optional.of(sampleCustomer));
		assertThrows(InvalidDetailsException.class, () -> authService.login(wrongPass));
	}

	@Test
	void testLogin_MissingCustomer() {
		when(customerDao.findByMobileNumber("9876543210")).thenReturn(Optional.empty());

		assertThrows(InvalidDetailsException.class, () -> authService.login(sampleDto));
	}

	@Test
	void testLogin_DuplicateActiveSession() {
		when(customerDao.findByMobileNumber("9876543210")).thenReturn(Optional.of(sampleCustomer));
		UserSession activeSession = new UserSession(10, LocalDateTime.now(), "activeKey123");
		when(sessionDao.findByCustomerId(10)).thenReturn(Optional.of(activeSession));

		assertThrows(InvalidDetailsException.class, () -> authService.login(sampleDto));
		verify(sessionDao, never()).save(any(UserSession.class));
	}

	@Test
	void testLogin_ExpiredSessionFollowedBySuccessfulLogin() {
		when(customerDao.findByMobileNumber("9876543210")).thenReturn(Optional.of(sampleCustomer));
		UserSession expiredSession = new UserSession(10, LocalDateTime.now().minusHours(2), "oldKey123");
		when(sessionDao.findByCustomerId(10)).thenReturn(Optional.of(expiredSession));
		when(sessionDao.findByUuid(any())).thenReturn(Optional.empty());

		String response = authService.login(sampleDto);

		assertNotNull(response);
		verify(sessionDao, times(1)).delete(expiredSession);
		verify(sessionDao, times(1)).save(any(UserSession.class));
	}

	@Test
	void testSessionValidation_MissingSessionKey() {
		assertThrows(UserNotFoundException.class, () -> userSessions.getCustomer(null));
		assertThrows(UserNotFoundException.class, () -> userSessions.getCustomer("   "));
		assertThrows(UserNotFoundException.class, () -> userSessions.getCustomerWallet(null));
		assertThrows(UserNotFoundException.class, () -> userSessions.getCustomerWallet("   "));

		assertThrows(InvalidDetailsException.class, () -> currentUserSessionDetails.getcurrentUserSessionDetails(null));
		assertThrows(InvalidDetailsException.class, () -> currentUserSessionDetails.getcurrentUserSessionDetails("   "));
	}

	@Test
	void testSessionValidation_InvalidSessionKey() {
		when(sessionDao.findByUuid("invalidKey")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> userSessions.getCustomer("invalidKey"));
		assertThrows(UserNotFoundException.class, () -> userSessions.getCustomerWallet("invalidKey"));
		assertThrows(InvalidDetailsException.class, () -> currentUserSessionDetails.getcurrentUserSessionDetails("invalidKey"));
	}

	@Test
	void testSessionValidation_ExpiredSessionValidation() {
		UserSession expiredSession = new UserSession(10, LocalDateTime.now().minusHours(2), "expiredKey");
		when(sessionDao.findByUuid("expiredKey")).thenReturn(Optional.of(expiredSession));

		assertThrows(UserNotFoundException.class, () -> userSessions.getCustomer("expiredKey"));
		verify(sessionDao, times(1)).delete(expiredSession);

		when(sessionDao.findByUuid("expiredKey2")).thenReturn(Optional.of(expiredSession));
		assertThrows(InvalidDetailsException.class, () -> currentUserSessionDetails.getcurrentUserSessionDetails("expiredKey2"));
		verify(sessionDao, times(2)).delete(expiredSession);
	}

	@Test
	void testSessionValidation_SuccessfulGetCustomerAndWallet() {
		UserSession validSession = new UserSession(10, LocalDateTime.now(), "validKey");
		when(sessionDao.findByUuid("validKey")).thenReturn(Optional.of(validSession));
		when(customerDao.findById(10)).thenReturn(Optional.of(sampleCustomer));

		Customer customer = userSessions.getCustomer("validKey");
		assertNotNull(customer);
		assertEquals("Aman Kumar", customer.getName());

		Wallet wallet = userSessions.getCustomerWallet("validKey");
		assertNotNull(wallet);
		assertEquals(500.0, wallet.getBalance());
	}

	@Test
	void testSessionValidation_CustomerWithoutWallet() {
		sampleCustomer.setWallet(null);
		UserSession validSession = new UserSession(10, LocalDateTime.now(), "validKey");
		when(sessionDao.findByUuid("validKey")).thenReturn(Optional.of(validSession));
		when(customerDao.findById(10)).thenReturn(Optional.of(sampleCustomer));

		assertThrows(WalletNotFound.class, () -> userSessions.getCustomerWallet("validKey"));
	}

	@Test
	void testLogout_Successful() {
		UserSession validSession = new UserSession(10, LocalDateTime.now(), "validLogoutKey");
		when(sessionDao.findByUuid("validLogoutKey")).thenReturn(Optional.of(validSession));

		String result = authService.logOut("validLogoutKey");

		assertEquals("Logged Out", result);
		verify(sessionDao, times(1)).delete(validSession);
	}

	@Test
	void testLogout_InvalidKey() {
		assertThrows(InvalidDetailsException.class, () -> authService.logOut(null));
		assertThrows(InvalidDetailsException.class, () -> authService.logOut("   "));

		when(sessionDao.findByUuid("unknownKey")).thenReturn(Optional.empty());
		assertThrows(InvalidDetailsException.class, () -> authService.logOut("unknownKey"));
	}

	@Test
	void testLogout_ExpiredKey() {
		UserSession expiredSession = new UserSession(10, LocalDateTime.now().minusHours(2), "expiredLogoutKey");
		when(sessionDao.findByUuid("expiredLogoutKey")).thenReturn(Optional.of(expiredSession));

		assertThrows(InvalidDetailsException.class, () -> authService.logOut("expiredLogoutKey"));
		verify(sessionDao, times(1)).delete(expiredSession);
	}

	@Test
	void testSessionValidation_ExactOneHourBoundary() {
		UserSession exactBoundarySession = new UserSession(10, LocalDateTime.now().minusHours(1), "exactBoundaryKey");
		when(sessionDao.findByUuid("exactBoundaryKey")).thenReturn(Optional.of(exactBoundarySession));

		assertThrows(UserNotFoundException.class, () -> userSessions.getCustomer("exactBoundaryKey"));
		verify(sessionDao, times(1)).delete(exactBoundarySession);

		when(sessionDao.findByUuid("exactBoundaryKey2")).thenReturn(Optional.of(exactBoundarySession));
		assertThrows(InvalidDetailsException.class, () -> currentUserSessionDetails.getcurrentUserSessionDetails("exactBoundaryKey2"));
		verify(sessionDao, times(2)).delete(exactBoundarySession);

		when(sessionDao.findByUuid("exactBoundaryLogoutKey")).thenReturn(Optional.of(exactBoundarySession));
		assertThrows(InvalidDetailsException.class, () -> authService.logOut("exactBoundaryLogoutKey"));
		verify(sessionDao, times(3)).delete(exactBoundarySession);
	}
}
