export const CHAVE_TOKEN_SESSAO = 'gestao-acoes:jwt';

function armazenamentoSessao() {
    try {
        return globalThis.sessionStorage ?? null;
    } catch {
        return null;
    }
}

export function lerTokenJWT() {
    try {
        const token = armazenamentoSessao()?.getItem(CHAVE_TOKEN_SESSAO);
        return typeof token === 'string' && token.trim() ? token : null;
    } catch {
        return null;
    }
}

export function salvarTokenJWT(token) {
    if (typeof token !== 'string' || !token.trim()) {
        throw new Error('A autenticação retornou uma sessão inválida.');
    }
    armazenamentoSessao()?.setItem(CHAVE_TOKEN_SESSAO, token);
}

export function limparTokenJWT() {
    try {
        armazenamentoSessao()?.removeItem(CHAVE_TOKEN_SESSAO);
    } catch {
        // A limpeza em memória no AuthProvider ainda encerra a sessão atual.
    }
}

