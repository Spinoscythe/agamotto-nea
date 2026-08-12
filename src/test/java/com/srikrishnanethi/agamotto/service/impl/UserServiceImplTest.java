package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.jwt.JwtService;
import com.srikrishnanethi.agamotto.mapper.UserMapper;
import com.srikrishnanethi.agamotto.repositories.UserProfileRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserProfileRepository userProfileRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private UserMapper userMapper;
	@Mock
	private JwtService jwtService;

	private UserServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new UserServiceImpl(
				userRepository,
				userProfileRepository,
				passwordEncoder,
				userMapper,
				jwtService);
	}

	@Test
	void getByIdThrowsResourceNotFoundInsteadOfNoSuchElement() {
		when(userRepository.findById("missing")).thenReturn(Optional.empty());

		ResourceNotFoundException ex = assertThrows(
				ResourceNotFoundException.class,
				() -> service.getById("missing"));
		assertEquals("User not found: missing", ex.getMessage());
	}

	@Test
	void getByIdReturnsUser() {
		User user = new User();
		user.setId("u1");
		user.setFullName("Ada");
		user.setEmail("ada@example.com");
		when(userRepository.findById("u1")).thenReturn(Optional.of(user));

		assertEquals("u1", service.getById("u1").getId());
	}
}
