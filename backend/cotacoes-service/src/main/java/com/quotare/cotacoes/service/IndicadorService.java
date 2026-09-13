package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.AtualizarIndicadorRequest;
import com.quotare.cotacoes.dto.CriarIndicadorRequest;
import com.quotare.cotacoes.dto.IndicadorResponse;
import com.quotare.cotacoes.dto.PaginaResponse;
import com.quotare.cotacoes.mapper.IndicadorMapper;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class IndicadorService {

    @Inject
    IndicadorMapper mapper;

    @Transactional
    public IndicadorResponse criar(CriarIndicadorRequest request) {
        Indicador indicador = mapper.toEntity(request);
        indicador.codigo = indicador.codigo.toUpperCase(Locale.ROOT);
        garantirCodigoDisponivel(indicador.codigo, null);
        indicador.persistAndFlush();
        Indicador.getEntityManager().refresh(indicador);

        return mapper.toResponse(indicador);
    }

    @Transactional
    public IndicadorResponse atualizar(Long id, AtualizarIndicadorRequest request) {
        Indicador indicador = Indicador.<Indicador>findByIdOptional(id).orElseThrow(NotFoundException::new);
        String codigo = request.codigo().toUpperCase(Locale.ROOT);
        garantirCodigoDisponivel(codigo, id);
        mapper.atualizar(request, indicador);
        indicador.codigo = codigo;
        Indicador.getEntityManager().flush();
        Indicador.getEntityManager().refresh(indicador);

        return mapper.toResponse(indicador);
    }

    @Transactional
    public void remover(Long id) {
        Indicador indicador = Indicador.<Indicador>findByIdOptional(id).orElseThrow(NotFoundException::new);
        garantirSemCotacoes(id);
        indicador.delete();
    }

    private void garantirSemCotacoes(Long indicadorId) {
        long cotacoes = Cotacao.count("indicador.id", indicadorId);
        if (cotacoes > 0) {
            throw new ClientErrorException(Response.status(Response.Status.CONFLICT)
                    .type(MediaType.TEXT_PLAIN_TYPE.withCharset("UTF-8"))
                    .entity("Não é possível remover um indicador com cotações associadas")
                    .build());
        }
    }

    private void garantirCodigoDisponivel(String codigo, Long idIgnorado) {
        long existentes = idIgnorado == null
                ? Indicador.count("codigo", codigo)
                : Indicador.count("codigo = ?1 and id <> ?2", codigo, idIgnorado);

        if (existentes > 0) {
            throw new ClientErrorException(Response.status(Response.Status.CONFLICT)
                    .type(MediaType.TEXT_PLAIN_TYPE.withCharset("UTF-8"))
                    .entity("Já existe um indicador com o código " + codigo)
                    .build());
        }
    }

    public IndicadorResponse buscarPorId(Long id) {
        Indicador indicador = Indicador.<Indicador>findByIdOptional(id)
                .orElseThrow(NotFoundException::new);

        return mapper.toResponse(indicador);
    }

    public PaginaResponse<IndicadorResponse> listar(int page, int size, FonteDados fonte, Boolean ativo) {
        var sort = Sort.by("id");

        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (fonte != null) {
            condicoes.add("fonte = :fonte");
            parametros.put("fonte", fonte);
        }
        if (ativo != null) {
            condicoes.add("ativo = :ativo");
            parametros.put("ativo", ativo);
        }

        var query = condicoes.isEmpty()
                ? Indicador.findAll(sort)
                : Indicador.find(String.join(" and ", condicoes), sort, parametros);

        var paginado = query.page(Page.of(page, size));

        var itens = paginado.<Indicador>list().stream()
                .map(mapper::toResponse)
                .toList();

        return new PaginaResponse<>(
                itens,
                page,
                size,
                paginado.count(),
                paginado.pageCount()
        );
    }
}
