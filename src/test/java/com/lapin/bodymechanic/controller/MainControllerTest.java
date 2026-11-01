package com.lapin.bodymechanic.controller;

import com.lapin.bodymechanic.domain.Message;
import com.lapin.bodymechanic.domain.User;
import com.lapin.bodymechanic.repository.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MainController controller;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "uploadPath", System.getProperty("java.io.tmpdir"));

        user = new User();
        user.setId(1L);
        user.setUsername("bob");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/", ".jsp"))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void main_withFilter_showsOnlyMessagesWithTag() throws Exception {
        List<Message> tagged = Collections.singletonList(new Message("a", "leg", user));
        when(messageRepository.findByTag("leg")).thenReturn(tagged);

        mockMvc.perform(get("/main").param("filter", "leg"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("messages", tagged))
                .andExpect(model().attribute("filter", "leg"));
    }

    @Test
    void add_keepsActiveFilterAfterSaving() throws Exception {
        List<Message> tagged = Collections.singletonList(new Message("a", "leg", user));
        when(messageRepository.findByTag("leg")).thenReturn(tagged);

        mockMvc.perform(multipart("/main")
                        .file(new MockMultipartFile("file", "", "application/octet-stream", new byte[0]))
                        .param("text", "squat day")
                        .param("tag", "leg")
                        .param("filter", "leg"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("messages", tagged))
                .andExpect(model().attribute("filter", "leg"));

        verify(messageRepository).save(any(Message.class));
        verify(messageRepository, never()).findAll();
    }

    @Test
    void add_withoutFilter_showsAllMessages() throws Exception {
        List<Message> all = Collections.singletonList(new Message("a", "leg", user));
        when(messageRepository.findAll()).thenReturn(all);

        mockMvc.perform(multipart("/main")
                        .file(new MockMultipartFile("file", "", "application/octet-stream", new byte[0]))
                        .param("text", "squat day")
                        .param("tag", "leg"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("messages", all));
    }
}
