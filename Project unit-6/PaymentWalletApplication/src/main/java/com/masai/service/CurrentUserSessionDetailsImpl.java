package com.masai.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.masai.entities.Customer;
import com.masai.entities.UserSession;
import com.masai.exception.InvalidDetailsException;
import com.masai.exception.UserNotFoundException;
import com.masai.repository.CustomerDao;
import com.masai.repository.UserSessionDao;

@Service
public class CurrentUserSessionDetailsImpl implements CurrentUserSessionDetails{

	@Autowired
	private UserSessionDao sessionDao;
	
	@Autowired
	private CustomerDao customerDao;
	
	@Override
	public UserSession getcurrentUserSessionDetails(String key) {
		if (key == null || key.trim().isEmpty()) {
			throw new InvalidDetailsException("Session key cannot be null or blank");
		}

		Optional<UserSession> opt = sessionDao.findByUuid(key);
		if (!opt.isPresent()) {
			throw new InvalidDetailsException("Entered key is wrong");
		}

		UserSession userSession = opt.get();
		if (userSession.getDateTime() == null || !userSession.getDateTime().plusHours(1).isAfter(LocalDateTime.now())) {
			sessionDao.delete(userSession);
			throw new InvalidDetailsException("Session has expired");
		}

		if (userSession.getCustomerId() == null) {
			throw new UserNotFoundException("Customer not found for the given session");
		}

		Optional<Customer> customerOpt = customerDao.findById(userSession.getCustomerId());
		if (!customerOpt.isPresent()) {
			throw new UserNotFoundException("Customer not found for the given session");
		}

		return userSession;
	}

}
