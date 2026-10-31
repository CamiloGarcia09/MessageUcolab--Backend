package co.edu.uco.application.usecase.domain.message.importing;

public record ImportReference(String id, String name, boolean available) {
    public boolean matches(String value) {
        return !value.isBlank() && name.equals(value);
    }
}
