package co.edu.uco.application.secondaryports.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReferenceCatalogTest {

    @ParameterizedTest
    @EnumSource(ReferenceCatalog.class)
    void everyConstant_hasNonEmptyTable(ReferenceCatalog referenceCatalog) {
        assertFalse(referenceCatalog.getTable().isEmpty());
    }

    @Test
    void tableNames_areUniqueAcrossConstants() {
        long distinctTables = Arrays.stream(ReferenceCatalog.values())
                .map(ReferenceCatalog::getTable)
                .distinct()
                .count();

        assertEquals(ReferenceCatalog.values().length, distinctTables);
    }

    @Test
    void messageCatalogs_mapToPersistentMessageTables() {
        assertAll(
                () -> assertEquals("message_type", ReferenceCatalog.MESSAGE_TYPE.getTable()),
                () -> assertEquals("message_category", ReferenceCatalog.MESSAGE_CATEGORY.getTable()),
                () -> assertEquals("message_state", ReferenceCatalog.MESSAGE_STATE.getTable()),
                () -> assertEquals("message_environment_state",
                        ReferenceCatalog.MESSAGE_ENVIRONMENT_STATE.getTable()));
    }
}