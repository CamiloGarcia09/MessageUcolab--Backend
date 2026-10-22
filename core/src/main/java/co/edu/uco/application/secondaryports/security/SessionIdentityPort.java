package co.edu.uco.application.secondaryports.security;

import co.edu.uco.application.usecase.domain.security.SessionIdentity;

public interface SessionIdentityPort {
    SessionIdentity resolve(String session);
}
