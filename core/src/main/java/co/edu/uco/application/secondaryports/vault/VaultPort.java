package co.edu.uco.application.secondaryports.vault;

public interface VaultPort {

    String getSecretValue(String secretName);

}