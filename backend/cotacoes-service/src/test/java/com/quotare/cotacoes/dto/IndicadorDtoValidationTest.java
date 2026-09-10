package com.quotare.cotacoes.dto;

import com.quotare.cotacoes.domain.FonteDados;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Validação Bean Validation dos DTOs de Indicador")
class IndicadorDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // --- CriarIndicadorRequest ---

    @Nested
    @DisplayName("CriarIndicadorRequest")
    class CriarIndicadorRequestTest {

        @Test
        @DisplayName("request válido não gera violações")
        void valido() {
            var request = new CriarIndicadorRequest("USD", "Dólar Americano", FonteDados.EXTERNA);

            Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

            assertTrue(violacoes.isEmpty(), "Não deveria haver violações para um request válido");
        }

        @Test
        @DisplayName("acumula todas as violações quando código, nome e fonte são inválidos")
        void acumulaTodasAsViolacoes() {
            var request = new CriarIndicadorRequest("", "", null);

            Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

            assertEquals(4, violacoes.size(),
                    "Esperadas 4 violações: @NotBlank e @Pattern em codigo, @NotBlank em nome, @NotNull em fonte");
        }

        @Nested
        @DisplayName("criação · campo codigo")
        class CampoCodigo {

            @Test
            @DisplayName("rejeita string em branco")
            void rejeitaEmBranco() {
                var request = new CriarIndicadorRequest("", "Dólar Americano", FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Código em branco deveria gerar violação");
            }

            @Test
            @DisplayName("rejeita valor nulo")
            void rejeitaNulo() {
                var request = new CriarIndicadorRequest(null, "Dólar Americano", FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Código nulo deveria gerar violação");
            }

            @Test
            @DisplayName("rejeita mais de 20 caracteres")
            void rejeitaMaisDe20Caracteres() {
                String codigoComVinteEUmCaracteres = "A".repeat(21);
                var request = new CriarIndicadorRequest(codigoComVinteEUmCaracteres, "Dólar Americano", FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Código com 21 caracteres deveria gerar violação");
            }

            @Test
            @DisplayName("aceita exatamente 20 caracteres")
            void aceitaExatamente20Caracteres() {
                String codigoComVinteCaracteres = "A".repeat(20);
                var request = new CriarIndicadorRequest(codigoComVinteCaracteres, "Dólar Americano", FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertTrue(violacoes.isEmpty(), "Código com exatamente 20 caracteres não deveria gerar violação");
            }

            @Test
            @DisplayName("rejeita caractere inválido")
            void rejeitaCaractereInvalido() {
                var request = new CriarIndicadorRequest("US$", "Dólar Americano", FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Código com caractere especial deveria gerar violação");
            }

            @Test
            @DisplayName("aceita código minúsculo")
            void aceitaMinusculo() {
                var request = new CriarIndicadorRequest("usd", "Dólar Americano", FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertTrue(violacoes.isEmpty(), "Código minúsculo não deveria gerar violação nesta etapa");
            }
        }

        @Nested
        @DisplayName("criação · campo nome")
        class CampoNome {

            @Test
            @DisplayName("rejeita string em branco")
            void rejeitaEmBranco() {
                var request = new CriarIndicadorRequest("USD", "", FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Nome em branco deveria gerar violação");
            }

            @Test
            @DisplayName("aceita exatamente 120 caracteres")
            void aceitaExatamente120Caracteres() {
                String nomeComCentoEVinteCaracteres = "A".repeat(120);
                var request = new CriarIndicadorRequest("USD", nomeComCentoEVinteCaracteres, FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertTrue(violacoes.isEmpty(), "Nome com exatamente 120 caracteres não deveria gerar violação");
            }

            @Test
            @DisplayName("rejeita mais de 120 caracteres")
            void rejeitaMaisDe120Caracteres() {
                String nomeComCentoEVinteEUmCaracteres = "A".repeat(121);
                var request = new CriarIndicadorRequest("USD", nomeComCentoEVinteEUmCaracteres, FonteDados.EXTERNA);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Nome com 121 caracteres deveria gerar violação");
            }
        }

        @Nested
        @DisplayName("criação · campo fonte")
        class CampoFonte {

            @Test
            @DisplayName("rejeita valor nulo")
            void rejeitaNula() {
                var request = new CriarIndicadorRequest("USD", "Dólar Americano", null);

                Set<ConstraintViolation<CriarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Fonte nula deveria gerar violação");
            }
        }
    }

    // --- AtualizarIndicadorRequest ---

    @Nested
    @DisplayName("AtualizarIndicadorRequest")
    class AtualizarIndicadorRequestTest {

        @Test
        @DisplayName("request válido não gera violações")
        void valido() {
            var request = new AtualizarIndicadorRequest("USD", "Dólar Americano", FonteDados.EXTERNA, true);

            Set<ConstraintViolation<AtualizarIndicadorRequest>> violacoes = validator.validate(request);

            assertTrue(violacoes.isEmpty(), "Não deveria haver violações para um request válido");
        }

        @Nested
        @DisplayName("atualização · campo codigo")
        class CampoCodigo {

            @Test
            @DisplayName("rejeita string em branco")
            void rejeitaEmBranco() {
                var request = new AtualizarIndicadorRequest("", "Dólar Americano", FonteDados.EXTERNA, true);

                Set<ConstraintViolation<AtualizarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Código em branco deveria gerar violação");
            }

            @Test
            @DisplayName("rejeita caractere inválido")
            void rejeitaCaractereInvalido() {
                var request = new AtualizarIndicadorRequest("US$", "Dólar Americano", FonteDados.EXTERNA, true);

                Set<ConstraintViolation<AtualizarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Código com caractere especial deveria gerar violação");
            }

            @Test
            @DisplayName("rejeita mais de 20 caracteres")
            void rejeitaMaisDe20Caracteres() {
                String codigoComVinteEUmCaracteres = "A".repeat(21);
                var request = new AtualizarIndicadorRequest(codigoComVinteEUmCaracteres, "Dólar Americano", FonteDados.EXTERNA, true);

                Set<ConstraintViolation<AtualizarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Código com 21 caracteres deveria gerar violação");
            }
        }

        @Nested
        @DisplayName("atualização · campo nome")
        class CampoNome {

            @Test
            @DisplayName("rejeita string em branco")
            void rejeitaEmBranco() {
                var request = new AtualizarIndicadorRequest("USD", "", FonteDados.EXTERNA, true);

                Set<ConstraintViolation<AtualizarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Nome em branco deveria gerar violação");
            }
        }

        @Nested
        @DisplayName("atualização · campo fonte")
        class CampoFonte {

            @Test
            @DisplayName("rejeita valor nulo")
            void rejeitaNula() {
                var request = new AtualizarIndicadorRequest("USD", "Dólar Americano", null, true);

                Set<ConstraintViolation<AtualizarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Fonte nula deveria gerar violação");
            }
        }

        @Nested
        @DisplayName("atualização · campo ativo")
        class CampoAtivo {

            @Test
            @DisplayName("rejeita valor nulo")
            void rejeitaNulo() {
                var request = new AtualizarIndicadorRequest("USD", "Dólar Americano", FonteDados.EXTERNA, null);

                Set<ConstraintViolation<AtualizarIndicadorRequest>> violacoes = validator.validate(request);

                assertFalse(violacoes.isEmpty(), "Campo 'ativo' nulo deveria gerar violação");
                assertEquals(1, violacoes.size(), "Apenas a violação de 'ativo' era esperada neste cenário");
            }
        }
    }
}
