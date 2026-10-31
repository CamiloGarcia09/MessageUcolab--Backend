package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogReferenceExistsSpecificationTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String MALFORMED_UUID = "not-a-uuid";

    @Mock
    private RecordExistsCatalogPort recordExistsCatalogPort;

    private CatalogReferenceExistsSpecification specification() {
        return new CatalogReferenceExistsSpecification(recordExistsCatalogPort, ReferenceCatalog.LANGUAGE_BASE);
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenReferenceExists() {
        when(recordExistsCatalogPort.exists(ReferenceCatalog.LANGUAGE_BASE, VALID_UUID)).thenReturn(true);

        assertTrue(specification().isSatisfiedBy(VALID_UUID));

        verify(recordExistsCatalogPort).exists(ReferenceCatalog.LANGUAGE_BASE, VALID_UUID);
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenReferenceDoesNotExist() {
        when(recordExistsCatalogPort.exists(ReferenceCatalog.LANGUAGE_BASE, VALID_UUID)).thenReturn(false);

        assertFalse(specification().isSatisfiedBy(VALID_UUID));

        verify(recordExistsCatalogPort).exists(ReferenceCatalog.LANGUAGE_BASE, VALID_UUID);
    }

    @Test
    void isSatisfiedBy_returnsFalseWithoutRepositoryLookup_whenReferenceIsMalformedUuid() {
        assertFalse(specification().isSatisfiedBy(MALFORMED_UUID));

        verifyNoInteractions(recordExistsCatalogPort);
    }

    @Test
    void isSatisfiedBy_returnsFalseWithoutRepositoryLookup_whenReferenceIsDefaultUuid() {
        assertFalse(specification().isSatisfiedBy(DEFAULT_UUID));

        verifyNoInteractions(recordExistsCatalogPort);
    }

    @Test
    void isSatisfiedBy_returnsFalseWithoutRepositoryLookup_whenReferenceIsNull() {
        assertFalse(specification().isSatisfiedBy(null));

        verifyNoInteractions(recordExistsCatalogPort);
    }

    @Test
    void isSatisfiedBy_returnsFalseWithoutRepositoryLookup_whenReferenceIsEmpty() {
        assertFalse(specification().isSatisfiedBy("   "));

        verifyNoInteractions(recordExistsCatalogPort);
    }
}
