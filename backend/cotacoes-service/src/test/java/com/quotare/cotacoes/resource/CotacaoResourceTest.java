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
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@DisplayName("CotacaoResource")
class CotacaoResourceTest {

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

    private Cotacao persistirCotacao(Indicador indicador, BigDecimal valor, Instant dataHora) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var cotacao = new Cotacao();
            cotacao.indicador = indicador;
            cotacao.valor = valor;
            cotacao.dataHora = dataHora;
            cotacao.fonte = FonteDados.LOCAL;
            cotacao.persist();
            return cotacao;
        });
    }

    @Nested
    @DisplayName("POST /api/v1/cotacoes")
    class Criar {

        @Test
        @DisplayName("retorna 201 com Location e a cotação criada no corpo")
        void retorna201ComLocationECotacaoCriada() {
            var indicador = persistirIndicador("CRI1", "Indicador Cotação", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 5.432100,
                        "dataHora": "2024-06-01T10:15:30Z"
                    }
                    """.formatted(indicador.id);

            var response = given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("indicadorId", is(indicador.id.intValue()))
                    .body("dataHora", is("2024-06-01T10:15:30Z"))
                    .body("fonte", is("LOCAL"))
                    .body("criadoEm", notNullValue())
                    .extract().response();

            var id = response.jsonPath().getLong("id");

            var valorRetornado = new BigDecimal(response.jsonPath().getString("valor"));
            assertEquals(0, new BigDecimal("5.432100").compareTo(valorRetornado), "valor deveria ser 5.432100");

            var cotacaoRecarregada = QuarkusTransaction.requiringNew()
                    .call(() -> Cotacao.<Cotacao>findById(id));

            assertEquals(cotacaoRecarregada.criadoEm.toString(), response.jsonPath().getString("criadoEm"));
            assertEquals(FonteDados.LOCAL, cotacaoRecarregada.fonte);

            var location = response.header("Location");
            assertEquals("/api/v1/cotacoes/" + id, URI.create(location).getPath());
        }

        @Test
        @DisplayName("o Location aponta para um recurso que existe")
        void locationApontaParaRecursoExistente() {
            var indicador = persistirIndicador("CRI2", "Indicador Location", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 3.140000,
                        "dataHora": "2024-06-02T08:00:00Z"
                    }
                    """.formatted(indicador.id);

            var location = given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(201)
                    .extract().header("Location");

            given()
                .when().get(location)
                .then()
                    .statusCode(200)
                    .body("indicadorId", is(indicador.id.intValue()));
        }

        @Test
        @DisplayName("retorna 404 quando o indicadorId não existe")
        void retorna404QuandoOIndicadorIdNaoExiste() {
            var corpo = """
                    {
                        "indicadorId": 999999,
                        "valor": 1.000000,
                        "dataHora": "2024-06-01T10:15:30Z"
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"))
                    .body("title", is("Recurso não encontrado"))
                    .body("status", is(404))
                    .body("detail", is("O recurso solicitado não foi encontrado."));
        }

        @Test
        @DisplayName("rejeita valor zero ou negativo com 400, sem gravar nada")
        void rejeitaValorZeroOuNegativo() {
            var indicador = persistirIndicador("CRI3", "Indicador Valor", FonteDados.LOCAL, true);

            var corpoComZero = """
                    {
                        "indicadorId": %d,
                        "valor": 0,
                        "dataHora": "2024-06-01T10:15:30Z"
                    }
                    """.formatted(indicador.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpoComZero)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(400);

            var corpoComNegativo = """
                    {
                        "indicadorId": %d,
                        "valor": -1.500000,
                        "dataHora": "2024-06-01T10:15:30Z"
                    }
                    """.formatted(indicador.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpoComNegativo)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(400);

            QuarkusTransaction.requiringNew().run(() -> assertEquals(0, Cotacao.count()));
        }

        @Test
        @DisplayName("rejeita dataHora no futuro com 400, sem gravar nada")
        void rejeitaDataHoraNoFuturo() {
            var indicador = persistirIndicador("CRI4", "Indicador Futuro", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 2.000000,
                        "dataHora": "2030-01-01T00:00:00Z"
                    }
                    """.formatted(indicador.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(400);

            QuarkusTransaction.requiringNew().run(() -> assertEquals(0, Cotacao.count()));
        }

        @Test
        @DisplayName("rejeita corpo sem campo obrigatório com 400, sem gravar nada")
        void rejeitaCorpoSemCampoObrigatorioSemGravarNada() {
            var indicador = persistirIndicador("CRI5", "Indicador Incompleto", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "indicadorId": %d,
                        "dataHora": "2024-06-01T10:15:30Z"
                    }
                    """.formatted(indicador.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(400);

            QuarkusTransaction.requiringNew().run(() -> assertEquals(0, Cotacao.count()));
        }

        @Test
        @DisplayName("retorna 409 em Problem Details quando indicador/dataHora/fonte já existem")
        void retorna409QuandoIndicadorDataHoraFonteJaExistem() {
            var indicador = persistirIndicador("CRI6", "Indicador Duplicado", FonteDados.LOCAL, true);
            var dataHoraFixa = Instant.parse("2024-06-05T12:00:00Z");
            persistirCotacao(indicador, new BigDecimal("4.000000"), dataHoraFixa);

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 9.000000,
                        "dataHora": "2024-06-05T12:00:00Z"
                    }
                    """.formatted(indicador.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(409)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/conflito-de-concorrencia"))
                    .body("title", is("Conflito de concorrência"))
                    .body("status", is(409))
                    .body("detail", is("Não foi possível salvar devido a uma alteração concorrente. Tente novamente."));

            QuarkusTransaction.requiringNew().run(() -> assertEquals(1, Cotacao.count()));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/cotacoes/{id}")
    class Atualizar {

        @Test
        @DisplayName("retorna 200 com a cotação atualizada no corpo")
        void retorna200ComACotacaoAtualizada() {
            var indicador = persistirIndicador("ATU1", "Indicador Original", FonteDados.LOCAL, true);
            var cotacaoPersistida = persistirCotacao(indicador, new BigDecimal("1.000000"), Instant.parse("2024-01-01T00:00:00Z"));

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 8.765432,
                        "dataHora": "2024-02-02T15:30:00Z"
                    }
                    """.formatted(indicador.id);

            var response = given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(200)
                    .body("id", is(cotacaoPersistida.id.intValue()))
                    .body("indicadorId", is(indicador.id.intValue()))
                    .body("dataHora", is("2024-02-02T15:30:00Z"))
                    .body("fonte", is("LOCAL"))
                    .extract().response();

            var valorRetornado = new BigDecimal(response.jsonPath().getString("valor"));
            assertEquals(0, new BigDecimal("8.765432").compareTo(valorRetornado));

            var cotacaoRecarregada = QuarkusTransaction.requiringNew()
                    .call(() -> Cotacao.<Cotacao>findById(cotacaoPersistida.id));

            assertEquals(Instant.parse("2024-02-02T15:30:00Z"), cotacaoRecarregada.dataHora);
            assertEquals(0, new BigDecimal("8.765432").compareTo(cotacaoRecarregada.valor));
        }

        @Test
        @DisplayName("retorna 404 quando a cotação não existe")
        void retorna404QuandoACotacaoNaoExiste() {
            var indicador = persistirIndicador("ATU2", "Indicador Existente", FonteDados.LOCAL, true);

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 1.000000,
                        "dataHora": "2024-01-01T00:00:00Z"
                    }
                    """.formatted(indicador.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/cotacoes/{id}", 999999)
                .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"))
                    .body("title", is("Recurso não encontrado"))
                    .body("status", is(404))
                    .body("detail", is("O recurso solicitado não foi encontrado."));
        }

        @Test
        @DisplayName("retorna 404 quando o novo indicadorId do request não existe, sem alterar a cotação")
        void retorna404QuandoONovoIndicadorIdNaoExiste() {
            var indicador = persistirIndicador("ATU3", "Indicador Original", FonteDados.LOCAL, true);
            var cotacaoPersistida = persistirCotacao(indicador, new BigDecimal("2.000000"), Instant.parse("2024-01-01T00:00:00Z"));

            var corpo = """
                    {
                        "indicadorId": 999999,
                        "valor": 2.000000,
                        "dataHora": "2024-01-01T00:00:00Z"
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"));

            QuarkusTransaction.requiringNew().run(() -> {
                var cotacaoIntacta = Cotacao.<Cotacao>findById(cotacaoPersistida.id);
                assertEquals(indicador.id, cotacaoIntacta.indicador.id);
            });
        }

        @Test
        @DisplayName("rejeita valor zero ou negativo com 400, sem alterar a cotação")
        void rejeitaValorZeroOuNegativo() {
            var indicador = persistirIndicador("ATU4", "Indicador Validação", FonteDados.LOCAL, true);
            var cotacaoPersistida = persistirCotacao(indicador, new BigDecimal("3.000000"), Instant.parse("2024-01-01T00:00:00Z"));

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": -5.000000,
                        "dataHora": "2024-01-01T00:00:00Z"
                    }
                    """.formatted(indicador.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(400);

            QuarkusTransaction.requiringNew().run(() -> {
                var cotacaoIntacta = Cotacao.<Cotacao>findById(cotacaoPersistida.id);
                assertEquals(0, new BigDecimal("3.000000").compareTo(cotacaoIntacta.valor));
            });
        }

        @Test
        @DisplayName("rejeita dataHora no futuro com 400")
        void rejeitaDataHoraNoFuturo() {
            var indicador = persistirIndicador("ATU5", "Indicador Futuro", FonteDados.LOCAL, true);
            var cotacaoPersistida = persistirCotacao(indicador, new BigDecimal("4.000000"), Instant.parse("2024-01-01T00:00:00Z"));

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 4.000000,
                        "dataHora": "2030-01-01T00:00:00Z"
                    }
                    """.formatted(indicador.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("move a cotação para outro indicador LOCAL")
        void moveACotacaoParaOutroIndicador() {
            var indicadorOrigem = persistirIndicador("ATU6", "Indicador Origem", FonteDados.LOCAL, true);
            var indicadorDestino = persistirIndicador("ATU7", "Indicador Destino", FonteDados.LOCAL, true);
            var cotacaoPersistida = persistirCotacao(indicadorOrigem, new BigDecimal("5.000000"), Instant.parse("2024-01-01T00:00:00Z"));

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 5.000000,
                        "dataHora": "2024-01-01T00:00:00Z"
                    }
                    """.formatted(indicadorDestino.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(200)
                    .body("indicadorId", is(indicadorDestino.id.intValue()));

            QuarkusTransaction.requiringNew().run(() -> {
                var cotacaoRecarregada = Cotacao.<Cotacao>findById(cotacaoPersistida.id);
                assertEquals(indicadorDestino.id, cotacaoRecarregada.indicador.id);
            });
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cotacoes/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("retorna 200 com todos os campos quando a cotação existe")
        void retorna200ComTodosOsCampos() {
            var indicador = persistirIndicador("BSC1", "Indicador Busca", FonteDados.LOCAL, true);
            var cotacaoPersistida = persistirCotacao(indicador, new BigDecimal("6.123456"), Instant.parse("2024-03-01T09:00:00Z"));

            var cotacaoRecarregada = QuarkusTransaction.requiringNew()
                    .call(() -> Cotacao.<Cotacao>findById(cotacaoPersistida.id));

            given()
                .when().get("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(200)
                    .body("id", is(cotacaoPersistida.id.intValue()))
                    .body("indicadorId", is(indicador.id.intValue()))
                    .body("dataHora", is("2024-03-01T09:00:00Z"))
                    .body("fonte", is("LOCAL"))
                    .body("criadoEm", is(cotacaoRecarregada.criadoEm.toString()));
        }

        @Test
        @DisplayName("retorna 404 quando a cotação não existe")
        void retorna404QuandoNaoExiste() {
            given()
                .when().get("/api/v1/cotacoes/{id}", 999999)
                .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"))
                    .body("title", is("Recurso não encontrado"))
                    .body("status", is(404))
                    .body("detail", is("O recurso solicitado não foi encontrado."));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/cotacoes/{id}")
    class Remover {

        @Test
        @DisplayName("retorna 204 e remove de verdade a cotação")
        void retorna204ERemoveDeVerdadeACotacao() {
            var indicador = persistirIndicador("DEL1", "Indicador Removível", FonteDados.LOCAL, true);
            var cotacaoPersistida = persistirCotacao(indicador, new BigDecimal("7.000000"), Instant.parse("2024-04-01T00:00:00Z"));

            given()
                .when().delete("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(204);

            given()
                .when().get("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("retorna 404 quando a cotação não existe")
        void retorna404QuandoACotacaoNaoExiste() {
            given()
                .when().delete("/api/v1/cotacoes/{id}", 999999)
                .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"))
                    .body("title", is("Recurso não encontrado"))
                    .body("status", is(404))
                    .body("detail", is("O recurso solicitado não foi encontrado."));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cotacoes")
    class Listar {

        @Test
        @DisplayName("retorna envelope vazio quando não há cotações")
        void retornaEnvelopeVazioQuandoNaoHaCotacoes() {
            given()
                .when().get("/api/v1/cotacoes")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(0))
                    .body("pagina", is(0))
                    .body("tamanho", is(20))
                    .body("totalItens", is(0))
                    .body("totalPaginas", is(1));
        }

        @Test
        @DisplayName("pagina as cotações de acordo com o tamanho pedido")
        void paginaCotacoesDeAcordoComTamanhoPedido() {
            var indicador = persistirIndicador("LST1", "Indicador Lista", FonteDados.LOCAL, true);
            var idsNaOrdemDeInsercao = List.of("2024-05-01T00:00:00Z", "2024-05-02T00:00:00Z", "2024-05-03T00:00:00Z").stream()
                    .map(dataHora -> persistirCotacao(indicador, new BigDecimal("1.000000"), Instant.parse(dataHora)).id)
                    .toList();

            var idsPagina0 = given()
                .when().get("/api/v1/cotacoes?page=0&size=2")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(2))
                    .body("pagina", is(0))
                    .body("tamanho", is(2))
                    .body("totalItens", is(3))
                    .body("totalPaginas", is(2))
                    .extract().jsonPath().getList("itens.id", Long.class);

            var idsPagina1 = given()
                .when().get("/api/v1/cotacoes?page=1&size=2")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("pagina", is(1))
                    .body("tamanho", is(2))
                    .body("totalItens", is(3))
                    .body("totalPaginas", is(2))
                    .extract().jsonPath().getList("itens.id", Long.class);

            var idsConcatenados = new ArrayList<Long>(idsPagina0);
            idsConcatenados.addAll(idsPagina1);

            assertEquals(idsNaOrdemDeInsercao, idsConcatenados);
        }

        @Test
        @DisplayName("filtra pelo indicadorId informado")
        void filtraPeloIndicadorIdInformado() {
            var indicadorA = persistirIndicador("LST2", "Indicador A", FonteDados.LOCAL, true);
            var indicadorB = persistirIndicador("LST3", "Indicador B", FonteDados.LOCAL, true);
            persistirCotacao(indicadorA, new BigDecimal("1.000000"), Instant.parse("2024-06-01T00:00:00Z"));
            persistirCotacao(indicadorB, new BigDecimal("2.000000"), Instant.parse("2024-06-02T00:00:00Z"));

            given()
                    .queryParam("indicadorId", indicadorA.id)
                .when().get("/api/v1/cotacoes")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("itens[0].indicadorId", is(indicadorA.id.intValue()))
                    .body("totalItens", is(1));
        }

        @Test
        @DisplayName("filtra pelo intervalo inicio/fim informado")
        void filtraPeloIntervaloInicioFimInformado() {
            var indicador = persistirIndicador("LST4", "Indicador Intervalo", FonteDados.LOCAL, true);
            persistirCotacao(indicador, new BigDecimal("1.000000"), Instant.parse("2024-07-01T00:00:00Z"));
            var cotacaoNoIntervalo = persistirCotacao(indicador, new BigDecimal("2.000000"), Instant.parse("2024-07-05T00:00:00Z"));
            persistirCotacao(indicador, new BigDecimal("3.000000"), Instant.parse("2024-07-10T00:00:00Z"));

            given()
                    .queryParam("inicio", "2024-07-03T00:00:00Z")
                    .queryParam("fim", "2024-07-07T00:00:00Z")
                .when().get("/api/v1/cotacoes")
                .then()
                    .statusCode(200)
                    .body("itens", hasSize(1))
                    .body("itens[0].id", is(cotacaoNoIntervalo.id.intValue()))
                    .body("totalItens", is(1));
        }
    }

    @Nested
    @DisplayName("Regra de fonte exclusiva (indicador EXTERNA)")
    class FonteExclusiva {

        @Test
        @DisplayName("POST em indicador EXTERNA retorna 409 e não persiste nada")
        void postEmIndicadorExternaRetorna409ENaoPersisteNada() {
            var indicadorExterno = persistirIndicador("EXT1", "Indicador Externo", FonteDados.EXTERNA, true);

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 1.000000,
                        "dataHora": "2024-08-01T00:00:00Z"
                    }
                    """.formatted(indicadorExterno.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().post("/api/v1/cotacoes")
                .then()
                    .statusCode(409)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/fonte-exclusiva"))
                    .body("title", is("Cotação gerida por integração automática"))
                    .body("status", is(409))
                    .body("detail", containsString("EXT1"));

            QuarkusTransaction.requiringNew().run(() -> assertEquals(0, Cotacao.count()));
        }

        @Test
        @DisplayName("PUT movendo cotação de LOCAL para EXTERNA retorna 409 sem alterar a cotação")
        void putMovendoCotacaoDeLocalParaExternaRetorna409SemAlterar() {
            var indicadorOrigem = persistirIndicador("EXT2", "Indicador Origem Local", FonteDados.LOCAL, true);
            var indicadorDestino = persistirIndicador("EXT3", "Indicador Destino Externo", FonteDados.EXTERNA, true);
            var cotacaoPersistida = persistirCotacao(indicadorOrigem, new BigDecimal("2.000000"), Instant.parse("2024-08-02T00:00:00Z"));

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 9.999999,
                        "dataHora": "2024-08-03T00:00:00Z"
                    }
                    """.formatted(indicadorDestino.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(409)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/fonte-exclusiva"));

            QuarkusTransaction.requiringNew().run(() -> {
                var cotacaoIntacta = Cotacao.<Cotacao>findById(cotacaoPersistida.id);
                assertEquals(indicadorOrigem.id, cotacaoIntacta.indicador.id);
                assertEquals(0, new BigDecimal("2.000000").compareTo(cotacaoIntacta.valor));
                assertEquals(Instant.parse("2024-08-02T00:00:00Z"), cotacaoIntacta.dataHora);
            });
        }

        @Test
        @DisplayName("PUT em cotação que já pertence a um indicador EXTERNA retorna 409 sem alterar nada")
        void putEmCotacaoQueJaPertenceAIndicadorExternaRetorna409() {
            var indicadorExterno = persistirIndicador("EXT4", "Indicador Já Externo", FonteDados.EXTERNA, true);
            var indicadorLocal = persistirIndicador("EXT5", "Indicador Local Alvo", FonteDados.LOCAL, true);
            var cotacaoPersistida = persistirCotacao(indicadorExterno, new BigDecimal("3.000000"), Instant.parse("2024-08-04T00:00:00Z"));

            var corpo = """
                    {
                        "indicadorId": %d,
                        "valor": 4.000000,
                        "dataHora": "2024-08-05T00:00:00Z"
                    }
                    """.formatted(indicadorLocal.id);

            given()
                    .contentType(ContentType.JSON)
                    .body(corpo)
                .when().put("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(409)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/fonte-exclusiva"));

            QuarkusTransaction.requiringNew().run(() -> {
                var cotacaoIntacta = Cotacao.<Cotacao>findById(cotacaoPersistida.id);
                assertEquals(indicadorExterno.id, cotacaoIntacta.indicador.id);
                assertEquals(0, new BigDecimal("3.000000").compareTo(cotacaoIntacta.valor));
                assertEquals(Instant.parse("2024-08-04T00:00:00Z"), cotacaoIntacta.dataHora);
            });
        }

        @Test
        @DisplayName("DELETE em cotação de indicador EXTERNA retorna 409 e mantém a cotação")
        void deleteEmCotacaoDeIndicadorExternaRetorna409EMantemACotacao() {
            var indicadorExterno = persistirIndicador("EXT6", "Indicador Externo Delete", FonteDados.EXTERNA, true);
            var cotacaoPersistida = persistirCotacao(indicadorExterno, new BigDecimal("5.000000"), Instant.parse("2024-08-06T00:00:00Z"));

            given()
                .when().delete("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(409)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/fonte-exclusiva"));

            given()
                .when().get("/api/v1/cotacoes/{id}", cotacaoPersistida.id)
                .then()
                    .statusCode(200);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cotacoes/serie")
    class Serie {

        @Test
        @DisplayName("retorna 400 em Problem Details quando início não é anterior ao fim")
        void retorna400QuandoInicioNaoEAnteriorAoFim() {
            given()
                    .queryParam("indicadorId", 999999)
                    .queryParam("inicio", "2024-06-10T00:00:00Z")
                    .queryParam("fim", "2024-06-01T00:00:00Z")
                .when().get("/api/v1/cotacoes/serie")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/intervalo-invalido"))
                    .body("title", is("Intervalo inválido"))
                    .body("status", is(400));
        }

        @Test
        @DisplayName("retorna 400 em Problem Details quando o intervalo excede o teto configurado (1825 dias)")
        void retorna400QuandoIntervaloExcedeOTetoConfigurado() {
            given()
                    .queryParam("indicadorId", 999999)
                    .queryParam("inicio", "2015-01-01T00:00:00Z")
                    .queryParam("fim", "2021-06-01T00:00:00Z")
                .when().get("/api/v1/cotacoes/serie")
                .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/intervalo-invalido"))
                    .body("title", is("Intervalo inválido"))
                    .body("status", is(400));
        }

        @Test
        @DisplayName("retorna 400 quando indicadorId não é informado")
        void retorna400QuandoIndicadorIdNaoEInformado() {
            given()
                    .queryParam("inicio", "2024-01-01T00:00:00Z")
                    .queryParam("fim", "2024-01-02T00:00:00Z")
                .when().get("/api/v1/cotacoes/serie")
                .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("retorna 400 quando início não é informado")
        void retorna400QuandoInicioNaoEInformado() {
            given()
                    .queryParam("indicadorId", 999999)
                    .queryParam("fim", "2024-01-02T00:00:00Z")
                .when().get("/api/v1/cotacoes/serie")
                .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("retorna 400 quando fim não é informado")
        void retorna400QuandoFimNaoEInformado() {
            given()
                    .queryParam("indicadorId", 999999)
                    .queryParam("inicio", "2024-01-01T00:00:00Z")
                .when().get("/api/v1/cotacoes/serie")
                .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("retorna 404 em Problem Details quando o indicadorId não existe")
        void retorna404QuandoOIndicadorIdNaoExiste() {
            given()
                    .queryParam("indicadorId", 999999)
                    .queryParam("inicio", "2024-01-01T00:00:00Z")
                    .queryParam("fim", "2024-01-02T00:00:00Z")
                .when().get("/api/v1/cotacoes/serie")
                .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", is("https://api.example.com/errors/recurso-nao-encontrado"))
                    .body("title", is("Recurso não encontrado"))
                    .body("status", is(404));
        }

        @Test
        @DisplayName("usa granularidade DIA como padrão quando o parâmetro não é informado")
        void usaGranularidadeDiaComoPadraoQuandoNaoInformada() {
            var indicador = persistirIndicador("SER1", "Indicador Série Default", FonteDados.LOCAL, true);
            persistirCotacao(indicador, new BigDecimal("1.000000"), Instant.parse("2024-01-01T10:00:00Z"));
            persistirCotacao(indicador, new BigDecimal("2.000000"), Instant.parse("2024-01-02T10:00:00Z"));

            given()
                    .queryParam("indicadorId", indicador.id)
                    .queryParam("inicio", "2024-01-01T00:00:00Z")
                    .queryParam("fim", "2024-01-03T00:00:00Z")
                .when().get("/api/v1/cotacoes/serie")
                .then()
                    .statusCode(200)
                    .body("granularidade", is("DIA"));
        }

        @Test
        @DisplayName("retorna 200 com a estrutura completa da série em granularidade BRUTO")
        void retorna200ComEstruturaCompletaEmGranularidadeBruto() {
            var indicador = persistirIndicador("SER2", "Indicador Série Completa", FonteDados.LOCAL, true);
            persistirCotacao(indicador, new BigDecimal("10.000000"), Instant.parse("2024-02-01T00:00:00Z"));
            persistirCotacao(indicador, new BigDecimal("5.000000"), Instant.parse("2024-02-01T01:00:00Z"));
            persistirCotacao(indicador, new BigDecimal("20.000000"), Instant.parse("2024-02-01T02:00:00Z"));

            var response = given()
                    .queryParam("indicadorId", indicador.id)
                    .queryParam("inicio", "2024-02-01T00:00:00Z")
                    .queryParam("fim", "2024-02-01T03:00:00Z")
                    .queryParam("granularidade", "BRUTO")
                .when().get("/api/v1/cotacoes/serie")
                .then()
                    .statusCode(200)
                    .body("indicador.id", is(indicador.id.intValue()))
                    .body("indicador.codigo", is("SER2"))
                    .body("indicador.nome", is("Indicador Série Completa"))
                    .body("granularidade", is("BRUTO"))
                    .body("pontos", hasSize(3))
                    .body("pontos[0].t", is("2024-02-01T00:00:00Z"))
                    .body("pontos[1].t", is("2024-02-01T01:00:00Z"))
                    .body("pontos[2].t", is("2024-02-01T02:00:00Z"))
                    .extract().response();

            assertEquals(0, new BigDecimal("10.000000").compareTo(new BigDecimal(response.jsonPath().getString("pontos[0].v"))));
            assertEquals(0, new BigDecimal("5.000000").compareTo(new BigDecimal(response.jsonPath().getString("pontos[1].v"))));
            assertEquals(0, new BigDecimal("20.000000").compareTo(new BigDecimal(response.jsonPath().getString("pontos[2].v"))));

            assertEquals(0, new BigDecimal("5.000000").compareTo(new BigDecimal(response.jsonPath().getString("resumo.minimo"))));
            assertEquals(0, new BigDecimal("20.000000").compareTo(new BigDecimal(response.jsonPath().getString("resumo.maximo"))));
            assertEquals(0, new BigDecimal("100.00").compareTo(new BigDecimal(response.jsonPath().getString("resumo.variacaoPercentual"))));
        }
    }
}
