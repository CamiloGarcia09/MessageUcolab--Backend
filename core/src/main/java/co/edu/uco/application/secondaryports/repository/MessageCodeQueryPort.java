package co.edu.uco.application.secondaryports.repository;

public interface MessageCodeQueryPort {

    boolean existsByCodeAndApplicationId(String code, String applicationId);

    default java.util.Set<String> findExistingCodes(java.util.Set<String> codes, String applicationId) {
        return codes.stream().filter(code -> existsByCodeAndApplicationId(code, applicationId))
                .collect(java.util.stream.Collectors.toSet());
    }
}
