package com.quotare.cotacoes.resource;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
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

    private Cotacao persistirCotacao(Indicador indicador) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var cotacao = new Cotacao();
            cotacao.indicador = indicador;
            cotacao.valor = new BigDecimal("5.432100");
            cotacao.dataHora = Instant.now();
            cotacao.fonte = FonteDados.LOCAL;
            cotacao.persist();
            return cotacao;
        });
    }

    @Nested
    @DisplayName("POST /api/v1/indicadores")
    class Criar {

        @Test
        @DisplayName("retorna 201 com Location e o indicador criado no corpo")
        void retorna201ComLocationEIndicadorCriado() {
            var corpo = """
                    {
                        "codigo": "CRI1",
                        "nome": "Indicador Criado",
                        "fonte": "LOCAL"
                    }
                    """;

            var response = given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/indicadores")
                .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("codigo", is("CRI1"))
                    .body("nome", is("Indicador Criado"))
                    .body("fonte", is("LOCAL"))
                    .body("ativo", is(true))
                    .body("criadoEm", notNullValue())
                    .body("atualizadoEm", notNullValue())
                    .extract().response();

            var id = response.jsonPath().getLong("id");

            var indicadorRecarregado = QuarkusTransaction.requiringNew()
                    .call(() -> Indicador.<Indicador>findById(id));

            assertEquals(indicadorRecarregado.criadoEm.toString(), response.jsonPath().getString("criadoEm"));
            assertEquals(indicadorRecarregado.atualizadoEm.toString(), response.jsonPath().getString("atualizadoEm"));

            var location = response.header("Location");
            assertEquals("/api/v1/indicadores/" + id, URI.create(location).getPath());
        }

        @Test
        @DisplayName("o Location aponta para um recurso que existe")
        void locationApontaParaRecursoExistente() {
            var corpo = """
                    {
                        "codigo": "CRI2",
                        "nome": "Indicador Location",
                        "fonte": "EXTERNA"
                    }
                    """;

            var location = given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/indicadores")
                .then()
                    .statusCode(201)
                    .extract().header("Location");

            given()
                .when().get(location)
                .then()
                    .statusCode(200)
                    .body("codigo", is("CRI2"));
        }

        @Test
        @DisplayName("normaliza o código para maiúsculas antes de persistir")
        void normalizaCodigoParaMaiusculas() {
            var corpo = """
                    {
                        "codigo": "cri3",
                        "nome": "Indicador Minúsculo",
                        "fonte": "LOCAL"
                    }
                    """;

            var id = given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/indicadores")
                .then()
                    .statusCode(201)
                    .body("codigo", is("CRI3"))
                    .extract().jsonPath().getLong("id");

            var indicadorRecarregado = QuarkusTransaction.requiringNew()
                    .call(() -> Indicador.<Indicador>findById(id));

            assertEquals("CRI3", indicadorRecarregado.codigo);
        }

        @Test
        @DisplayName("rejeita corpo inválido com 400 sem gravar nada")
        void rejeitaCorpoInvalidoSemGravarNada() {
            var corpo = """
                    {
                        "codigo": "",
                        "nome": "Indicador Inválido",
                        "fonte": "LOCAL"
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/indicadores")
                .then()
                    .statusCode(400);

            var total = QuarkusTransaction.requiringNew().call(() -> Indicador.count());
            assertEquals(0, total);
        }

        @Test
        @DisplayName("retorna 409 em Problem Details quando o código já existe")
        void retorna409QuandoOCodigoJaExiste() {
            persistirIndicador("CRI4", "Original", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "codigo": "CRI4",
                        "nome": "Duplicado",
                        "fonte": "EXTERNA"
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/indicadores")
                .then()
                    .statusCode(409)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/codigo-duplicado"))
                    .body("title", is("Código duplicado"))
                    .body("status", is(409))
                    .body("detail", is("Já existe um indicador com o código CRI4"));

            QuarkusTransaction.requiringNew().run(() -> {
                assertEquals(1, Indicador.count());
                var indicadorExistente = Indicador.<Indicador>find("codigo", "CRI4").singleResult();
                assertEquals("Original", indicadorExistente.nome);
            });
        }

        @Test
        @DisplayName("retorna 409 quando o código difere só em maiúsculas e minúsculas")
        void retorna409QuandoOCodigoDifereSoEmMaiusculasEMinusculas() {
            persistirIndicador("CRI5", "Original", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "codigo": "cri5",
                        "nome": "Duplicado",
                        "fonte": "EXTERNA"
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/indicadores")
                .then()
                    .statusCode(409)
                    .body("detail", is("Já existe um indicador com o código CRI5"));

            QuarkusTransaction.requiringNew().run(() -> {
                assertEquals(1, Indicador.count());
            });
        }

        @Test
        @DisplayName("sob concorrência real, apenas uma das duas requisições com o mesmo código novo é aceita")
        void apenasUmaRequisicaoVenceQuandoDuasCriamComMesmoCodigoConcorrentemente() throws Exception {
            var corpo = """
                    {
                        "codigo": "RACE1",
                        "nome": "Corrida",
                        "fonte": "LOCAL"
                    }
                    """;

            var barreira = new CyclicBarrier(2);
            Callable<Integer> requisicao = () -> {
                barreira.await();
                return given()
                        .contentType(ContentType.JSON)
                        .body(corpo)
                    .when().post("/api/v1/indicadores")
                        .statusCode();
            };

            var executor = Executors.newFixedThreadPool(2);
            try {
                var futuro1 = executor.submit(requisicao);
                var futuro2 = executor.submit(requisicao);

                int status1 = futuro1.get(10, TimeUnit.SECONDS);
                int status2 = futuro2.get(10, TimeUnit.SECONDS);

                var statusOrdenados = List.of(status1, status2).stream().sorted().toList();
                assertEquals(List.of(201, 409), statusOrdenados,
                        "esperado exatamente um 201 e um 409 entre as duas requisições concorrentes");
            } finally {
                executor.shutdown();
            }

            QuarkusTransaction.requiringNew().run(() -> assertEquals(1, Indicador.count("codigo", "RACE1")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/indicadores/{id}")
    class Atualizar {

        @Test
        @DisplayName("retorna 200 com o indicador atualizado no corpo")
        void retorna200ComOIndicadorAtualizado() {
            var indicadorPersistido = persistirIndicador("ATU1", "Nome Antigo", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "codigo": "ATU1B",
                        "nome": "Nome Novo",
                        "fonte": "EXTERNA",
                        "ativo": false
                    }
                    """;

            var response = given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(200)
                    .body("id", is(indicadorPersistido.id.intValue()))
                    .body("codigo", is("ATU1B"))
                    .body("nome", is("Nome Novo"))
                    .body("fonte", is("EXTERNA"))
                    .body("ativo", is(false))
                    .extract().response();

            var indicadorRecarregado = QuarkusTransaction.requiringNew()
                    .call(() -> Indicador.<Indicador>findById(indicadorPersistido.id));

            assertEquals(indicadorRecarregado.criadoEm.toString(), response.jsonPath().getString("criadoEm"));
            assertEquals(indicadorRecarregado.atualizadoEm.toString(), response.jsonPath().getString("atualizadoEm"));
            assertEquals("ATU1B", indicadorRecarregado.codigo);
            assertEquals("Nome Novo", indicadorRecarregado.nome);
            assertEquals(FonteDados.EXTERNA, indicadorRecarregado.fonte);
            assertEquals(false, indicadorRecarregado.ativo);
        }

        @Test
        @DisplayName("aceita manter o mesmo código do próprio indicador")
        void aceitaManterOMesmoCodigoDoProprioIndicador() {
            var indicadorPersistido = persistirIndicador("ATU2", "Nome Antigo", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "codigo": "ATU2",
                        "nome": "Nome Atualizado",
                        "fonte": "LOCAL",
                        "ativo": true
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(200)
                    .body("codigo", is("ATU2"))
                    .body("nome", is("Nome Atualizado"));
        }

        @Test
        @DisplayName("retorna 409 quando o código pertence a outro indicador, sem alterar nada")
        void retorna409QuandoOCodigoPertenceAOutroIndicador() {
            persistirIndicador("ATU3", "Alvo", FonteDados.LOCAL, true);
            var outro = persistirIndicador("ATU4", "Outro", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "codigo": "atu3",
                        "nome": "Invadido",
                        "fonte": "EXTERNA",
                        "ativo": false
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/indicadores/{id}", outro.id)
                .then()
                    .statusCode(409)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/codigo-duplicado"))
                    .body("title", is("Código duplicado"))
                    .body("status", is(409))
                    .body("detail", is("Já existe um indicador com o código ATU3"));

            QuarkusTransaction.requiringNew().run(() -> {
                var atu4Recarregado = Indicador.<Indicador>findById(outro.id);
                assertEquals("ATU4", atu4Recarregado.codigo);
                assertEquals("Outro", atu4Recarregado.nome);

                var atu3Recarregado = Indicador.<Indicador>find("codigo", "ATU3").singleResult();
                assertEquals("Alvo", atu3Recarregado.nome);
            });
        }

        @Test
        @DisplayName("normaliza o código para maiúsculas antes de persistir")
        void normalizaCodigoParaMaiusculas() {
            var indicadorPersistido = persistirIndicador("ATU5", "Nome Antigo", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "codigo": "atu5b",
                        "nome": "Nome Novo",
                        "fonte": "LOCAL",
                        "ativo": true
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(200)
                    .body("codigo", is("ATU5B"));

            QuarkusTransaction.requiringNew().run(() -> {
                var indicadorRecarregado = Indicador.<Indicador>findById(indicadorPersistido.id);
                assertEquals("ATU5B", indicadorRecarregado.codigo);
            });
        }

        @Test
        @DisplayName("retorna 404 quando o indicador não existe")
        void retorna404QuandoOIndicadorNaoExiste() {
            var corpo = """
                    {
                        "codigo": "ATU7",
                        "nome": "Não Existe",
                        "fonte": "LOCAL",
                        "ativo": true
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/indicadores/{id}", 999999)
                .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"))
                    .body("title", is("Recurso não encontrado"))
                    .body("status", is(404))
                    .body("detail", is("O recurso solicitado não foi encontrado."));
        }

        @Test
        @DisplayName("rejeita corpo sem o campo ativo com 400")
        void rejeitaCorpoSemOCampoAtivo() {
            var indicadorPersistido = persistirIndicador("ATU6", "Nome Antigo", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "codigo": "ATU6",
                        "nome": "Nome Novo",
                        "fonte": "LOCAL"
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(400);

            QuarkusTransaction.requiringNew().run(() -> {
                var indicadorRecarregado = Indicador.<Indicador>findById(indicadorPersistido.id);
                assertEquals(true, indicadorRecarregado.ativo);
            });
        }

        @Test
        @DisplayName("retorna 400 em Problem Details quando o id não é numérico")
        void retorna400QuandoOIdNaoENumerico() {
            var corpo = """
                    {
                        "codigo": "ATU8",
                        "nome": "Não Importa",
                        "fonte": "LOCAL",
                        "ativo": true
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/indicadores/{id}", "abc")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/parametro-invalido"))
                    .body("title", is("Parâmetro inválido"))
                    .body("status", is(400))
                    .body("detail", is("O valor 'abc' não é válido para o parâmetro 'id'."));
        }
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
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"))
                    .body("title", is("Recurso não encontrado"))
                    .body("status", is(404))
                    .body("detail", is("O recurso solicitado não foi encontrado."));
        }

        @Test
        @DisplayName("retorna 400 em Problem Details quando o id não é numérico")
        void retorna400QuandoOIdNaoENumerico() {
            given()
                .when().get("/api/v1/indicadores/{id}", "abc")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/parametro-invalido"))
                    .body("title", is("Parâmetro inválido"))
                    .body("status", is(400))
                    .body("detail", is("O valor 'abc' não é válido para o parâmetro 'id'."));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/indicadores/{id}")
    class Remover {

        @Test
        @DisplayName("retorna 204 e remove de verdade um indicador sem cotações")
        void retorna204ERemoveDeVerdadeUmIndicadorSemCotacoes() {
            var indicadorPersistido = persistirIndicador("DEL1", "Indicador Removível", FonteDados.LOCAL, true);

            given()
                .when().delete("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(204);

            given()
                .when().get("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("retorna 404 quando o indicador não existe")
        void retorna404QuandoOIndicadorNaoExiste() {
            given()
                .when().delete("/api/v1/indicadores/{id}", 999999)
                .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"))
                    .body("title", is("Recurso não encontrado"))
                    .body("status", is(404))
                    .body("detail", is("O recurso solicitado não foi encontrado."));
        }

        @Test
        @DisplayName("retorna 409 em Problem Details e não remove quando há cotação vinculada")
        void retorna409ENaoRemoveQuandoHaCotacaoVinculada() {
            var indicadorPersistido = persistirIndicador("DEL2", "Indicador Com Cotação", FonteDados.LOCAL, true);
            persistirCotacao(indicadorPersistido);

            given()
                .when().delete("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(409)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/indicador-com-cotacoes"))
                    .body("title", is("Indicador possui cotações associadas"))
                    .body("status", is(409))
                    .body("detail", is("Não é possível remover um indicador com cotações associadas"));

            given()
                .when().get("/api/v1/indicadores/{id}", indicadorPersistido.id)
                .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("retorna 400 em Problem Details quando o id não é numérico")
        void retorna400QuandoOIdNaoENumerico() {
            given()
                .when().delete("/api/v1/indicadores/{id}", "abc")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/parametro-invalido"))
                    .body("title", is("Parâmetro inválido"))
                    .body("status", is(400))
                    .body("detail", is("O valor 'abc' não é válido para o parâmetro 'id'."));
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
        @DisplayName("retorna 400 em Problem Details quando size não é numérico")
        void retorna400QuandoSizeNaoENumerico() {
            given()
                .when().get("/api/v1/indicadores?size=abc")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/parametro-invalido"))
                    .body("title", is("Parâmetro inválido"))
                    .body("status", is(400))
                    .body("detail", is("Um dos parâmetros numéricos da requisição possui um valor inválido."));
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
        @DisplayName("retorna 400 em Problem Details quando a fonte informada não existe no enum")
        void retorna400QuandoFonteNaoExisteNoEnum() {
            given()
                .when().get("/api/v1/indicadores?fonte=BOLSA")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/parametro-invalido"))
                    .body("title", is("Parâmetro inválido"))
                    .body("status", is(400))
                    .body("detail", is("O valor 'BOLSA' não é válido para o parâmetro 'fonte'."));
        }

        @Test
        @DisplayName("retorna 400 em Problem Details quando ativo não é um booleano válido")
        void retorna400QuandoAtivoNaoEBooleano() {
            given()
                .when().get("/api/v1/indicadores?ativo=talvez")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/parametro-invalido"))
                    .body("title", is("Parâmetro inválido"))
                    .body("status", is(400))
                    .body("detail", is("O valor 'talvez' não é válido para o parâmetro 'ativo'."));

            given()
                .when().get("/api/v1/indicadores?ativo=1")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/parametro-invalido"))
                    .body("title", is("Parâmetro inválido"))
                    .body("status", is(400))
                    .body("detail", is("O valor '1' não é válido para o parâmetro 'ativo'."));
        }
    }
}
