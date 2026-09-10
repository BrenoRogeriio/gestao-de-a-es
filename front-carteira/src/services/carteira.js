import { apiFetch, erroDaResposta } from './api.js';

async function consultarLista(path, signal, request = apiFetch) {
    const resposta = await request(path, { cache: 'no-store', signal });
    if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível carregar os dados da carteira.');
    const dados = await resposta.json();
    if (!Array.isArray(dados)) throw new Error('O servidor retornou dados em um formato inesperado.');
    return dados;
}

export async function consultarCarteira(signal, request = apiFetch) {
    const [resumos, posicoes] = await Promise.all([
        consultarLista('/carteira/resumo', signal, request),
        consultarLista('/carteira/posicao', signal, request)
    ]);
    return { resumos, posicoes };
}

export async function consultarHistorico(signal, request = apiFetch) {
    return consultarLista('/carteira/historico', signal, request);
}

export async function registrarOperacao(tipo, payload, idempotencyKey, request = apiFetch) {
    const endpoint = tipo === 'VENDA' ? '/carteira/vender' : '/carteira/comprar';
    const resposta = await request(endpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify(payload)
    });
    if (!resposta.ok) {
        throw await erroDaResposta(resposta, 'Não foi possível registrar a operação. Revise os dados e tente novamente.');
    }
}

export async function solicitarAtualizacaoCotacao(acaoId, request = apiFetch) {
    const resposta = await request(`/acoes/${acaoId}/atualizar-cotacao`, { method: 'PUT' });
    if (!resposta.ok) {
        throw await erroDaResposta(resposta, 'Não foi possível atualizar a cotação deste ativo.');
    }
    return resposta.json();
}
