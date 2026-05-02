package ru.vinyl.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vinyl.service.CatalogService;
import ru.vinyl.service.ValidationService;

@Controller
public class HomeController {

    private final CatalogService catalogService;
    private final ValidationService validationService;

    public HomeController(CatalogService catalogService, ValidationService validationService) {
        this.catalogService = catalogService;
        this.validationService = validationService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(value = "q", defaultValue = "") String searchQuery,
            @RequestParam(value = "page", defaultValue = "1") String pageValue,
            Model model
    ) {
        int page = validationService.toInt(pageValue, 1, 1);
        CatalogService.HomePageData data = catalogService.getHomePage(searchQuery, page);
        model.addAttribute("featured", data.featured());
        model.addAttribute("saleItems", data.saleItems());
        model.addAttribute("genres", data.genres());
        model.addAttribute("products", data.products());
        model.addAttribute("totalPages", data.totalPages());
        model.addAttribute("page", data.page());
        model.addAttribute("searchQuery", data.searchQuery());
        return "home";
    }
}
