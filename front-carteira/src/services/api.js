import { lerTokenJWT, limparTokenJWT } from '../auth/session.js';

const ambiente = import.meta.env ?? {};

const MENSAGENS_POR_TIPO = {
    'https://gestao-acoes.com/erros/resposta-externa-invalida': 'O provedor externo retornou uma resposta inválida.',
    'https://gestao-acoes.com/erros/provedor-externo-indisponivel': 'O provedor externo está temporariamente indisponível.',
    'https://gestao-acoes.com/erros/erro-interno': 'Ocorreu um erro interno. Tente novamente mais tarde.'
};

export const API_URL = (ambiente.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

const ENDPOINTS_AUTENTICACAO_PUBLICOS = new Set(['/auth/login', '/auth/register']);
let tratamentoNaoAutorizado = null;

export function endpointAutenticacaoPublico(path) {
    return ENDPOINTS_AUTENTICACAO_PUBLICOS.has(String(path).split('?')[0]);
}

export function configurarTratamentoNaoAutorizado(tratamento) {
    tratamentoNaoAutorizado = typeof tratamento === 'function' ? tratamento : null;
    return () => {
        if (tratamentoNaoAutorizado === tratamento) tratamentoNaoAutorizado = null;
    };
}

export async function apiFetch(path, options = {}) {
    const endpointPublico = endpointAutenticacaoPublico(path);
    const headers = new Headers(options.headers ?? {});

    if (endpointPublico) {
        headers.delete('Authorization');
    } else {
        const token = lerTokenJWT();
        if (token) headers.set('Authorization', `Bearer ${token}`);
    }

    const resposta = await fetch(`${API_URL}${path}`, { ...options, headers });

    if (resposta.status === 401 && !endpointPublico) {
        limparTokenJWT();
        try {
            tratamentoNaoAutorizado?.();
        } catch {
            // A resposta original ainda deve chegar ao chamador.
        }
    }

    return resposta;
}

function textoSeguro(valor) {
    if (typeof valor !== 'string' || !valor.trim()) return null;
    const texto = valor.trim();
    if (/feign|exception|stack\s*trace|java\.|jdbc|\bsql\b|https?:\/\//i.test(texto)) return null;
    return texto;
}

export async function erroDaResposta(resposta, fallback) {
    let dados;
    try {
        dados = await resposta.json();
    } catch {
        return Object.assign(new Error(fallback), { status: resposta.status });
    }

    const mensagemTipo = MENSAGENS_POR_TIPO[dados?.type];
    const erroCampo = dados?.erros && typeof dados.erros === 'object'
        ? Object.values(dados.erros).map(textoSeguro).find(Boolean)
        : null;
    const mensagem = mensagemTipo || erroCampo || textoSeguro(dados?.detail) || textoSeguro(dados?.message) || fallback;
    return Object.assign(new Error(mensagem), { status: resposta.status, type: dados?.type });
}
