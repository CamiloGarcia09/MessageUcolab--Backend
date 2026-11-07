package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

/** Rechecks the validated reference inside the write transaction to avoid a stale validation. */
final class SurrealFunctionalityWriteGuard {
    private SurrealFunctionalityWriteGuard() { }

    static String requireActive(String functionalityId, String applicationId, String errorMessage) {
        return "IF array::len((SELECT id FROM " + recordIdLiteral("functionality", functionalityId)
                + " WHERE application_id = " + recordIdLiteral("application", applicationId)
                + " AND string::lowercase(state_id.name) IN ['activo', 'active'])) != 1 { THROW "
                + quote(errorMessage) + "; };";
    }

    static String requireActiveForStatus(String functionalityId, String applicationId, String statusId,
                                          String errorMessage) {
        return "IF string::lowercase(" + recordIdLiteral("message_state", statusId)
                + ".name) IN ['activo', 'active'] { "
                + requireActive(functionalityId, applicationId, errorMessage) + " };";
    }
}
