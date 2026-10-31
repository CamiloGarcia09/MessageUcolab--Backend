package co.edu.uco.application.primaryports.facade;

public interface UseCaseFacade<T, R> {
    void execute(T dto, R response);
}
