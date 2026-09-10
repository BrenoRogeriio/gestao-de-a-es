import { useState } from 'react';
import { ArrowRight, LoaderCircle } from 'lucide-react';
import { navegar, navegarPorLink } from '../../auth/rotas.js';
import { possuiErros, prepararCadastro, validarCadastro } from '../../auth/validacao.js';
import { useAuth } from '../../auth/useAuth.js';
import AuthShell from './AuthShell.jsx';
import PasswordField from './PasswordField.jsx';

export default function CadastroPage({ darkMode, onToggleTheme }) {
    const { cadastrar } = useAuth();
    const [campos, setCampos] = useState({ nome: '', email: '', senha: '', confirmacaoSenha: '' });
    const [erros, setErros] = useState({});
    const [erroGeral, setErroGeral] = useState('');
    const [enviando, setEnviando] = useState(false);

    const alterar = evento => {
        const { name, value } = evento.target;
        setCampos(atuais => ({ ...atuais, [name]: value }));
        setErros(atuais => ({ ...atuais, [name]: undefined }));
        setErroGeral('');
    };

    const enviar = async evento => {
        evento.preventDefault();
        const novosErros = validarCadastro(campos);
        setErros(novosErros);
        if (possuiErros(novosErros)) return;

        setEnviando(true);
        try {
            await cadastrar(prepararCadastro(campos));
            navegar('/', { substituir: true });
        } catch (erro) {
            setErroGeral(erro?.message || 'Não foi possível concluir o cadastro. Tente novamente.');
        } finally {
            setEnviando(false);
        }
    };

    return (
        <AuthShell title="Crie sua conta" description="Comece a organizar seus investimentos." darkMode={darkMode} onToggleTheme={onToggleTheme}>
            {erroGeral && <div className="auth-message auth-message-error" role="alert">{erroGeral}</div>}
            <form className="auth-form" onSubmit={enviar} noValidate>
                <div className="auth-field">
                    <label htmlFor="nome">Nome</label>
                    <input id="nome" name="nome" value={campos.nome} onChange={alterar} autoComplete="name" aria-invalid={Boolean(erros.nome)} aria-describedby={erros.nome ? 'nome-erro' : undefined} required />
                    {erros.nome && <span id="nome-erro" className="field-error" role="alert">{erros.nome}</span>}
                </div>
                <div className="auth-field">
                    <label htmlFor="email">E-mail</label>
                    <input id="email" name="email" type="email" value={campos.email} onChange={alterar} autoComplete="email" inputMode="email" aria-invalid={Boolean(erros.email)} aria-describedby={erros.email ? 'email-erro' : undefined} required />
                    {erros.email && <span id="email-erro" className="field-error" role="alert">{erros.email}</span>}
                </div>
                <PasswordField id="senha" label="Senha" value={campos.senha} onChange={alterar} error={erros.senha} autoComplete="new-password" />
                <PasswordField id="confirmacaoSenha" label="Confirmar senha" value={campos.confirmacaoSenha} onChange={alterar} error={erros.confirmacaoSenha} autoComplete="new-password" />
                <button className="button button-primary auth-submit" type="submit" disabled={enviando}>
                    {enviando ? <LoaderCircle className="spin" size={18} aria-hidden="true" /> : <ArrowRight size={18} aria-hidden="true" />}
                    {enviando ? 'Criando conta…' : 'Criar conta'}
                </button>
            </form>
            <p className="auth-alternative">Já tem uma conta? <a href="/login" onClick={evento => navegarPorLink(evento, '/login')}>Entrar</a></p>
        </AuthShell>
    );
}
