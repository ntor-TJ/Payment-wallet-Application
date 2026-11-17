package com.masai.userSessions;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.masai.entities.Customer;
import com.masai.entities.UserSession;
import com.masai.entities.Wallet;
import com.masai.exception.UserNotFoundException;
import com.masai.exception.WalletNotFound;
import com.masai.repository.CustomerDao;
import com.masai.repository.UserSessionDao;

@Component
public class UserSessionsImpl implements UserSessions{
	
	@Autowired
	private UserSessionDao userSessionDao;
	
	@Autowired
	private CustomerDao customerDao;
	
	private UserSession validateSession(String key) {
		if (key == null || key.trim().isEmpty()) {
			throw new UserNotFoundException("Session key cannot be null or blank");
		}
		
		Optional<UserSession> optionalUserSession = userSessionDao.findByUuid(key);
		if (!optionalUserSession.isPresent()) {
			throw new UserNotFoundException("Unauthorized key");
		}
		
		UserSession userSession = optionalUserSession.get();
		if (userSession.getDateTime() == null || !userSession.getDateTime().plusHours(1).isAfter(LocalDateTime.now())) {
			userSessionDao.delete(userSession);
			throw new UserNotFoundException("Session has expired");
		}
		
		return userSession;
	}

	@Override
	public Wallet getCustomerWallet(String key) {
		Customer customer = getCustomer(key);
		Wallet wallet = customer.getWallet();
		if (wallet == null) {
			throw new WalletNotFound("Wallet does not exist for the customer");
		}
		return wallet;
	}

	@Override
	public Customer getCustomer(String key) {
		UserSession userSession = validateSession(key);
		
		if (userSession.getCustomerId() == null) {
			throw new UserNotFoundException("Customer not found for session");
		}
		
		Optional<Customer> optionalCustomer = customerDao.findById(userSession.getCustomerId());
		if (!optionalCustomer.isPresent()) {
			throw new UserNotFoundException("Customer not found");
		}
		
		return optionalCustomer.get();
	}

}
