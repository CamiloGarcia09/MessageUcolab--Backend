package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.usecase.domain.message.importing.*;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageImportSurrealAdapterTest {
    private static final String APP = "123e4567-e89b-12d3-a456-426614175106";
    private static final String ENV = "123e4567-e89b-12d3-a456-426614175107";
    private static final String FUNCTION = "123e4567-e89b-12d3-a456-426614175108";
    private static final String TYPE = "123e4567-e89b-12d3-a456-426614175109";
    private static final String CATEGORY = "123e4567-e89b-12d3-a456-426614175110";
    private static final String STATE = "123e4567-e89b-12d3-a456-426614175111";
    private static final String ENV_STATE = "123e4567-e89b-12d3-a456-426614175112";
    @Mock private Surreal surreal;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private LoggingPort logger;
    @Mock private ObjectMapper json;
    @Mock private CatalogPort catalog;
    private MessageImportSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(MessageImportSurrealAdapter.class)).thenReturn(logger);
        when(catalog.getMessage(anyString())).thenReturn("Import failed");
        adapter = new MessageImportSurrealAdapter(surreal, loggerFactory, json, catalog);
    }

    @Test
    void createAtomically_issuesSingleGuardedTransactionAndReturnsDurableReceipt() throws Exception {
        var preview = preview();
        var receipt = receipt(preview.importId());
        var transactionResponse = resultResponse();
        var durableResponse = receiptResponse("receipt-json");
        when(json.writeValueAsString(any(MessageImportReceipt.class))).thenReturn("receipt-json");
        when(json.readValue("receipt-json", MessageImportReceipt.class)).thenReturn(receipt);
        when(surreal.query(anyString())).thenReturn(transactionResponse, durableResponse);

        var result = adapter.createAtomically(preview, ENV_STATE);

        assertThat(result).isSameAs(receipt);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(sql.capture());
        assertThat(sql.getAllValues().getFirst()).startsWith("BEGIN TRANSACTION;")
                .contains("record::exists(message_environment_state:")
                .contains("record::exists(message_type:")
                .contains("CREATE ONLY message:")
                .contains("CREATE ONLY message_environment:")
                .contains("CREATE ONLY message_import_receipt:")
                .contains("COMMIT TRANSACTION;")
                .doesNotContain("UPSERT");
    }

    @Test
    void createAtomically_consumesEveryDriverResultBeforeCheckingReceipt() throws Exception {
        var response = resultResponse();
        when(response.size()).thenReturn(2);
        var durableResponse = receiptResponse("receipt-json");
        when(json.writeValueAsString(any(MessageImportReceipt.class))).thenReturn("receipt-json");
        when(json.readValue("receipt-json", MessageImportReceipt.class)).thenReturn(receipt("import-id"));
        when(surreal.query(anyString())).thenReturn(response, durableResponse);

        adapter.createAtomically(preview(), ENV_STATE);

        verify(response).take(0);
        verify(response).take(1);
    }

    @Test
    void createAtomically_recoversCommittedReceiptAfterDriverThrows() throws Exception {
        var receipt = receipt("import-id");
        doThrow(new IllegalStateException("driver failed after commit")).when(surreal).query(anyString());
        doReturn(receiptResponse("receipt-json")).when(surreal).query(containsReceiptLookup());
        when(json.writeValueAsString(any(MessageImportReceipt.class))).thenReturn("receipt-json");
        when(json.readValue("receipt-json", MessageImportReceipt.class)).thenReturn(receipt);

        assertThat(adapter.createAtomically(preview(), ENV_STATE)).isSameAs(receipt);
    }

    @Test
    void createAtomically_raisesConflictWhenDriverFailsAndNoReceiptExists() throws Exception {
        doThrow(new IllegalStateException("transaction failed")).when(surreal).query(anyString());
        doReturn(emptyResponse()).when(surreal).query(containsReceiptLookup());
        when(json.writeValueAsString(any(MessageImportReceipt.class))).thenReturn("receipt-json");

        assertThatThrownBy(() -> adapter.createAtomically(preview(), ENV_STATE))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createAtomically_failsIfSuccessfulQueryDoesNotProduceReceipt() throws Exception {
        var transactionResponse = resultResponse();
        var emptyResponse = emptyResponse();
        when(json.writeValueAsString(any(MessageImportReceipt.class))).thenReturn("receipt-json");
        when(surreal.query(anyString())).thenReturn(transactionResponse, emptyResponse);

        assertThatThrownBy(() -> adapter.createAtomically(preview(), ENV_STATE))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createAtomically_wrapsReceiptSerializationFailureBeforeSendingTransaction() throws Exception {
        when(json.writeValueAsString(any(MessageImportReceipt.class)))
                .thenThrow(new JsonProcessingException("cannot serialize") { });

        assertThatThrownBy(() -> adapter.createAtomically(preview(), ENV_STATE))
                .isInstanceOf(BusinessException.class);
        verify(surreal, never()).query(anyString());
    }

    @Test
    void findReceipt_buildsApplicationScopedLookupAndDeserializesReceipt() throws Exception {
        var receipt = receipt("import-id");
        var response = receiptResponse("receipt-json");
        when(surreal.query(anyString())).thenReturn(response);
        when(json.readValue("receipt-json", MessageImportReceipt.class)).thenReturn(receipt);

        assertThat(adapter.findReceipt("import-id", APP)).containsSame(receipt);
        var query = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(query.capture());
        assertThat(query.getValue()).contains("message_import_receipt:", "application:`" + APP + "`");
    }

    private static String containsReceiptLookup() {
        return "SELECT receipt_json FROM message_import_receipt:`import-id` WHERE application_id = application:`" + APP + "`;";
    }

    private static Response resultResponse() {
        var response = org.mockito.Mockito.mock(Response.class);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(org.mockito.Mockito.mock(Value.class));
        return response;
    }

    private static Response emptyResponse() {
        var response = org.mockito.Mockito.mock(Response.class);
        when(response.size()).thenReturn(0);
        return response;
    }

    private static Response receiptResponse(String json) {
        var response = org.mockito.Mockito.mock(Response.class);
        when(response.size()).thenReturn(1);
        var statement = org.mockito.Mockito.mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        var array = org.mockito.Mockito.mock(com.surrealdb.Array.class);
        when(array.len()).thenReturn(1);
        var value = org.mockito.Mockito.mock(Value.class);
        when(value.isObject()).thenReturn(true);
        var object = org.mockito.Mockito.mock(com.surrealdb.Object.class);
        var jsonValue = org.mockito.Mockito.mock(Value.class);
        when(jsonValue.isNull()).thenReturn(false);
        when(jsonValue.isNone()).thenReturn(false);
        when(jsonValue.isString()).thenReturn(true);
        when(jsonValue.getString()).thenReturn(json);
        when(object.get("receipt_json")).thenReturn(jsonValue);
        when(value.getObject()).thenReturn(object);
        when(array.get(0)).thenReturn(value);
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private static MessageImportPreview preview() {
        var dto = CreateMessageDTO.builder().code("CODE").title("A valid title").content("A valid content")
                .functionalityId(FUNCTION).typeId(TYPE).categoryId(CATEGORY).statusId(STATE).build();
        var row = new ImportRow(2, "CODE", FUNCTION, TYPE, CATEGORY, STATE, "A valid title", "A valid content");
        return new MessageImportPreview("import-id", APP, List.of(ENV),
                List.of(new ImportRowResult(row, "MSG_CODE", dto, List.of())), Instant.now().plusSeconds(300));
    }

    private static MessageImportReceipt receipt(String importId) {
        return new MessageImportReceipt(importId, 1, List.of(ENV), Instant.now());
    }
}
