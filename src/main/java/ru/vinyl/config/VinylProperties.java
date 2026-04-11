package ru.vinyl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vinyl")
public class VinylProperties {

    private String baseUrl = "http://127.0.0.1:8080";
    private String mailFrom = "noreply@vinylstore.local";
    private int loginAttemptLimit = 5;
    private int loginBlockMinutes = 15;
    private int emailTokenTtlHours = 48;
    private int resetTokenTtlMinutes = 30;
    private boolean requireEmailConfirmation = true;
    private boolean mailConsoleOnly = false;
    private Admin admin = new Admin();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public int getLoginAttemptLimit() {
        return loginAttemptLimit;
    }

    public void setLoginAttemptLimit(int loginAttemptLimit) {
        this.loginAttemptLimit = loginAttemptLimit;
    }

    public int getLoginBlockMinutes() {
        return loginBlockMinutes;
    }

    public void setLoginBlockMinutes(int loginBlockMinutes) {
        this.loginBlockMinutes = loginBlockMinutes;
    }

    public int getEmailTokenTtlHours() {
        return emailTokenTtlHours;
    }

    public void setEmailTokenTtlHours(int emailTokenTtlHours) {
        this.emailTokenTtlHours = emailTokenTtlHours;
    }

    public int getResetTokenTtlMinutes() {
        return resetTokenTtlMinutes;
    }

    public void setResetTokenTtlMinutes(int resetTokenTtlMinutes) {
        this.resetTokenTtlMinutes = resetTokenTtlMinutes;
    }

    public boolean isRequireEmailConfirmation() {
        return requireEmailConfirmation;
    }

    public void setRequireEmailConfirmation(boolean requireEmailConfirmation) {
        this.requireEmailConfirmation = requireEmailConfirmation;
    }

    public boolean isMailConsoleOnly() {
        return mailConsoleOnly;
    }

    public void setMailConsoleOnly(boolean mailConsoleOnly) {
        this.mailConsoleOnly = mailConsoleOnly;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public static class Admin {
        private String email = "admin@vinylstore.local";
        private String password = "Admin12345";
        private String firstName = "Главный";
        private String lastName = "Администратор";

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
