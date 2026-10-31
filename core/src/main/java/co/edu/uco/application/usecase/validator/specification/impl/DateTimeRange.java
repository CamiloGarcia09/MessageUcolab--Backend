package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import java.time.LocalDateTime;

/**
 * Valor de dominio inmutable que agrupa una fecha de inicio y una fecha de fin.
 */
public record DateTimeRange(LocalDateTime startDate, LocalDateTime endDate) {

    public boolean startIsAfterEnd() {
        return startDate.isAfter(endDate);
    }
}