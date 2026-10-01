package com.paymesh.user_service.service;

import com.paymesh.user_service.dto.SignupRequest;
import com.paymesh.user_service.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User createUser(SignupRequest request);
    Optional<User> getUserById(Long id);
    List<User> getAllUsers();
}
