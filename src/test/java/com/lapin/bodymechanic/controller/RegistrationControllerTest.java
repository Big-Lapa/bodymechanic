package com.lapin.bodymechanic.controller;

import com.lapin.bodymechanic.domain.User;
import com.lapin.bodymechanic.domain.dto.CaptchaResponseDto;
import com.lapin.bodymechanic.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RegistrationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "secret", "test-secret");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/", ".jsp"))
                .build();
    }

    private void captchaReturns(CaptchaResponseDto dto) {
        when(restTemplate.postForObject(anyString(), any(), eq(CaptchaResponseDto.class))).thenReturn(dto);
    }

    @Test
    void register_captchaServiceReturnsNothing_showsCaptchaErrorInsteadOfFailing() throws Exception {
        captchaReturns(null);

        mockMvc.perform(post("/registration")
                        .param("username", "bob")
                        .param("password", "pw")
                        .param("password2", "pw")
                        .param("email", "bob@example.org")
                        .param("g-recaptcha-response", "token"))
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().attributeExists("captchaError"));

        verify(userService, never()).addUser(any(User.class));
    }

    @Test
    void register_captchaIsVerifiedWithFormEncodedBody_notQueryString() throws Exception {
        CaptchaResponseDto dto = new CaptchaResponseDto();
        dto.setSuccess(true);
        captchaReturns(dto);
        when(userService.addUser(any(User.class))).thenReturn(true);

        mockMvc.perform(post("/registration")
                .param("username", "bob")
                .param("password", "pw")
                .param("password2", "pw")
                .param("email", "bob@example.org")
                .param("g-recaptcha-response", "tok&en=+1"));

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> request = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForObject(url.capture(), request.capture(), eq(CaptchaResponseDto.class));

        assertEquals("https://www.google.com/recaptcha/api/siteverify", url.getValue(),
                "secret and token must not be put into the URL");
        assertTrue(request.getValue() instanceof HttpEntity);
        HttpEntity<?> entity = (HttpEntity<?>) request.getValue();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, entity.getHeaders().getContentType());
        MultiValueMap<?, ?> body = (MultiValueMap<?, ?>) entity.getBody();
        assertEquals(Collections.singletonList("test-secret"), body.get("secret"));
        assertEquals(Collections.singletonList("tok&en=+1"), body.get("response"));
    }

    @Test
    void register_captchaFailed_doesNotCreateUser() throws Exception {
        CaptchaResponseDto dto = new CaptchaResponseDto();
        dto.setSuccess(false);
        captchaReturns(dto);

        mockMvc.perform(post("/registration")
                        .param("username", "bob")
                        .param("password", "pw")
                        .param("password2", "pw")
                        .param("email", "bob@example.org")
                        .param("g-recaptcha-response", "token"))
                .andExpect(view().name("registration"))
                .andExpect(model().attributeExists("captchaError"));

        verify(userService, never()).addUser(any(User.class));
    }

    @Test
    void register_valid_createsUserAndRedirectsToLogin() throws Exception {
        CaptchaResponseDto dto = new CaptchaResponseDto();
        dto.setSuccess(true);
        dto.setErrorCodes(Collections.emptySet());
        captchaReturns(dto);
        when(userService.addUser(any(User.class))).thenReturn(true);

        mockMvc.perform(post("/registration")
                        .param("username", "bob")
                        .param("password", "pw")
                        .param("password2", "pw")
                        .param("email", "bob@example.org")
                        .param("g-recaptcha-response", "token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void register_passwordsDiffer_doesNotCreateUser() throws Exception {
        CaptchaResponseDto dto = new CaptchaResponseDto();
        dto.setSuccess(true);
        captchaReturns(dto);

        mockMvc.perform(post("/registration")
                        .param("username", "bob")
                        .param("password", "pw")
                        .param("password2", "other")
                        .param("email", "bob@example.org")
                        .param("g-recaptcha-response", "token"))
                .andExpect(view().name("registration"))
                .andExpect(model().attributeExists("passwordError"));

        verify(userService, never()).addUser(any(User.class));
    }
}
