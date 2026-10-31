package co.edu.uco.application.secondaryports.broker;

import co.edu.uco.application.usecase.domain.MessageCodeDomain;
import co.edu.uco.application.secondaryports.GenericPort;
public interface SendMessage extends GenericPort<MessageCodeDomain, Void> {
}