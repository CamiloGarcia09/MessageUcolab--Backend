package co.edu.uco.application.secondaryports.event;

public interface Publisher<T> {
    void publish(T event);
}
