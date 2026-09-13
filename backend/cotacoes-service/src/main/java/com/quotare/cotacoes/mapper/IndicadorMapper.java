package com.quotare.cotacoes.mapper;

import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.AtualizarIndicadorRequest;
import com.quotare.cotacoes.dto.CriarIndicadorRequest;
import com.quotare.cotacoes.dto.IndicadorResponse;
import com.quotare.cotacoes.dto.IndicadorResumoResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)

public interface IndicadorMapper {

    IndicadorResponse toResponse(Indicador indicador);

    IndicadorResumoResponse toResumoResponse(Indicador indicador);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Indicador toEntity(CriarIndicadorRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    void atualizar(AtualizarIndicadorRequest request, @MappingTarget Indicador indicador);
}
