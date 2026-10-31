package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.usecase.MessageImportUseCase;
import co.edu.uco.application.usecase.domain.message.importing.ImportReferences;
import co.edu.uco.application.usecase.domain.message.importing.MessageImportPreview;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.infraestructure.primaryadapters.interceptors.AcceptHeaderInterceptor;
import co.edu.uco.infraestructure.primaryadapters.importing.MessageImportWorkbookAdapter;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageImportDownloadHttpRegressionTest {
    private static final String APP_ID = "123e4567-e89b-12d3-a456-426614175106";
    private static final UUID IMPORT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175107");
    private static final String BASE_PATH = "/messageucolab/v1/messages/imports";
    private static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String MIXED_ACCEPT = EXCEL + ", application/json";
    private static final byte[] XLSX_BYTES = {80, 75, 3, 4, 1, 2, 3};

    @Mock private MessageImportUseCase imports;
    @Mock private MessageImportWorkbookAdapter workbook;
    @Mock private CatalogPort catalog;
    @Mock private SerializerRegistry registry;
    @Mock private LoggingPortFactory loggingFactory;
    @Mock private co.edu.uco.application.secondaryports.logging.LoggingPort logger;
    @Mock private SerializerType serializer;

    private AnnotationConfigApplicationContext context;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        lenient().when(loggingFactory.getLogger(AcceptHeaderInterceptor.class)).thenReturn(logger);
        lenient().when(registry.getSerializerForMediaType(anyString())).thenReturn(serializer);
        lenient().when(serializer.supports(anyString())).thenReturn(false);
        lenient().when(serializer.supports(MediaType.APPLICATION_JSON_VALUE)).thenReturn(true);
        lenient().when(serializer.getSupportedContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        lenient().when(catalog.getMessage("TCH_023")).thenReturn("Unsupported response format");
        lenient().when(imports.references(any(MessageAccessContext.class))).thenReturn(new ImportReferences(
                List.of(), List.of(), List.of(), List.of()));
        lenient().when(workbook.template(any(ImportReferences.class), eq(APP_ID))).thenReturn(XLSX_BYTES);
        var preview = new MessageImportPreview("preview-id", APP_ID, List.of(), List.of(), Instant.now().plusSeconds(60));
        lenient().when(imports.preview(eq(IMPORT_ID.toString()), any(MessageAccessContext.class))).thenReturn(preview);
        lenient().when(workbook.report(preview)).thenReturn(XLSX_BYTES);
        lenient().when(imports.result(eq(IMPORT_ID.toString()), any(MessageAccessContext.class)))
                .thenReturn(Optional.empty());

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "message-import-http-test", java.util.Map.of(
                        "crosswords.api.path.messages", "/messageucolab/v1/messages")));
        context.registerBean(MessageImportUseCase.class, () -> imports);
        context.registerBean(MessageImportWorkbookAdapter.class, () -> workbook);
        context.registerBean(CatalogPort.class, () -> catalog);
        context.registerBean(SerializerRegistry.class, () -> registry);
        context.registerBean(LoggingPortFactory.class, () -> loggingFactory);
        context.registerBean(MessageImportController.class,
                () -> new MessageImportController(imports, workbook, catalog));
        context.registerBean(AcceptHeaderInterceptor.class,
                () -> new AcceptHeaderInterceptor(registry, catalog, loggingFactory));
        context.registerBean(MessageImportWebConfiguration.class);
        context.refresh();
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void template_mixedFrontendAcceptNegotiatesExcelDownloadThroughFilterAndHandlerMapping() {
        assertExcelDownload(BASE_PATH + "/template", MIXED_ACCEPT, "plantilla-mensajes.xlsx");

        verify(imports).references(new MessageAccessContext(APP_ID));
        verify(workbook).template(any(ImportReferences.class), eq(APP_ID));
        verify(registry, never()).getSerializerForMediaType(anyString());
    }

    @Test
    void report_mixedFrontendAcceptNegotiatesExcelDownloadThroughFilterAndHandlerMapping() {
        assertExcelDownload(BASE_PATH + "/" + IMPORT_ID + "/errors", MIXED_ACCEPT, "errores-importacion.xlsx");

        verify(imports).preview(IMPORT_ID.toString(), new MessageAccessContext(APP_ID));
        verify(workbook).report(any(MessageImportPreview.class));
        verify(registry, never()).getSerializerForMediaType(anyString());
    }

    @ParameterizedTest
    @MethodSource("downloadPathsAndSupportedAcceptHeaders")
    void downloads_acceptExcelAndWildcardUsingNativeNegotiation(String path, String accept) {
        assertExcelDownload(path, accept, path.endsWith("/template")
                ? "plantilla-mensajes.xlsx" : "errores-importacion.xlsx");
        verify(registry, never()).getSerializerForMediaType(anyString());
    }

    @ParameterizedTest
    @MethodSource("downloadPaths")
    void excelOnlyRoute_rejectsJsonOnlyAcceptWithNative406(String path) {
        client.get().uri(path).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchange().expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        verify(registry, never()).getSerializerForMediaType(anyString());
    }

    @ParameterizedTest
    @MethodSource("pathsOutsideNarrowExemption")
    void nonmatchingPathOrMethod_remainsSubjectToSerializerFilter(String method, String path) {
        client.method(HttpMethod.valueOf(method)).uri(path).header(HttpHeaders.ACCEPT, "application/pdf")
                .exchange().expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        verify(registry).getSerializerForMediaType("application/pdf");
        verify(serializer).supports("application/pdf");
    }

    @Test
    void jsonEndpoint_remainsUnderExistingSerializerValidation() {
        client.get().uri(BASE_PATH + "/" + IMPORT_ID + "/result")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchange().expectStatus().isOk();

        verify(registry).getSerializerForMediaType(MediaType.APPLICATION_JSON_VALUE);
        verify(serializer).supports(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void serverSentEvents_keepTheirExistingFilterExemption() {
        client.get().uri("/messageucolab/v1/messages/events")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .exchange().expectStatus().isNotFound();

        verify(registry, never()).getSerializerForMediaType(anyString());
    }

    private void assertExcelDownload(String path, String accept, String filename) {
        EntityExchangeResult<byte[]> response = client.get().uri(path).header(HttpHeaders.ACCEPT, accept)
                .exchange().expectStatus().isOk()
                .expectHeader().contentType(EXCEL)
                .expectBody(byte[].class).returnResult();

        assertAll(
                () -> assertThat(response.getResponseBody()).containsExactly(XLSX_BYTES),
                () -> assertThat(response.getResponseHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                        .contains("attachment").contains(filename),
                () -> assertThat(response.getResponseHeaders().getCacheControl()).isEqualTo("no-store"));
    }

    private static Stream<Arguments> downloadPathsAndSupportedAcceptHeaders() {
        return Stream.of(
                Arguments.of(BASE_PATH + "/template", EXCEL),
                Arguments.of(BASE_PATH + "/template", "*/*"),
                Arguments.of(BASE_PATH + "/" + IMPORT_ID + "/errors", EXCEL),
                Arguments.of(BASE_PATH + "/" + IMPORT_ID + "/errors", "*/*"));
    }

    private static Stream<Arguments> downloadPaths() {
        return Stream.of(
                Arguments.of(BASE_PATH + "/template"),
                Arguments.of(BASE_PATH + "/" + IMPORT_ID + "/errors"));
    }

    private static Stream<Arguments> pathsOutsideNarrowExemption() {
        return Stream.of(
                Arguments.of("POST", BASE_PATH + "/template"),
                Arguments.of("GET", BASE_PATH + "/not-a-uuid/errors"),
                Arguments.of("GET", BASE_PATH + "/" + IMPORT_ID + "/errors/extra"),
                Arguments.of("GET", "/messageucolab/v1/other/messages/imports/template"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebFlux
    static class MessageImportWebConfiguration {
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        WebFilter applicationIdContextFilter() {
            return (ServerWebExchange exchange, WebFilterChain chain) -> {
                exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, APP_ID);
                return chain.filter(exchange);
            };
        }
    }
}
