package co.edu.uco.application.secondaryports;

public interface GenericPort <T, R> {
    void execute(T dto, R response);
}
