package com.lapin.bodymechanic.service;

import com.lapin.bodymechanic.domain.Role;
import com.lapin.bodymechanic.domain.User;
import com.lapin.bodymechanic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        // new property: base URL used to build links in e-mails (app.base-url)
        ReflectionTestUtils.setField(userService, "baseUrl", "https://body.example.org");
    }

    @Test
    void updateProfile_encodesNewPassword() {
        User user = new User();
        user.setPassword("old-hash");
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");

        userService.updateProfile(user, "new-secret", null);

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_emptyPassword_keepsOldHash() {
        User user = new User();
        user.setPassword("old-hash");

        userService.updateProfile(user, "", null);

        assertEquals("old-hash", user.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void activateUser_marksUserActiveAndClearsCode() {
        User user = new User();
        user.setActive(false);
        user.setActivationCode("code-1");
        when(userRepository.findByActivationCode("code-1")).thenReturn(user);

        assertTrue(userService.activateUser("code-1"));

        assertTrue(user.isActive());
        assertNull(user.getActivationCode());
        verify(userRepository).save(user);
    }

    @Test
    void activateUser_unknownCode_returnsFalse() {
        when(userRepository.findByActivationCode("nope")).thenReturn(null);

        assertFalse(userService.activateUser("nope"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void addUser_activationLinkUsesConfiguredBaseUrl() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword("pw");
        user.setEmail("bob@example.org");
        when(userRepository.findByUsername("bob")).thenReturn(null);
        when(passwordEncoder.encode("pw")).thenReturn("hash");

        assertTrue(userService.addUser(user));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("bob@example.org"), anyString(), body.capture());
        assertTrue(body.getValue().contains("https://body.example.org/activate/" + user.getActivationCode()));
        assertFalse(body.getValue().contains("localhost"));
    }

    @Test
    void addUser_mailFailureDoesNotFailRegistration() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword("pw");
        user.setEmail("bob@example.org");
        when(userRepository.findByUsername("bob")).thenReturn(null);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        doThrow(new MailSendException("smtp is down"))
                .when(mailService).send(anyString(), anyString(), anyString());

        assertTrue(userService.addUser(user));

        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_mailFailureDoesNotFailTheUpdate() {
        User user = new User();
        user.setEmail("old@example.org");
        doThrow(new MailSendException("smtp is down"))
                .when(mailService).send(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> userService.updateProfile(user, "", "new@example.org"));

        verify(userRepository).save(user);
        assertEquals("new@example.org", user.getEmail());
    }

    @Test
    void addUser_existingUsername_returnsFalse() {
        User user = new User();
        user.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(new User());

        assertFalse(userService.addUser(user));
        verify(userRepository, never()).save(any());
    }

    @Test
    void addUser_setsUserRoleAndEncodedPassword() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword("pw");
        when(userRepository.findByUsername("bob")).thenReturn(null);
        when(passwordEncoder.encode("pw")).thenReturn("hash");

        userService.addUser(user);

        assertEquals("hash", user.getPassword());
        assertTrue(user.getRoles().contains(Role.USER));
        assertFalse(user.getRoles().contains(Role.ADMIN));
    }
}
