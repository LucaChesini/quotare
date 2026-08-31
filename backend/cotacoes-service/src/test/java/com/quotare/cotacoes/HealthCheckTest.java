package com.quotare.cotacoes;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class HealthCheckTest {
    @Test
    void livenessEndpointEstaUp() {
        given()
            .when().get("/q/health/live")
            .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void readinessEndpointConfirmaConexaoComOBanco() {
        given()
            .when().get("/q/health/ready")
            .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.find { it.name.contains('Database') }.status", is("UP"));
    }
}
