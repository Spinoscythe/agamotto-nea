package com.srikrishnanethi.agamotto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AgamottoApplication {
	static void main(String[] args) {
		SpringApplication.run(AgamottoApplication.class, args);
	}
}
