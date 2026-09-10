const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validarLogin({ email = '', senha = '' } = {}) {
    const erros = {};
    if (!email.trim()) erros.email = 'Informe seu e-mail.';
    else if (!EMAIL_VALIDO.test(email.trim())) erros.email = 'Informe um e-mail válido.';
    if (!senha) erros.senha = 'Informe sua senha.';
    return erros;
}

export function validarCadastro({ nome = '', email = '', senha = '', confirmacaoSenha = '' } = {}) {
    const erros = validarLogin({ email, senha });
    if (!nome.trim()) erros.nome = 'Informe seu nome.';
    if (senha && senha.length < 8) erros.senha = 'A senha deve ter pelo menos 8 caracteres.';
    if (!confirmacaoSenha) erros.confirmacaoSenha = 'Confirme sua senha.';
    else if (senha !== confirmacaoSenha) erros.confirmacaoSenha = 'As senhas não coincidem.';
    return erros;
}

export function prepararLogin({ email, senha }) {
    return { email: email.trim().toLowerCase(), senha };
}

export function prepararCadastro({ nome, email, senha }) {
    return { nome: nome.trim(), email: email.trim().toLowerCase(), senha };
}

export function possuiErros(erros) {
    return Object.keys(erros).length > 0;
}

