package com.quotare.integracao.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@DisplayName("CotacaoExternaResource")
class CotacaoExternaResourceTest {

    @Nested
    @DisplayName("GET /cotacoes-externas/{codigo}/serie")
    class BuscarSerie {

        @Test
        @DisplayName("retorna 200 com a quantidade de pontos pedida em 'dias'")
        void retorna200ComQuantidadeDeDiasPedida() {
            given()
                    .when()
                    .get("/cotacoes-externas/USD/serie?dias=5")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(5));
        }

        @Test
        @DisplayName("usa 30 como valor padrão de 'dias' quando o parâmetro não é informado")
        void usaTrintaDiasComoPadrao() {
            given()
                    .when()
                    .get("/cotacoes-externas/USD/serie")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(30));
        }

        @Test
        @DisplayName("retorna 400 quando 'dias' é zero")
        void retorna400QuandoDiasEhZero() {
            given()
                    .when()
                    .get("/cotacoes-externas/USD/serie?dias=0")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("retorna 400 quando 'dias' é negativo")
        void retorna400QuandoDiasEhNegativo() {
            given()
                    .when()
                    .get("/cotacoes-externas/USD/serie?dias=-1")
                    .then()
                    .statusCode(400);
        }
    }
}
