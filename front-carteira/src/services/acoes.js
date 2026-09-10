import { apiFetch, erroDaResposta } from './api.js';

export async function consultarAcoes(signal, request = apiFetch) {
    const resposta = await request('/acoes?size=200&sort=ticker,asc', { cache: 'no-store', signal });
    if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível carregar os ativos cadastrados.');
    const dados = await resposta.json();
    const acoes = Array.isArray(dados) ? dados : dados?.content;
    if (!Array.isArray(acoes)) throw new Error('O servidor retornou os ativos em um formato inesperado.');
    return {
        acoes,
        total: Number.isFinite(Number(dados?.totalElements)) ? Number(dados.totalElements) : acoes.length
    };
}

export async function cadastrarAcao(payload, request = apiFetch) {
    const resposta = await request('/acoes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível validar esse ativo.');
    return resposta.json();
}

export async function atualizarCotacaoAcao(id, request = apiFetch) {
    const resposta = await request(`/acoes/${id}/atualizar-cotacao`, { method: 'PUT' });
    if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível atualizar a cotação deste ativo.');
    return resposta.json();
}
