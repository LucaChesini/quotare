package com.quotare.cotacoes;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@DisplayName("Health checks do serviço")
class HealthCheckTest {
    @Test
    @DisplayName("liveness responde UP")
    void livenessUp() {
        given()
            .when().get("/q/health/live")
            .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    @DisplayName("readiness confirma conexão com o banco")
    void readinessConfirmaBanco() {
        given()
            .when().get("/q/health/ready")
            .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.find { it.name.contains('Database') }.status", is("UP"));
    }
}
