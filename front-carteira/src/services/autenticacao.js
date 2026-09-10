import { apiFetch, erroDaResposta } from './api.js';

async function lerAutenticacao(resposta, fallback) {
    if (!resposta.ok) throw await erroDaResposta(resposta, fallback);

    const dados = await resposta.json();
    if (!dados?.token || !dados?.usuario) {
        throw new Error('A resposta de autenticação recebida é inválida.');
    }
    return dados;
}

export async function autenticar(credenciais, request = apiFetch) {
    const resposta = await request('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credenciais)
    });
    return lerAutenticacao(resposta, 'Não foi possível entrar. Verifique seus dados e tente novamente.');
}

export async function cadastrarUsuario(cadastro, request = apiFetch) {
    const resposta = await request('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cadastro)
    });
    return lerAutenticacao(resposta, 'Não foi possível concluir o cadastro. Tente novamente.');
}

export async function obterUsuarioAutenticado(signal, request = apiFetch) {
    const resposta = await request('/auth/me', { signal, cache: 'no-store' });
    if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível validar sua sessão.');
    return resposta.json();
}
