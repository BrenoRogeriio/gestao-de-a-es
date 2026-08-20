import { useState, useEffect } from 'react';

export default function HomeBroker() {
    const [listaAcoes, setListaAcoes] = useState([]);
    const [listaCorretoras, setListaCorretoras] = useState([]); // <- NOVO: Lista de Corretoras

    const hoje = new Date().toISOString().split('T')[0];

    const [form, setForm] = useState({
        tipo: 'COMPRA',
        tipoAtivo: 'Ações',
        acaoId: '',
        corretoraId: '', // <- NOVO: Agora começa vazio para você selecionar
        data: hoje,
        quantidade: 1,
        preco: 0.00
    });

    useEffect(() => {
        // Busca Ações
        fetch('http://localhost:8080/acoes', { cache: 'no-store' })
            .then(res => res.json())
            .then(dados => {
                const lista = dados.content ? dados.content : dados;
                if (Array.isArray(lista)) setListaAcoes(lista);
            })
            .catch(erro => console.error(erro));

        // Busca Corretoras
        fetch('http://localhost:8080/corretoras', { cache: 'no-store' })
            .then(res => res.json())
            .then(dados => {
                const lista = dados.content ? dados.content : dados;
                if (Array.isArray(lista)) setListaCorretoras(lista);
            })
            .catch(erro => console.error(erro));
    }, []);

    const handleAcaoChange = (e) => {
        const idSelecionado = e.target.value;
        const acaoEncontrada = listaAcoes.find(a => a.id.toString() === idSelecionado);
        setForm({
            ...form,
            acaoId: idSelecionado,
            preco: acaoEncontrada ? acaoEncontrada.cotacaoAtual : 0
        });
    };

    const enviarOperacao = (e) => {
        e.preventDefault();
        if (!form.acaoId) return alert("Selecione um ativo!");
        if (!form.corretoraId) return alert("Selecione uma corretora!");
        if (form.quantidade <= 0) return alert("A quantidade deve ser maior que zero!");

        const endpoint = form.tipo === 'COMPRA' ? '/carteira/comprar' : '/carteira/vender';

        // Tratamento de segurança para o preço (garante que envia com ponto, não vírgula)
        const precoFormatado = parseFloat(form.preco.toString().replace(',', '.'));

        fetch(`http://localhost:8080${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                acaoId: parseInt(form.acaoId, 10),
                corretoraId: parseInt(form.corretoraId, 10),
                quantidade: parseInt(form.quantidade, 10),
                valorUnitario: precoFormatado
            })
        }).then(res => {
            if (res.ok) {
                alert(`✅ Lançamento de ${form.tipo} adicionado com sucesso!`);
                setForm({ ...form, acaoId: '', quantidade: 1, preco: 0 });
            } else {
                alert("❌ Erro: Verifique se você possui saldo/ações suficientes ou revise os dados.");
            }
        }).catch(erro => console.error(erro));
    };

    const valorTotal = (Number(form.quantidade) * Number(form.preco));
    const formatarMoeda = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

    return (
        <div className="card-padrao" style={{ maxWidth: '700px', margin: '0 auto', padding: '0', borderRadius: '16px' }}>

            <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 style={{ margin: 0, color: 'var(--text-main)', fontSize: '18px', fontWeight: '600' }}>Adicionar Lançamento</h3>
            </div>

            <form onSubmit={enviarOperacao} style={{ padding: '24px' }}>

                {/* COMPRA E VENDA */}
                <div style={{ display: 'flex', background: 'var(--hover-row)', borderRadius: '12px', padding: '4px', marginBottom: '24px' }}>
                    <button
                        type="button"
                        onClick={() => setForm({...form, tipo: 'COMPRA'})}
                        style={{ flex: 1, padding: '10px', border: 'none', borderRadius: '8px', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', transition: 'all 0.2s', background: form.tipo === 'COMPRA' ? 'var(--bg-card)' : 'transparent', color: form.tipo === 'COMPRA' ? '#10b981' : 'var(--text-muted)', boxShadow: form.tipo === 'COMPRA' ? '0 2px 4px rgba(0,0,0,0.05)' : 'none' }}>
                        <span>💰</span> Compra
                    </button>

                    <button
                        type="button"
                        onClick={() => setForm({...form, tipo: 'VENDA'})}
                        style={{ flex: 1, padding: '10px', border: 'none', borderRadius: '8px', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', transition: 'all 0.2s', background: form.tipo === 'VENDA' ? 'var(--bg-card)' : 'transparent', color: form.tipo === 'VENDA' ? '#ef4444' : 'var(--text-muted)', boxShadow: form.tipo === 'VENDA' ? '0 2px 4px rgba(0,0,0,0.05)' : 'none' }}>
                        <span>📈</span> Venda
                    </button>
                </div>

                {/* GRID DE CAMPOS RESPONSIVO */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px' }}>

                    <div>
                        <label className="lbl-form">Tipo de ativo</label>
                        <select className="input-moderno" value={form.tipoAtivo} onChange={e => setForm({...form, tipoAtivo: e.target.value})} disabled>
                            <option value="Ações">Ações</option>
                        </select>
                    </div>

                    <div>
                        <label className="lbl-form">Ativo</label>
                        <select className="input-moderno" required value={form.acaoId} onChange={handleAcaoChange}>
                            <option value="" disabled>Selecionar Ação...</option>
                            {listaAcoes.map(acao => (
                                <option key={acao.id} value={acao.id}>{acao.ticker} - {acao.nomeEmpresa}</option>
                            ))}
                        </select>
                    </div>

                    <div style={{ gridColumn: '1 / -1' }}>
                        <label className="lbl-form">Instituição Financeira (Corretora)</label>
                        <select className="input-moderno" required value={form.corretoraId} onChange={e => setForm({...form, corretoraId: e.target.value})}>
                            <option value="" disabled>Selecionar Corretora...</option>
                            {listaCorretoras.map(c => (
                                <option key={c.id} value={c.id}>CNPJ: {c.cnpj} - {c.email}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="lbl-form">Data da transação</label>
                        <input className="input-moderno" type="date" required value={form.data} onChange={e => setForm({...form, data: e.target.value})} />
                    </div>

                    <div>
                        <label className="lbl-form">Quantidade</label>
                        <input className="input-moderno" type="number" required min="1" value={form.quantidade} onChange={e => setForm({...form, quantidade: e.target.value})} />
                    </div>

                    <div>
                        <label className="lbl-form">Preço unitário</label>
                        <div style={{ display: 'flex', border: '1px solid var(--input-border)', borderRadius: '10px', overflow: 'hidden', background: 'var(--input-bg)' }}>
                            <span style={{ padding: '12px 14px', background: 'var(--hover-row)', borderRight: '1px solid var(--input-border)', color: 'var(--text-muted)', fontWeight: '600' }}>R$</span>
                            <input type="number" step="0.01" style={{ flex: 1, border: 'none', padding: '12px 14px', background: 'transparent', color: 'var(--text-main)', outline: 'none', fontSize: '15px' }} value={form.preco} onChange={e => setForm({...form, preco: e.target.value})} />
                        </div>
                    </div>

                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--hover-row)', padding: '20px', borderRadius: '12px', marginTop: '30px', border: '1px solid var(--border-color)' }}>
                    <span style={{ color: 'var(--text-main)', fontWeight: '600', fontSize: '16px' }}>Valor total</span>
                    <span style={{ color: 'var(--text-main)', fontWeight: '700', fontSize: '20px' }}>{formatarMoeda(valorTotal)}</span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '30px' }}>
                    <button type="button" onClick={() => setForm({...form, acaoId: '', corretoraId: '', quantidade: 1, preco: 0})} style={{ background: 'transparent', border: '1px solid var(--border-color)', padding: '12px 24px', borderRadius: '8px', color: 'var(--text-main)', fontWeight: '600', cursor: 'pointer', transition: 'all 0.2s' }}>
                        Cancelar
                    </button>

                    <button type="submit" style={{ background: '#6366f1', border: 'none', padding: '12px 24px', borderRadius: '8px', color: 'white', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)', transition: 'all 0.2s' }}>
                        + Adicionar Lançamento
                    </button>
                </div>

            </form>
        </div>
    );
}