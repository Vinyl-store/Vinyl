package ru.vinyl.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class FlashModelAdvice {

    @ModelAttribute("flashMessages")
    public List<FlashMessage> flashMessages(HttpSession session) {
        return FlashMessages.consume(session);
    }
}
