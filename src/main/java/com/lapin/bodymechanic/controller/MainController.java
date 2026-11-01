package com.lapin.bodymechanic.controller;

import com.lapin.bodymechanic.domain.Message;
import com.lapin.bodymechanic.domain.User;
import com.lapin.bodymechanic.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
public class MainController {

    private final MessageRepository messageRepository;

    @Value("${upload.path}")
    private String uploadPath;

    public MainController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping("/")
    public String greeting() {
        return "greeting";
    }

    @GetMapping("/main")
    public String main(@RequestParam(required = false, defaultValue = "") String filter, Model model) {
        addMessages(model, filter);

        return "main";
    }

    @PostMapping("/main")
    public String add(
            @AuthenticationPrincipal User user,
            @Valid Message message,
            BindingResult bindingResult,
            Model model,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "") String filter
    ) throws IOException {
        message.setAuthor(user);

        if (bindingResult.hasErrors()) {
            Map<String, String> errorsMap = ControllerUtils.getErrors(bindingResult);
            model.mergeAttributes(errorsMap);
            model.addAttribute("message", message);

        } else {
            saveFile(message, file);
            model.addAttribute("message", null);

            messageRepository.save(message);
        }

        addMessages(model, filter);

        return "main";
    }

    private void addMessages(Model model, String filter) {
        Iterable<Message> messages = !StringUtils.hasLength(filter)
                ? messageRepository.findAll()
                : messageRepository.findByTag(filter);

        model.addAttribute("messages", messages);
        model.addAttribute("filter", filter);
    }

    private void saveFile(Message message, MultipartFile file) throws IOException {
        if (file != null && StringUtils.hasLength(file.getOriginalFilename())) {
            File uploadDir = new File(uploadPath);

            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // keep only the base name: never trust a path sent by the client
            String originalName = StringUtils.getFilename(StringUtils.cleanPath(file.getOriginalFilename()));
            String resultFilename = UUID.randomUUID() + "." + originalName;

            file.transferTo(new File(uploadDir, resultFilename).getAbsoluteFile());

            message.setFilename(resultFilename);
        }
    }

    @GetMapping("/user-messages/{user}")
    public String userMessages(
            @AuthenticationPrincipal User currentUser,
            @PathVariable User user,
            Model model,
            @RequestParam(required = false) Message message
    ) {
        Set<Message> messages = user.getMessages();

        model.addAttribute("messages", messages);
        model.addAttribute("message", message);
        model.addAttribute("isCurrentUser", currentUser.equals(user));

        return "userMessages";
    }

    @PostMapping("/user-messages/{user}")
    public String updateMessage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long user,
            @RequestParam("id") Message message,
            @RequestParam("text") String text,
            @RequestParam("tag") String tag,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (message.getAuthor().equals(currentUser)) {
            if (StringUtils.hasLength(text)) {
                message.setText(text);
            }

            if (StringUtils.hasLength(tag)) {
                message.setTag(tag);
            }

            saveFile(message, file);

            messageRepository.save(message);
        }

        return "redirect:/user-messages/" + user;
    }
}
