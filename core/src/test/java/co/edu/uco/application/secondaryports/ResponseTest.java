package co.edu.uco.application.secondaryports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseTest {

    @Test
    void constructor_withErrors_storesBoth() {
        Response<String> response = new Response<>(List.of("a", "b"), List.of("err"));

        assertThat(response.data()).containsExactly("a", "b");
        assertThat(response.errors()).containsExactly("err");
    }

    @Test
    void constructor_withDataOnly_usesEmptyErrors() {
        Response<String> response = new Response<>(List.of("a"));

        assertThat(response.data()).containsExactly("a");
        assertThat(response.errors()).isEmpty();
    }
}