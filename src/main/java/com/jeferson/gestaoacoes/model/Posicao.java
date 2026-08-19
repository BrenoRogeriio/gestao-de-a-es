package com.jeferson.gestaoacoes.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "posicoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Posicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Relação 1 para 1: Cada Ação cadastrada terá apenas uma Posição consolidada
    @OneToOne(optional = false)
    @JoinColumn(name = "acao_id", unique = true, nullable = false)
    private Acao acao;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_medio", nullable = false, precision = 19, scale = 4)
    private BigDecimal precoMedio;
}