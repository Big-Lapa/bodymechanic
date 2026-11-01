package com.lapin.bodymechanic.config;

import com.lapin.bodymechanic.domain.Role;
import com.lapin.bodymechanic.domain.User;
import com.lapin.bodymechanic.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    private static final String STRONG = "a-long-enough-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer initializer;

    private void password(String value) {
        ReflectionTestUtils.setField(initializer, "adminPassword", value);
    }

    @Test
    void emptyPassword_doesNothing() {
        password("");

        initializer.run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void nullPassword_doesNothing() {
        password(null);

        initializer.run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void blankPassword_doesNothing() {
        password("   ");

        initializer.run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void tooShortPassword_refusesToStartAndSavesNothing() {
        password("short");

        assertThrows(IllegalStateException.class, () -> initializer.run(null));

        verify(userRepository, never()).save(any());
    }

    @Test
    void noAdminYet_createsActiveAdminWithEncodedPassword() {
        password(STRONG);
        when(userRepository.findByUsername("admin")).thenReturn(null);
        when(passwordEncoder.encode(STRONG)).thenReturn("hash");

        initializer.run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User admin = saved.getValue();
        assertEquals("admin", admin.getUsername());
        assertEquals("hash", admin.getPassword());
        assertTrue(admin.isActive());
        assertEquals(EnumSet.of(Role.USER, Role.ADMIN), admin.getRoles());
        assertNotNull(admin.getEmail(), "email is @NotBlank, a placeholder is required");
        assertFalse(admin.getEmail().isEmpty());
    }

    @Test
    void activeAdminExists_isLeftUntouched_soProfilePasswordChangesSurviveRestart() {
        password(STRONG);
        User existing = new User();
        existing.setUsername("admin");
        existing.setPassword("changed-by-admin");
        existing.setActive(true);
        existing.setRoles(EnumSet.of(Role.USER, Role.ADMIN));
        when(userRepository.findByUsername("admin")).thenReturn(existing);

        initializer.run(null);

        assertEquals("changed-by-admin", existing.getPassword());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void disabledAdminExists_isReenabledWithNewPasswordAndRoles() {
        password(STRONG);
        User existing = new User();
        existing.setUsername("admin");
        existing.setPassword("old-hash");
        existing.setActive(false);
        existing.setEmail("admin@example.org");
        existing.setRoles(EnumSet.of(Role.USER));
        when(userRepository.findByUsername("admin")).thenReturn(existing);
        when(passwordEncoder.encode(STRONG)).thenReturn("new-hash");

        initializer.run(null);

        assertTrue(existing.isActive());
        assertEquals("new-hash", existing.getPassword());
        assertEquals(EnumSet.of(Role.USER, Role.ADMIN), existing.getRoles());
        assertEquals("admin@example.org", existing.getEmail(), "real email must not be overwritten");
        verify(userRepository).save(existing);
    }

    @Test
    void disabledAdminWithoutEmail_getsPlaceholderEmail() {
        password(STRONG);
        User existing = new User();
        existing.setUsername("admin");
        existing.setActive(false);
        existing.setRoles(EnumSet.of(Role.USER));
        when(userRepository.findByUsername("admin")).thenReturn(existing);
        when(passwordEncoder.encode(STRONG)).thenReturn("new-hash");

        initializer.run(null);

        assertNotNull(existing.getEmail());
        assertFalse(existing.getEmail().trim().isEmpty());
        verify(userRepository).save(existing);
    }
}
