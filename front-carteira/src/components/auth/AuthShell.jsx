import { CandlestickChart, Moon, Sun } from 'lucide-react';

export default function AuthShell({ title, description, darkMode, onToggleTheme, children }) {
    return (
        <main className="auth-page">
            <section className="auth-intro" aria-label="Gestão de Ações">
                <div className="auth-brand">
                    <span className="brand-mark" aria-hidden="true"><CandlestickChart size={24} /></span>
                    <span><strong>Gestão de Ações</strong><small>Carteira inteligente</small></span>
                </div>
                <div className="auth-intro-copy">
                    <span className="auth-eyebrow">Investimentos sob controle</span>
                    <h1>Decisões melhores começam com uma carteira organizada.</h1>
                    <p>Acompanhe posições, operações e indicadores em um ambiente seguro e objetivo.</p>
                </div>
            </section>

            <section className="auth-panel">
                <button className="icon-button auth-theme" type="button" onClick={onToggleTheme} aria-label={darkMode ? 'Usar tema claro' : 'Usar tema escuro'}>
                    {darkMode ? <Sun size={19} aria-hidden="true" /> : <Moon size={19} aria-hidden="true" />}
                </button>
                <div className="auth-card card">
                    <header className="auth-card-header">
                        <span className="auth-eyebrow">Acesso seguro</span>
                        <h2>{title}</h2>
                        <p>{description}</p>
                    </header>
                    {children}
                </div>
            </section>
        </main>
    );
}
