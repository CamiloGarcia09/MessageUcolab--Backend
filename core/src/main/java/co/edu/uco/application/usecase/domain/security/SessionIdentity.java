package co.edu.uco.application.usecase.domain.security;

public record SessionIdentity(String source, String tenantId, String subject, String email, String name) {
}
