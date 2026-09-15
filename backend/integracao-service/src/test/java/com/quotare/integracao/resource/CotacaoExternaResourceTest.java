package com.quotare.integracao.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@DisplayName("CotacaoExternaResource")
class CotacaoExternaResourceTest {

    @Nested
    @DisplayName("GET /cotacoes-externas/{codigo}/serie")
    class BuscarSerie {

        @Test
        @DisplayName("retorna 200 com pontos para o intervalo 'inicio'/'fim' informado")
        void retorna200ComIntervaloInformado() {
            String inicio = Instant.now().minus(5, ChronoUnit.DAYS).toString();
            String fim = Instant.now().toString();

            given()
                    .when()
                    .get("/cotacoes-externas/USD/serie?inicio=" + inicio + "&fim=" + fim)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(5));
        }

        @Test
        @DisplayName("usa 30 dias (fim=agora, inicio=agora-30d) como padrão quando os parâmetros não são informados")
        void usaTrintaDiasComoPadrao() {
            given()
                    .when()
                    .get("/cotacoes-externas/USD/serie")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(30));
        }

        @Test
        @DisplayName("retorna 400 quando 'inicio' não é anterior a 'fim'")
        void retorna400QuandoInicioNaoEhAnteriorAFim() {
            String fim = Instant.now().minus(5, ChronoUnit.DAYS).toString();
            String inicio = Instant.now().toString();

            given()
                    .when()
                    .get("/cotacoes-externas/USD/serie?inicio=" + inicio + "&fim=" + fim)
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("retorna 400 quando 'inicio' ou 'fim' não está em formato ISO-8601 válido")
        void retorna400QuandoFormatoEhInvalido() {
            given()
                    .when()
                    .get("/cotacoes-externas/USD/serie?inicio=data-invalida")
                    .then()
                    .statusCode(400);
        }
    }
}
