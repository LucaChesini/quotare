package com.quotare.cotacoes.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "indicador")
public class Indicador extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String codigo;

    public String nome;

    @Enumerated(EnumType.STRING)
    public FonteDados fonte;

    public Boolean ativo = true;

    @Column(name = "criado_em", insertable = false, updatable = false)
    public Instant criadoEm;

    @Column(name = "atualizado_em", insertable = false, updatable = false)
    public Instant atualizadoEm;
}
