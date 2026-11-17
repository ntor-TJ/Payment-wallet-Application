package com.masai.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.masai.DTO.CustomerDTO;
import com.masai.entities.Customer;
import com.masai.entities.UserSession;
import com.masai.exception.InvalidDetailsException;
import com.masai.repository.CustomerDao;
import com.masai.repository.UserSessionDao;

import net.bytebuddy.utility.RandomString;

@Service
public class CustomerAuthenticationImpl implements CustomerAuthentication{

	@Autowired
	CustomerDao customerDao;
	
	@Autowired
	UserSessionDao sessionDao;
	
	@Autowired
	CurrentUserSessionDetailsImpl currentUser;

	@Override
	public String login(CustomerDTO customerDTO) {
		if (customerDTO == null || customerDTO.getMobileNumber() == null || customerDTO.getMobileNumber().trim().isEmpty()) {
			throw new InvalidDetailsException("Please Check the credentials");
		}
		if (customerDTO.getPassword() == null || customerDTO.getPassword().trim().isEmpty()) {
			throw new InvalidDetailsException("Please Check the credentials");
		}

		Optional<Customer> opt = customerDao.findByMobileNumber(customerDTO.getMobileNumber());
		if (!opt.isPresent()) {
			throw new InvalidDetailsException("Please Check the credentials");
		}

		Customer customer = opt.get();
		if (!customer.getPassword().equals(customerDTO.getPassword())) {
			throw new InvalidDetailsException("Please Check the credentials");
		}

		Integer customerId = customer.getCustomerId();
		Optional<UserSession> currentUserOpt = sessionDao.findByCustomerId(customerId);

		if (currentUserOpt.isPresent()) {
			UserSession existingSession = currentUserOpt.get();
			if (existingSession.getDateTime() == null || !existingSession.getDateTime().plusHours(1).isAfter(LocalDateTime.now())) {
				sessionDao.delete(existingSession);
			} else {
				throw new InvalidDetailsException("User is already logged in");
			}
		}

		String key;
		do {
			key = RandomString.make(10);
		} while (sessionDao.findByUuid(key).isPresent());

		UserSession userSession = new UserSession(customer.getCustomerId(), LocalDateTime.now(), key);
		sessionDao.save(userSession);

		return userSession.toString();
	}

	@Override
	public String logOut(String key) {
		if (key == null || key.trim().isEmpty()) {
			throw new InvalidDetailsException("Entered key is wrong");
		}

		Optional<UserSession> currentUser = sessionDao.findByUuid(key);
		if (!currentUser.isPresent()) {
			throw new InvalidDetailsException("Entered key is wrong");
		}

		UserSession userSession = currentUser.get();
		if (userSession.getDateTime() == null || !userSession.getDateTime().plusHours(1).isAfter(LocalDateTime.now())) {
			sessionDao.delete(userSession);
			throw new InvalidDetailsException("Session has expired");
		}

		sessionDao.delete(userSession);
		return "Logged Out";
	}

}
