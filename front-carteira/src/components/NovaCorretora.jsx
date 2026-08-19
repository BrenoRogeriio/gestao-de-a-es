import { useState } from 'react';

export default function NovaCorretora() {
    const [formCorretora, setFormCorretora] = useState({ cnpj: '', cep: '', numero: '', complemento: '', email: '', telefone: '' });

    const enviarCorretora = (e) => {
        e.preventDefault();
        fetch('http://localhost:8080/corretoras', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formCorretora)
        }).then(res => {
            if (res.ok) {
                alert('✅ Instituição homologada pela CVM/Receita Federal!');
                setFormCorretora({ cnpj: '', cep: '', numero: '', complemento: '', email: '', telefone: '' });
            } else alert('❌ Erro nos dados institucionais.');
        });
    };

    return (
        <div className="card-padrao" style={{ maxWidth: '700px', margin: '0 auto' }}>
            <h3 className="titulo-form">Homologar Corretora</h3>
            <form onSubmit={enviarCorretora} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                    <div><label className="lbl-form">CNPJ:</label><input className="input-moderno" required value={formCorretora.cnpj} onChange={e => setFormCorretora({...formCorretora, cnpj: e.target.value})} /></div>
                    <div><label className="lbl-form">CEP:</label><input className="input-moderno" required value={formCorretora.cep} onChange={e => setFormCorretora({...formCorretora, cep: e.target.value})} /></div>
                    <div><label className="lbl-form">Número:</label><input className="input-moderno" required value={formCorretora.numero} onChange={e => setFormCorretora({...formCorretora, numero: e.target.value})} /></div>
                    <div><label className="lbl-form">Complemento:</label><input className="input-moderno" value={formCorretora.complemento} onChange={e => setFormCorretora({...formCorretora, complemento: e.target.value})} /></div>
                    <div><label className="lbl-form">Email Institucional:</label><input className="input-moderno" type="email" required value={formCorretora.email} onChange={e => setFormCorretora({...formCorretora, email: e.target.value})} /></div>
                    <div><label className="lbl-form">Telefone de Contato:</label><input className="input-moderno" required value={formCorretora.telefone} onChange={e => setFormCorretora({...formCorretora, telefone: e.target.value})} /></div>
                </div>
                <button className="btn-submit" type="submit" style={{ backgroundColor: '#6366f1' }}>Autorizar Instituição</button>
            </form>
        </div>
    );
}