package ru.vinyl.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorPageController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute("jakarta.servlet.error.status_code");
        int status = statusCode == null ? 500 : Integer.parseInt(statusCode.toString());
        if (status == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("title", "Страница не найдена");
            model.addAttribute("message", "Запрошенная страница отсутствует.");
        } else {
            model.addAttribute("title", "Ошибка сервера");
            model.addAttribute("message", "Во время обработки запроса произошла ошибка.");
        }
        return "error";
    }
}
