package com.masai.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.masai.DTO.CustomerDTO;
import com.masai.entities.Customer;
import com.masai.entities.UserSession;
import com.masai.entities.Wallet;
import com.masai.repository.CustomerDao;

@Service
public class CustomerServiceImpl implements CustomerService{

	@Autowired
    private CustomerDao customerDao;
	
	@Autowired
	CurrentUserSessionDetailsImpl currentuser;
	
	@Override
	public CustomerDTO createCustomer(Customer customer) {
		
	   Optional<Customer> opt = customerDao.findByMobileNumber(customer.getMobileNumber());
		
	   if(opt.isPresent()){
		  System.out.println("User Already Exist"); 
	   }

	   if (customer.getWallet() == null) {
		   Wallet wallet = new Wallet();
		   wallet.setBalance(0.0);
		   wallet.setCustomer(customer);
		   customer.setWallet(wallet);
	   } else {
		   customer.getWallet().setCustomer(customer);
	   }
	   
	    customerDao.save(customer);
	    CustomerDTO dto = new CustomerDTO(customer.getMobileNumber(),customer.getPassword());
	    
	    return dto;
	}

	@Override
	public Customer updateCustomer(Customer customer, String key) {
		UserSession customer2 = currentuser.getcurrentUserSessionDetails(key);
		if (customer2 == null) {
			System.out.println("no user found");
		} else if (customer2.getCustomerId() != null) {
			Optional<Customer> opt = customerDao.findById(customer2.getCustomerId());
			if (opt.isPresent()) {
				Customer existingCustomer = opt.get();
				customer.setCustomerId(existingCustomer.getCustomerId());
				if (customer.getWallet() == null) {
					customer.setWallet(existingCustomer.getWallet());
				} else if (existingCustomer.getWallet() != null) {
					customer.getWallet().setBalance(existingCustomer.getWallet().getBalance());
					customer.getWallet().setCustomer(customer);
				}
			}
		}

		return customerDao.save(customer);
	}

	
}
