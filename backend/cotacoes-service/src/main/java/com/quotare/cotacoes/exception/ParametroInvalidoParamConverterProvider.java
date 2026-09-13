package com.quotare.cotacoes.exception;

import com.quotare.cotacoes.domain.FonteDados;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class ParametroInvalidoParamConverterProvider implements ParamConverterProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType == Long.class) {
            return (ParamConverter<T>) new LongParamConverter(nomeDoParametro(annotations));
        }

        if (rawType == FonteDados.class) {
            return (ParamConverter<T>) new FonteDadosParamConverter(nomeDoParametro(annotations));
        }

        return null;
    }

    private String nomeDoParametro(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof PathParam pathParam) {
                return pathParam.value();
            }
            if (annotation instanceof QueryParam queryParam) {
                return queryParam.value();
            }
        }
        return "desconhecido";
    }

    private record LongParamConverter(String nomeParametro) implements ParamConverter<Long> {

        @Override
        public Long fromString(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new ParametroInvalidoException(
                        "O valor '" + value + "' não é válido para o parâmetro '" + nomeParametro + "'.");
            }
        }

        @Override
        public String toString(Long value) {
            return value == null ? null : value.toString();
        }
    }

    private record FonteDadosParamConverter(String nomeParametro) implements ParamConverter<FonteDados> {

        @Override
        public FonteDados fromString(String value) {
            if (value == null) {
                return null;
            }
            try {
                return FonteDados.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new ParametroInvalidoException(
                        "O valor '" + value + "' não é válido para o parâmetro '" + nomeParametro + "'.");
            }
        }

        @Override
        public String toString(FonteDados value) {
            return value == null ? null : value.name();
        }
    }
}
