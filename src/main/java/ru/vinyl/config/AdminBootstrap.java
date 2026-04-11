package ru.vinyl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.vinyl.service.AdminBootstrapService;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminBootstrapService adminBootstrapService;

    public AdminBootstrap(AdminBootstrapService adminBootstrapService) {
        this.adminBootstrapService = adminBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            adminBootstrapService.ensureAdminAccount();
            adminBootstrapService.ensureBuyerProfiles();
        } catch (Exception ex) {
            log.error("Не удалось инициализировать администратора", ex);
        }
    }
}
