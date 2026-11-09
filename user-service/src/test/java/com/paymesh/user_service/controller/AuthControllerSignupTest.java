package com.paymesh.user_service.controller;

import com.paymesh.user_service.dto.SignupRequest;
import com.paymesh.user_service.entity.User;
import com.paymesh.user_service.kafka.UserEventProducer;
import com.paymesh.user_service.repository.UserRepository;
import com.paymesh.user_service.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerSignupTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JWTUtil jwtUtil;
    @Mock
    private UserEventProducer userEventProducer;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userRepository, passwordEncoder, jwtUtil, userEventProducer);
    }

    @Test
    void signup_publishesUserCreatedEventSoWalletCanBeProvisioned() {
        SignupRequest request = new SignupRequest("Ann", "ann@example.com", "pw", null);
        given(userRepository.findByEmail("ann@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("pw")).willReturn("encoded");

        User saved = new User();
        saved.setId(12L);
        saved.setEmail("ann@example.com");
        saved.setName("Ann");
        given(userRepository.save(any(User.class))).willReturn(saved);

        controller.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userEventProducer).sendUserCreated(captor.capture());
        assertEquals("ann@example.com", captor.getValue().getEmail());
    }

    @Test
    void signup_doesNotPublishWhenUserAlreadyExists() {
        SignupRequest request = new SignupRequest("Ann", "ann@example.com", "pw", null);
        given(userRepository.findByEmail("ann@example.com")).willReturn(Optional.of(new User()));

        controller.signup(request);

        verify(userEventProducer, never()).sendUserCreated(any());
    }
}