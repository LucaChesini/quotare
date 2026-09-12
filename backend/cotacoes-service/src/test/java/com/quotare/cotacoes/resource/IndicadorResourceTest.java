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

import java.util.ArrayList;
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

    @Nested
    @DisplayName("GET /api/v1/indicadores — filtros e ordenação")
    class FiltrarEOrdenar {

        @Test
        @DisplayName("filtra apenas pela fonte informada")
        void filtraApenasPelaFonte() {
            persistirIndicador("FLT1", "Filtro Local", FonteDados.LOCAL, true);
            persistirIndicador("FLT2", "Filtro Externa", FonteDados.EXTERNA, true);

            given()
                .when().get("/api/v1/indicadores?fonte=LOCAL")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("itens[0].codigo", is("FLT1"))
                    .body("totalItens", is(1));
        }

        @Test
        @DisplayName("filtra apenas pelos indicadores ativos")
        void filtraApenasPelosAtivos() {
            persistirIndicador("FLT3", "Filtro Ativo", FonteDados.LOCAL, true);
            persistirIndicador("FLT4", "Filtro Inativo", FonteDados.LOCAL, false);

            given()
                .when().get("/api/v1/indicadores?ativo=true")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("itens[0].codigo", is("FLT3"))
                    .body("totalItens", is(1));
        }

        @Test
        @DisplayName("filtra apenas pelos indicadores inativos")
        void filtraApenasPelosInativos() {
            persistirIndicador("FLT5", "Filtro Ativo Dois", FonteDados.LOCAL, true);
            persistirIndicador("FLT6", "Filtro Inativo Dois", FonteDados.LOCAL, false);

            given()
                .when().get("/api/v1/indicadores?ativo=false")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("itens[0].codigo", is("FLT6"))
                    .body("totalItens", is(1));
        }

        @Test
        @DisplayName("combina os filtros de fonte e ativo")
        void combinaFiltrosDeFonteEAtivo() {
            persistirIndicador("FLT7", "Combinado Alvo", FonteDados.EXTERNA, true);
            persistirIndicador("FLT8", "Combinado Fonte Diferente", FonteDados.LOCAL, true);
            persistirIndicador("FLT9", "Combinado Ativo Diferente", FonteDados.EXTERNA, false);

            given()
                .when().get("/api/v1/indicadores?fonte=EXTERNA&ativo=true")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("itens[0].codigo", is("FLT7"))
                    .body("totalItens", is(1));
        }

        @Test
        @DisplayName("devolve os indicadores ordenados por id ao atravessar as páginas")
        void devolveOrdenadoPorIdAoAtravessarPaginas() {
            var codigos = List.of("ORD1", "ORD2", "ORD3", "ORD4", "ORD5");
            var idsNaOrdemDeInsercao = codigos.stream()
                    .map(codigo -> persistirIndicador(codigo, "Ordem " + codigo, FonteDados.LOCAL, true).id)
                    .toList();

            var idsPagina0 = given()
                .when().get("/api/v1/indicadores?page=0&size=2")
                .then()
                    .statusCode(200)
                    .extract().jsonPath().getList("itens.id", Long.class);

            var idsPagina1 = given()
                .when().get("/api/v1/indicadores?page=1&size=2")
                .then()
                    .statusCode(200)
                    .extract().jsonPath().getList("itens.id", Long.class);

            var idsPagina2 = given()
                .when().get("/api/v1/indicadores?page=2&size=2")
                .then()
                    .statusCode(200)
                    .extract().jsonPath().getList("itens.id", Long.class);

            var idsConcatenados = new ArrayList<Long>();
            idsConcatenados.addAll(idsPagina0);
            idsConcatenados.addAll(idsPagina1);
            idsConcatenados.addAll(idsPagina2);

            assertEquals(idsNaOrdemDeInsercao, idsConcatenados);
        }

        @Test
        @DisplayName("retorna 404 quando a fonte informada não existe no enum")
        void retorna404QuandoFonteNaoExisteNoEnum() {
            given()
                .when().get("/api/v1/indicadores?fonte=BOLSA")
                .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("interpreta valor não booleano de ativo como false, sem erro")
        void interpretaAtivoNaoBooleanoComoFalse() {
            persistirIndicador("BOO1", "Booleano Ativo", FonteDados.LOCAL, true);
            persistirIndicador("BOO2", "Booleano Inativo", FonteDados.LOCAL, false);

            given()
                .when().get("/api/v1/indicadores?ativo=talvez")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("itens[0].codigo", is("BOO2"));

            given()
                .when().get("/api/v1/indicadores?ativo=1")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("itens[0].codigo", is("BOO2"));
        }
    }
}
