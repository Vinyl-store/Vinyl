package ru.vinyl.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.vinyl.web.interceptor.AuthInterceptor;
import ru.vinyl.web.interceptor.NavigationInterceptor;

@Configuration
@EnableConfigurationProperties(VinylProperties.class)
public class AppConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final NavigationInterceptor navigationInterceptor;

    public AppConfig(AuthInterceptor authInterceptor, NavigationInterceptor navigationInterceptor) {
        this.authInterceptor = authInterceptor;
        this.navigationInterceptor = navigationInterceptor;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(navigationInterceptor);
        registry.addInterceptor(authInterceptor);
    }
}
