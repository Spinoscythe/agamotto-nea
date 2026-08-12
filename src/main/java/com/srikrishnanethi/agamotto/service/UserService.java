package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.dto.request.RegisterUserRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateUserRequest;
import com.srikrishnanethi.agamotto.entities.User;

public interface UserService {
    AuthenticatedUser register(RegisterUserRequest request);

    AuthenticatedUser login(String email, String password);

    User getById(String userId);

    User updateUser(String userId, UpdateUserRequest request);

    record AuthenticatedUser(User user, String token) {
    }
}
