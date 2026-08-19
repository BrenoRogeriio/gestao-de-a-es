package com.jeferson.gestaoacoes.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_transacao", nullable = false, length = 10)
    private TipoTransacao tipoTransacao;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorUnitario;

    @Column(name = "data_hora_transacao", nullable = false)
    private OffsetDateTime dataHoraTransacao;
}