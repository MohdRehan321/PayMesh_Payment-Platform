package com.paymesh.user_service.service;

import com.paymesh.user_service.dto.SignupRequest;
import com.paymesh.user_service.entity.User;
import com.paymesh.user_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 2. Inject PasswordEncoder through the constructor
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    // Accept SignupRequest instead of User entity
    public User createUser(SignupRequest request) {
        // Map the safe fields from the DTO to a brand new Entity
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Hardcode the default role so clients cannot override it
        user.setRole("ROLE_USER");

        // Encode the password before saving
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}