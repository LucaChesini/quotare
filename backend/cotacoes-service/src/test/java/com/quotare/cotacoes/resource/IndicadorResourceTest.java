package com.quotare.cotacoes.resource;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@DisplayName("IndicadorResource")
class IndicadorResourceTest {

    @BeforeEach
    void limparBanco() {
        QuarkusTransaction.requiringNew().run(() -> {
            Cotacao.deleteAll();
            Indicador.deleteAll();
        });
    }

    private Indicador persistirIndicador(String codigo, String nome, FonteDados fonte, boolean ativo) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var indicador = new Indicador();
            indicador.codigo = codigo;
            indicador.nome = nome;
            indicador.fonte = fonte;
            indicador.ativo = ativo;
            indicador.persist();
            return indicador;
        });
    }

    @Nested
    @DisplayName("GET /api/v1/indicadores/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("retorna 200 com todos os campos quando o indicador existe")
        void retorna200ComTodosOsCampos() {
            var indicadorPersistido = persistirIndicador("EURX", "Euro de Teste", FonteDados.EXTERNA, false);

            var indicadorRecarregado = QuarkusTransaction.requiringNew()
                    .call(() -> Indicador.<Indicador>findById(indicadorPersistido.id));

            given()
                .when().get("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(200)
                    .body("id", is(indicadorPersistido.id.intValue()))
                    .body("codigo", is("EURX"))
                    .body("nome", is("Euro de Teste"))
                    .body("fonte", is("EXTERNA"))
                    .body("ativo", is(false))
                    .body("criadoEm", is(indicadorRecarregado.criadoEm.toString()))
                    .body("atualizadoEm", is(indicadorRecarregado.atualizadoEm.toString()));
        }

        @Test
        @DisplayName("retorna 404 quando o indicador não existe")
        void retorna404QuandoNaoExiste() {
            given()
                .when().get("/api/v1/indicadores/{id}", 999999)
                .then()
                    .statusCode(404);
        }
    }
}
