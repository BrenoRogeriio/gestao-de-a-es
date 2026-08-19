import { useState } from 'react';

export default function NovaAcao() {
    const [formAcao, setFormAcao] = useState({ ticker: '', mercado: 'BRASIL' });

    const enviarAcao = (e) => {
        e.preventDefault();
        fetch('http://localhost:8080/acoes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formAcao)
        }).then(res => {
            if (res.ok) {
                alert('✅ Ativo validado e listado no Home Broker!');
                setFormAcao({ ticker: '', mercado: 'BRASIL' });
            } else alert('❌ Ticker não encontrado nas APIs externas.');
        });
    };

    return (
        <div className="card-padrao" style={{ maxWidth: '500px', margin: '0 auto' }}>
            <h3 className="titulo-form">Listar Novo Ativo</h3>
            <form onSubmit={enviarAcao} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                <div>
                    <label className="lbl-form">Símbolo (Ticker):</label>
                    <input className="input-moderno" type="text" required placeholder="Ex: VALE3" value={formAcao.ticker} onChange={e => setFormAcao({...formAcao, ticker: e.target.value.toUpperCase()})} />
                </div>
                <div>
                    <label className="lbl-form">Bolsa de Valores:</label>
                    <select className="input-moderno" required value={formAcao.mercado} onChange={e => setFormAcao({...formAcao, mercado: e.target.value})}>
                        <option value="BRASIL">B3 (Brasil)</option>
                        <option value="ESTADOS_UNIDOS">NYSE/NASDAQ (EUA)</option>
                    </select>
                </div>
                <button className="btn-submit" type="submit" style={{ backgroundColor: '#6366f1' }}>Validar Ativo</button>
            </form>
        </div>
    );
}