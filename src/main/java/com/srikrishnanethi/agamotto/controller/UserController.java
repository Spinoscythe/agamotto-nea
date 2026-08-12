package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.UpdateUserRequest;
import com.srikrishnanethi.agamotto.dto.response.UserResponse;
import com.srikrishnanethi.agamotto.mapper.UserMapper;
import com.srikrishnanethi.agamotto.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable String id) {
        return userMapper.toResponse(userService.getById(id));
    }

    @PatchMapping("/{id}")
    public UserResponse updateUser(@PathVariable String id, @RequestBody @Valid UpdateUserRequest request) {
        return userMapper.toResponse(userService.updateUser(id, request));
    }
}
