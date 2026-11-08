package com.paymesh.user_service.controller;

import com.paymesh.user_service.dto.JwtResponse;
import com.paymesh.user_service.dto.LoginRequest;
import com.paymesh.user_service.dto.SignupRequest;
import com.paymesh.user_service.entity.User;
import com.paymesh.user_service.kafka.UserEventProducer;
import com.paymesh.user_service.repository.UserRepository;
import com.paymesh.user_service.util.JWTUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class  AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final UserEventProducer userEventProducer;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTUtil jwtUtil,
                          UserEventProducer userEventProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userEventProducer = userEventProducer;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request){
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest().body("⚠️ User already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole("ROLE_USER");  // Normal users only!
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);
        // Notify downstream services so the new user's wallet gets provisioned.
        userEventProducer.sendUserCreated(savedUser);

        return ResponseEntity.ok("✅ User registered successfully");
    }





    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim();

        System.out.println("[AUTH-DEBUG] Login email: [" + email + "]");

        Optional<User> userOpt = userRepository.findByEmail(email);

        System.out.println("[AUTH-DEBUG] User found: " + userOpt.isPresent());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }

        User user = userOpt.get();

        System.out.println("[AUTH-DEBUG] Stored hash present: "
                + (user.getPassword() != null));

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        System.out.println("[AUTH-DEBUG] Password matches: "
                + passwordMatches);

        if (!passwordMatches) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return ResponseEntity.ok(new JwtResponse(token));
    }

}
