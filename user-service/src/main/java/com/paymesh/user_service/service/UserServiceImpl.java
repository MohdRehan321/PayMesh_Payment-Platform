package com.paymesh.user_service.service;

import com.paymesh.user_service.dto.SignupRequest;
import com.paymesh.user_service.entity.User;
import com.paymesh.user_service.kafka.UserEventProducer;
import com.paymesh.user_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventProducer userEventProducer;

    // 2. Inject PasswordEncoder through the constructor
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           UserEventProducer userEventProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userEventProducer = userEventProducer;
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

        User savedUser = userRepository.save(user);
        // Notify downstream services so the new user's wallet gets provisioned.
        userEventProducer.sendUserCreated(savedUser);
        return savedUser;
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