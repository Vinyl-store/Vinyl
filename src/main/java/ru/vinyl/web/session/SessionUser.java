package ru.vinyl.web.session;

import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;

public class SessionUser {
    private final long id;
    private final UserRole role;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final boolean emailConfirmed;
    private final boolean blocked;

    public SessionUser(User user) {
        this.id = user.getId();
        this.role = user.getRole();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.emailConfirmed = user.isEmailConfirmed();
        this.blocked = user.isBlocked();
    }

    public long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isEmailConfirmed() {
        return emailConfirmed;
    }

    public boolean isBlocked() {
        return blocked;
    }
}
