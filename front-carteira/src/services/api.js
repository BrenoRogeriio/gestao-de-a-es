const ambiente = import.meta.env ?? {};

const MENSAGENS_POR_TIPO = {
    'https://gestao-acoes.com/erros/resposta-externa-invalida': 'O provedor externo retornou uma resposta inválida.',
    'https://gestao-acoes.com/erros/provedor-externo-indisponivel': 'O provedor externo está temporariamente indisponível.',
    'https://gestao-acoes.com/erros/erro-interno': 'Ocorreu um erro interno. Tente novamente mais tarde.'
};

export const API_URL = (ambiente.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

export function apiFetch(path, options) {
    return fetch(`${API_URL}${path}`, options);
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
