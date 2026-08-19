import { useEffect, useState } from 'react';

export default function Carteira() {
    const [posicoes, setPosicoes] = useState([]);
    const [carregando, setCarregando] = useState(true);

    useEffect(() => {
        fetch('http://localhost:8080/carteira/posicao')
            .then(res => res.json())
            .then(dados => {
                if (Array.isArray(dados)) setPosicoes(dados);
                setCarregando(false);
            }).catch(erro => console.error(erro));
    }, []);

    const patrimonioTotal = posicoes.reduce((acc, pos) => acc + pos.saldoTotalAtual, 0);
    const custoTotal = posicoes.reduce((acc, pos) => acc + (pos.precoMedio * pos.quantidade), 0);
    const rentabilidadeGeral = custoTotal > 0 ? ((patrimonioTotal / custoTotal) - 1) * 100 : 0;
    const formatarMoeda = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

    return (
        <>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px', marginBottom: '40px' }}>
                <div style={{ background: 'linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%)', padding: '24px', borderRadius: '16px', color: 'white', boxShadow: '0 10px 25px -5px rgba(59, 130, 246, 0.4)' }}>
                    <p className="lbl-card" style={{ color: 'rgba(255,255,255,0.8)' }}>Patrimônio Total</p>
                    <h2 style={{ margin: '10px 0 0 0', fontSize: '36px' }}>{formatarMoeda(patrimonioTotal)}</h2>
                </div>
                <div className="card-padrao">
                    <p className="lbl-card">Rentabilidade Geral</p>
                    <h2 style={{ margin: '10px 0 0 0', fontSize: '36px', color: rentabilidadeGeral >= 0 ? '#10b981' : '#ef4444' }}>
                        {rentabilidadeGeral > 0 ? '+' : ''}{rentabilidadeGeral.toFixed(2)}%
                    </h2>
                </div>
            </div>

            <div className="card-padrao">
                <h3 style={{ margin: '0 0 20px 0', color: 'var(--text-main)' }}>Meus Ativos</h3>
                {carregando ? <p style={{ color: 'var(--text-muted)' }}>Sincronizando com a Bolsa... ⏳</p> : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                            <tr>
                                <th style={{ textAlign: 'left' }}>Ativo</th>
                                <th style={{ textAlign: 'right' }}>Qtd.</th>
                                <th style={{ textAlign: 'right' }}>Preço Médio</th>
                                <th style={{ textAlign: 'right' }}>Cotação Atual</th>
                                <th style={{ textAlign: 'right' }}>Rentabilidade</th>
                                <th style={{ textAlign: 'right' }}>Saldo Total</th>
                            </tr>
                            </thead>
                            <tbody>
                            {posicoes.map((p, i) => (
                                <tr key={i} className="tr-hover">
                                    <td style={{ fontWeight: '600' }}>
                                        <span style={{ color: '#6366f1', marginRight: '8px' }}>●</span>{p.ticker}
                                    </td>
                                    <td style={{ textAlign: 'right', fontWeight: '500' }}>{p.quantidade}</td>
                                    <td style={{ textAlign: 'right' }}>{formatarMoeda(p.precoMedio)}</td>
                                    <td style={{ textAlign: 'right', fontWeight: '600' }}>{formatarMoeda(p.cotacaoAtual)}</td>
                                    <td style={{ textAlign: 'right', color: p.rentabilidadePercentual >= 0 ? '#10b981' : '#ef4444', fontWeight: 'bold' }}>
                                        {p.rentabilidadePercentual > 0 ? '↑ ' : '↓ '}{p.rentabilidadePercentual.toFixed(2)}%
                                    </td>
                                    <td style={{ textAlign: 'right', fontWeight: 'bold' }}>{formatarMoeda(p.saldoTotalAtual)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </>
    );
}