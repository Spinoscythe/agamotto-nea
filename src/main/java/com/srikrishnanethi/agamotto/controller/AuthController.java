package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.LoginRequest;
import com.srikrishnanethi.agamotto.dto.request.RegisterUserRequest;
import com.srikrishnanethi.agamotto.dto.response.AuthResponse;
import com.srikrishnanethi.agamotto.mapper.UserMapper;
import com.srikrishnanethi.agamotto.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final UserMapper userMapper;

    public AuthController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterUserRequest request) {
        UserService.AuthenticatedUser authenticatedUser = userService.register(request);
        return this.userMapper.toAuthResponse(authenticatedUser.token(), authenticatedUser.user());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserService.AuthenticatedUser authenticatedUser = this.userService.login(request.email(), request.password());
        return this.userMapper.toAuthResponse(authenticatedUser.token(), authenticatedUser.user());
    }
}
