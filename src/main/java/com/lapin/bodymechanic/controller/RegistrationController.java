package com.lapin.bodymechanic.controller;

import com.lapin.bodymechanic.domain.User;
import com.lapin.bodymechanic.domain.dto.CaptchaResponseDto;
import com.lapin.bodymechanic.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import java.util.Map;

@Controller
public class RegistrationController {

    private static final String CAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final UserService userService;
    private final RestTemplate restTemplate;

    @Value("${recaptcha.secret}")
    private String secret;

    public RegistrationController(UserService userService, RestTemplate restTemplate) {
        this.userService = userService;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("message", "");
        return "registration";
    }

    @PostMapping("/registration")
    public String addUser(
            @RequestParam("password2") String passwordConfirm,
            @RequestParam("g-recaptcha-response") String captchaResponse,
            @Valid User user,
            BindingResult bindingResult,
            Model model) {

        boolean isCaptchaValid = isCaptchaValid(captchaResponse);

        if (!isCaptchaValid) {
            model.addAttribute("captchaError", "Fill captcha");
        }

        boolean isConfirmEmpty = !StringUtils.hasLength(passwordConfirm);

        if (isConfirmEmpty) {
            model.addAttribute("password2Error", "Password confirmation cannot be empty");
        }

        if (user.getPassword() != null && !user.getPassword().equals(passwordConfirm)) {
            model.addAttribute("passwordError", "Password are different!");
        }

        if (isConfirmEmpty || bindingResult.hasErrors() || !isCaptchaValid
                || model.containsAttribute("passwordError")) {
            Map<String, String> errors = ControllerUtils.getErrors(bindingResult);

            model.mergeAttributes(errors);

            return "registration";
        }
        if (!userService.addUser(user)) {
            model.addAttribute("usernameError", "User exists!");
            return "registration";
        }

        return "redirect:/login";
    }

    private boolean isCaptchaValid(String captchaResponse) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", captchaResponse);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            CaptchaResponseDto response = restTemplate.postForObject(
                    CAPTCHA_URL, new HttpEntity<>(form, headers), CaptchaResponseDto.class);
            return response != null && response.isSuccess();
        } catch (RestClientException e) {
            return false;
        }
    }

    @GetMapping("/activate/{code}")
    public String activate(Model model, @PathVariable String code) {
        boolean isActivated = userService.activateUser(code);

        if (isActivated) {
            model.addAttribute("messageType", "success");
            model.addAttribute("message", "User successfully activated");
        } else {
            model.addAttribute("messageType", "danger");
            model.addAttribute("message", "Activation code is not found!");
        }

        return "login";
    }
}
