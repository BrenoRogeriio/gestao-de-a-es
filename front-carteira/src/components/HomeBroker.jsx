import { useState, useEffect } from 'react';

export default function HomeBroker() {
    const [listaAcoes, setListaAcoes] = useState([]);
    const [formOperacao, setFormOperacao] = useState({ tipo: 'COMPRA', acaoId: '', corretoraId: 1, quantidade: '' });

    useEffect(() => {
        fetch('http://localhost:8080/acoes')
            .then(res => res.json())
            .then(dados => {
                if (Array.isArray(dados)) setListaAcoes(dados);
            });
    }, []);

    const enviarOperacao = (e) => {
        e.preventDefault();
        if (!formOperacao.acaoId) return alert("Selecione um ativo!");

        const endpoint = formOperacao.tipo === 'COMPRA' ? '/carteira/comprar' : '/carteira/vender';
        fetch(`http://localhost:8080${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                acaoId: parseInt(formOperacao.acaoId),
                corretoraId: formOperacao.corretoraId,
                quantidade: parseInt(formOperacao.quantidade)
            })
        }).then(res => {
            if (res.ok) {
                alert(`✅ Ordem de ${formOperacao.tipo} executada a preço de mercado!`);
                setFormOperacao({ ...formOperacao, quantidade: '', acaoId: '' });
            } else alert("❌ Erro: Saldo/Quantidade insuficiente.");
        });
    };

    return (
        <div className="card-padrao" style={{ maxWidth: '500px', margin: '0 auto' }}>
            <h3 className="titulo-form">Boleta de Negociação</h3>

            <form onSubmit={enviarOperacao} style={{ display: 'flex', flexDirection: 'column', gap: '24px', marginTop: '20px' }}>
                <div style={{ display: 'flex', background: 'var(--input-bg)', borderRadius: '12px', padding: '6px', border: '1px solid var(--border-color)' }}>
                    <button type="button" onClick={() => setFormOperacao({...formOperacao, tipo: 'COMPRA'})} className={formOperacao.tipo === 'COMPRA' ? 'btn-pill compra' : 'btn-pill inativo'}>Comprar</button>
                    <button type="button" onClick={() => setFormOperacao({...formOperacao, tipo: 'VENDA'})} className={formOperacao.tipo === 'VENDA' ? 'btn-pill venda' : 'btn-pill inativo'}>Vender</button>
                </div>

                <div>
                    <label className="lbl-form">Selecione o Ativo:</label>
                    <select className="input-moderno" required value={formOperacao.acaoId} onChange={e => setFormOperacao({...formOperacao, acaoId: e.target.value})}>
                        <option value="" disabled>Selecione para ver o gráfico invisível...</option>
                        {listaAcoes.map(acao => (
                            <option key={acao.id} value={acao.id}>{acao.ticker} - {acao.nomeEmpresa}</option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="lbl-form">Quantidade de Lotes/Ações:</label>
                    <input className="input-moderno" type="number" required min="1" placeholder="Ex: 100" value={formOperacao.quantidade} onChange={e => setFormOperacao({...formOperacao, quantidade: e.target.value})} />
                </div>

                <button className="btn-submit" type="submit" style={{ backgroundColor: formOperacao.tipo === 'COMPRA' ? '#10b981' : '#ef4444' }}>
                    Enviar Ordem de {formOperacao.tipo}
                </button>
            </form>
        </div>
    );
}