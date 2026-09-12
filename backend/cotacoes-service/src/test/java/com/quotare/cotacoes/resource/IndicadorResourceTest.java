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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Nested
    @DisplayName("GET /api/v1/indicadores")
    class Listar {

        @Test
        @DisplayName("retorna envelope vazio quando não há indicadores")
        void retornaEnvelopeVazioQuandoNaoHaIndicadores() {
            given()
                .when().get("/api/v1/indicadores")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(0))
                    .body("pagina", is(0))
                    .body("tamanho", is(20))
                    .body("totalItens", is(0))
                    .body("totalPaginas", is(1));
        }

        @Test
        @DisplayName("pagina os indicadores de acordo com o tamanho pedido")
        void paginaIndicadoresDeAcordoComTamanhoPedido() {
            var codigos = List.of("PGN1", "PGN2", "PGN3", "PGN4", "PGN5");
            codigos.forEach(codigo -> persistirIndicador(codigo, "Indicador " + codigo, FonteDados.LOCAL, true));

            var itensPagina0 = given()
                .when().get("/api/v1/indicadores?page=0&size=2")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(2))
                    .body("pagina", is(0))
                    .body("tamanho", is(2))
                    .body("totalItens", is(5))
                    .body("totalPaginas", is(3))
                    .extract().jsonPath().getList("itens.codigo", String.class);

            var itensPagina1 = given()
                .when().get("/api/v1/indicadores?page=1&size=2")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(2))
                    .body("pagina", is(1))
                    .body("tamanho", is(2))
                    .body("totalItens", is(5))
                    .body("totalPaginas", is(3))
                    .extract().jsonPath().getList("itens.codigo", String.class);

            var itensPagina2 = given()
                .when().get("/api/v1/indicadores?page=2&size=2")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("pagina", is(2))
                    .body("tamanho", is(2))
                    .body("totalItens", is(5))
                    .body("totalPaginas", is(3))
                    .extract().jsonPath().getList("itens.codigo", String.class);

            Set<String> todosOsCodigosRetornados = new HashSet<>();
            todosOsCodigosRetornados.addAll(itensPagina0);
            todosOsCodigosRetornados.addAll(itensPagina1);
            todosOsCodigosRetornados.addAll(itensPagina2);

            assertEquals(new HashSet<>(codigos), todosOsCodigosRetornados);
        }

        @Test
        @DisplayName("retorna itens vazios quando a página pedida está além do total")
        void retornaItensVaziosQuandoPaginaAlemDoTotal() {
            persistirIndicador("PGN6", "Indicador Além", FonteDados.LOCAL, true);

            given()
                .when().get("/api/v1/indicadores?page=99&size=10")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(0))
                    .body("pagina", is(99))
                    .body("tamanho", is(10))
                    .body("totalItens", is(1))
                    .body("totalPaginas", is(1));
        }

        @Test
        @DisplayName("rejeita page negativo")
        void rejeitaPageNegativo() {
            given()
                .when().get("/api/v1/indicadores?page=-1")
                .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("rejeita size zero")
        void rejeitaSizeZero() {
            given()
                .when().get("/api/v1/indicadores?size=0")
                .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("rejeita size acima de cem")
        void rejeitaSizeAcimaDeCem() {
            given()
                .when().get("/api/v1/indicadores?size=101")
                .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("aceita size exatamente no limite de cem")
        void aceitaSizeNoLimiteDeCem() {
            given()
                .when().get("/api/v1/indicadores?size=100")
                .then()
                    .statusCode(200)
                    .body("tamanho", is(100));
        }

        @Test
        @DisplayName("acumula as violações quando page e size são inválidos juntos")
        void acumulaViolacoesDePageESize() {
            given()
                .when().get("/api/v1/indicadores?page=-1&size=0")
                .then()
                    .statusCode(400)
                    .body("violations", hasSize(2))
                    .body("violations.field", containsInAnyOrder("listar.page", "listar.size"));
        }
    }
}
