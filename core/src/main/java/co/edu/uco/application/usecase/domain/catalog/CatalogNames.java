package co.edu.uco.application.usecase.domain.catalog;

import static co.edu.uco.application.CrosswordsConstant.ENVIRONMENT_PRODUCTION;
import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE;
import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE_LEGACY;
import static co.edu.uco.application.CrosswordsConstant.STATE_INACTIVE;

/** Recognizes catalog names during the non-destructive transition to Spanish. */
public final class CatalogNames {
    private CatalogNames() { }

    public static boolean isActive(String name) {
        return STATE_ACTIVE.equalsIgnoreCase(name) || STATE_ACTIVE_LEGACY.equalsIgnoreCase(name);
    }

    public static boolean isInactive(String name) {
        return STATE_INACTIVE.equalsIgnoreCase(name) || "Inactive".equalsIgnoreCase(name);
    }

    public static boolean isProduction(String name) {
        return ENVIRONMENT_PRODUCTION.equalsIgnoreCase(name) || "Producción".equalsIgnoreCase(name)
                || "Production".equalsIgnoreCase(name);
    }

    public static boolean sameEnvironmentType(String expected, String actual) {
        return expected != null && actual != null
                && (expected.equalsIgnoreCase(actual) || (isProduction(expected) && isProduction(actual)));
    }
}
