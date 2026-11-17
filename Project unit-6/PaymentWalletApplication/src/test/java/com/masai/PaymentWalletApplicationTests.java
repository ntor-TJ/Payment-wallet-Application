package com.masai;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires running MySQL instance on localhost:3306")
class PaymentWalletApplicationTests {

	@Test
	void contextLoads() {
	}

}
