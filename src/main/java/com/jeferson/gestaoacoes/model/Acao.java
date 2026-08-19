package com.jeferson.gestaoacoes.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "acoes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_acao_ticker_mercado", columnNames = {"ticker", "mercado"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Acao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(name = "nome_empresa")
    private String nomeEmpresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Mercado mercado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moeda moeda;

    @Column(name = "cotacao_atual", nullable = false, precision = 19, scale = 4)
    private BigDecimal cotacaoAtual;

    @Column(name = "data_hora_cotacao", nullable = false)
    private OffsetDateTime dataHoraCotacao;

    @Column(name = "provedor_origem", length = 100)
    private String provedorOrigem;
}