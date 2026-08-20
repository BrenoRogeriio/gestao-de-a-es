import { useEffect, useState } from 'react';

export default function Carteira({ setAbaAtiva }) {
    const [posicoes, setPosicoes] = useState([]);
    const [historico, setHistorico] = useState([]);
    const [abaInterna, setAbaInterna] = useState('resumo');

    useEffect(() => {
        fetch('http://localhost:8080/carteira/posicao', { cache: 'no-store' })
            .then(res => res.json())
            .then(dados => { if (Array.isArray(dados)) setPosicoes(dados); })
            .catch(erro => console.error(erro));

        fetch('http://localhost:8080/carteira/historico', { cache: 'no-store' })
            .then(res => res.json())
            .then(dados => { if (Array.isArray(dados)) setHistorico(dados); })
            .catch(erro => console.error(erro));
    }, []);

    // ==========================================
    // MATEMÁTICA FINANCEIRA (LUCRO TOTAL REAL)
    // ==========================================

    // 1. Patrimônio Total (Quanto valem as ações que você tem HOJE)
    const patrimonioTotal = posicoes.reduce((acc, pos) => acc + pos.saldoTotalAtual, 0);

    // 2. O que você já tirou do próprio bolso (Total de Compras)
    const totalGastoCompras = historico
        .filter(h => h.tipo === 'COMPRA')
        .reduce((acc, h) => acc + h.valorTotal, 0);

    // 3. O que você já botou no bolso de volta (Total de Vendas)
    const totalRecebidoVendas = historico
        .filter(h => h.tipo === 'VENDA')
        .reduce((acc, h) => acc + h.valorTotal, 0);

    // 4. O LUCRO TOTAL VERDADEIRO (Realizado + Não Realizado)
    const lucroTotal = patrimonioTotal + totalRecebidoVendas - totalGastoCompras;

    // 5. Custo das ações que ainda estão na carteira
    const valorInvestido = posicoes.reduce((acc, pos) => acc + (pos.precoMedio * pos.quantidade), 0);

    // Rentabilidade Geral (%)
    const rentabilidade = totalGastoCompras > 0 ? (lucroTotal / totalGastoCompras) * 100 : 0;

    // ==========================================
    // FORMATADORES DE TEXTO
    // ==========================================
    const formatMoeda = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v || 0);

    const formatarData = (dataIso) => {
        if (!dataIso) return '-';
        const data = new Date(dataIso);
        return data.toLocaleDateString('pt-BR') + ' ' + data.toLocaleTimeString('pt-BR', {hour: '2-digit', minute:'2-digit'});
    };

    // Estilos das Abas
    const tabAtivaStyle = { color: 'var(--text-main)', borderBottom: '2px solid var(--text-main)', paddingBottom: '12px', marginBottom: '-13px', cursor: 'pointer', fontWeight: '600', transition: 'all 0.2s', whiteSpace: 'nowrap' };
    const tabInativaStyle = { color: 'var(--text-muted)', cursor: 'pointer', paddingBottom: '12px', marginBottom: '-13px', fontWeight: '500', transition: 'all 0.2s', whiteSpace: 'nowrap' };

    return (
        <div style={{ width: '100%', margin: '0' }}>

            {/* MENU SUPERIOR */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '20px', borderBottom: '1px solid var(--border-color)', paddingBottom: '12px', marginBottom: '24px' }}>

                <div style={{ display: 'flex', gap: '24px', fontSize: '14px', flexWrap: 'wrap' }}>
                    <span onClick={() => setAbaInterna('resumo')} style={abaInterna === 'resumo' ? tabAtivaStyle : tabInativaStyle}>✓ Resumo</span>
                    <span onClick={() => setAbaInterna('posicoes')} style={abaInterna === 'posicoes' ? tabAtivaStyle : tabInativaStyle}>⊞ Posições (Carteira)</span>
                    <span onClick={() => setAbaInterna('historico')} style={abaInterna === 'historico' ? tabAtivaStyle : tabInativaStyle}>📄 Histórico (Extrato)</span>
                </div>

                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                    <button onClick={() => setAbaAtiva('operacoes')} style={{ background: '#6366f1', border: 'none', padding: '8px 16px', borderRadius: '8px', color: 'white', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        + Adicionar Lançamento
                    </button>
                </div>
            </div>

            {/* TELA 1: RESUMO (DASHBOARD) */}
            {abaInterna === 'resumo' && (
                <>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px', marginBottom: '24px' }}>
                        <div className="card-padrao" style={{ padding: '20px' }}>
                            <div style={{ color: 'var(--text-muted)', fontSize: '13px', fontWeight: '600', marginBottom: '8px' }}>💰 Patrimônio total</div>
                            <h2 style={{ margin: 0, fontSize: '24px', color: 'var(--text-main)' }}>{formatMoeda(patrimonioTotal)}</h2>
                            <div style={{ marginTop: '16px', fontSize: '12px', color: 'var(--text-muted)' }}>Valor investido na carteira <br/><strong style={{ color: 'var(--text-main)' }}>{formatMoeda(valorInvestido)}</strong></div>
                        </div>

                        <div className="card-padrao" style={{ padding: '20px' }}>
                            <div style={{ color: 'var(--text-muted)', fontSize: '13px', fontWeight: '600', marginBottom: '8px' }}>💲 Lucro total</div>
                            <h2 style={{ margin: 0, fontSize: '24px', color: lucroTotal >= 0 ? 'var(--primary)' : '#ef4444' }}>{formatMoeda(lucroTotal)}</h2>
                            <div style={{ marginTop: '16px', fontSize: '12px', color: 'var(--text-muted)' }}>Ganho de Capital Realizado + Flutuante</div>
                        </div>

                        <div className="card-padrao" style={{ padding: '20px' }}>
                            <div style={{ color: 'var(--text-muted)', fontSize: '13px', fontWeight: '600', marginBottom: '8px' }}>📈 Rentabilidade Histórica</div>
                            <h2 style={{ margin: 0, fontSize: '24px', color: rentabilidade >= 0 ? 'var(--primary)' : '#ef4444' }}>{rentabilidade.toFixed(2)}%</h2>
                        </div>
                    </div>
                </>
            )}

            {/* TELA 2: POSIÇÕES (CARTEIRA) */}
            {abaInterna === 'posicoes' && (
                <div className="card-padrao">
                    <h3 style={{ margin: '0 0 20px 0', color: 'var(--text-main)' }}>Meus Ativos Detalhados</h3>
                    {posicoes.length === 0 ? (
                        <p style={{ color: 'var(--text-muted)' }}>Você não possui ações na carteira.</p>
                    ) : (
                        <div style={{ overflowX: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: '700px' }}>
                                <thead>
                                <tr>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Ativo</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Qtd</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Preço Médio (PM)</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Cotação Atual</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Rentabilidade</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Saldo Total</th>
                                </tr>
                                </thead>
                                <tbody>
                                {posicoes.map((p, i) => (
                                    <tr key={i} className="tr-hover">
                                        <td style={{ padding: '16px 12px', fontWeight: '600' }}><span style={{ color: '#6366f1', marginRight: '8px' }}>●</span>{p.ticker}</td>
                                        <td style={{ padding: '16px 12px' }}>{p.quantidade}</td>
                                        <td style={{ padding: '16px 12px' }}>{formatMoeda(p.precoMedio)}</td>
                                        <td style={{ padding: '16px 12px' }}>{formatMoeda(p.cotacaoAtual)}</td>
                                        <td style={{ padding: '16px 12px', color: p.rentabilidadePercentual >= 0 ? 'var(--primary)' : '#ef4444', fontWeight: 'bold' }}>
                                            {p.rentabilidadePercentual > 0 ? '↑ ' : '↓ '}{p.rentabilidadePercentual.toFixed(2)}%
                                        </td>
                                        <td style={{ padding: '16px 12px', fontWeight: 'bold' }}>{formatMoeda(p.saldoTotalAtual)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}

            {/* TELA 3: HISTÓRICO (EXTRATO) */}
            {abaInterna === 'historico' && (
                <div className="card-padrao">
                    <h3 style={{ margin: '0 0 20px 0', color: 'var(--text-main)' }}>Histórico de Transações</h3>
                    {historico.length === 0 ? (
                        <p style={{ color: 'var(--text-muted)' }}>Nenhuma transação registrada ainda.</p>
                    ) : (
                        <div style={{ overflowX: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: '700px' }}>
                                <thead>
                                <tr>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Data e Hora</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Operação</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Ativo</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Qtd</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Preço Executado</th>
                                    <th style={{ padding: '12px', borderBottom: '2px solid var(--border-color)', color: 'var(--text-muted)' }}>Volume Total</th>
                                </tr>
                                </thead>
                                <tbody>
                                {historico.map((h, i) => (
                                    <tr key={i} className="tr-hover" style={{ borderBottom: '1px solid var(--hover-row)' }}>
                                        <td style={{ padding: '16px 12px', fontSize: '13px', color: 'var(--text-muted)' }}>{formatarData(h.data)}</td>
                                        <td style={{ padding: '16px 12px', fontWeight: 'bold', color: h.tipo === 'COMPRA' ? '#10b981' : '#ef4444' }}>
                                            {h.tipo}
                                        </td>
                                        <td style={{ padding: '16px 12px', fontWeight: '600' }}>{h.ticker}</td>
                                        <td style={{ padding: '16px 12px' }}>{h.quantidade}</td>
                                        <td style={{ padding: '16px 12px' }}>{formatMoeda(h.valorUnitario)}</td>
                                        <td style={{ padding: '16px 12px', fontWeight: 'bold' }}>{formatMoeda(h.valorTotal)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}

        </div>
    );
}