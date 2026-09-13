package com.quotare.cotacoes.mapper;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.dto.AtualizarCotacaoRequest;
import com.quotare.cotacoes.dto.CriarCotacaoRequest;
import com.quotare.cotacoes.dto.CotacaoResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)

public interface CotacaoMapper {

    @Mapping(target = "indicadorId", source = "indicador.id")
    CotacaoResponse toResponse(Cotacao cotacao);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "indicador", ignore = true)
    @Mapping(target = "fonte", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    Cotacao toEntity(CriarCotacaoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "indicador", ignore = true)
    @Mapping(target = "fonte", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    void atualizar(AtualizarCotacaoRequest request, @MappingTarget Cotacao cotacao);
}
