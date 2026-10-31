package co.edu.uco.application.secondaryports.presenter;


import java.util.List;

public interface PresenterPort<T> {
    RenderedResponse presentRestSuccess(List<T> dto, String acceptHeader);
}