export const SITUACAO_TODAS = 'TODAS';
export const SITUACAO_ATIVAS = 'ATIVAS';
export const SITUACAO_OUTRAS = 'OUTRAS';

export const ORDENACOES_CORRETORAS = [
    { value: 'nome-asc', label: 'Nome (A–Z)' },
    { value: 'nome-desc', label: 'Nome (Z–A)' },
    { value: 'localizacao', label: 'Cidade / UF' }
];

export function somenteDigitos(valor, limite) {
    return String(valor ?? '').replace(/\D/g, '').slice(0, limite);
}

export function formatarCnpj(valor) {
    const digitos = somenteDigitos(valor, 14);
    if (digitos.length !== 14) return digitos || '—';
    return digitos.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
}

export function mascararCnpj(valor) {
    return somenteDigitos(valor, 14)
        .replace(/^(\d{2})(\d)/, '$1.$2')
        .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
        .replace(/\.(\d{3})(\d)/, '.$1/$2')
        .replace(/(\d{4})(\d)/, '$1-$2');
}

export function formatarCep(valor) {
    const digitos = somenteDigitos(valor, 8);
    if (digitos.length !== 8) return digitos || '—';
    return digitos.replace(/^(\d{5})(\d{3})$/, '$1-$2');
}

export function mascararCep(valor) {
    return somenteDigitos(valor, 8).replace(/^(\d{5})(\d)/, '$1-$2');
}

export function nomeDaCorretora(corretora = {}) {
    const nomeFantasia = String(corretora.nomeFantasia ?? '').trim();
    const razaoSocial = String(corretora.razaoSocial ?? '').trim();
    return nomeFantasia || razaoSocial || `CNPJ ${formatarCnpj(corretora.cnpj)}`;
}

export function rotuloOpcaoCorretora(corretora = {}) {
    const nome = String(corretora.nomeFantasia ?? '').trim() || String(corretora.razaoSocial ?? '').trim();
    const cnpj = formatarCnpj(corretora.cnpj);
    return nome ? `${nome} — ${cnpj}` : cnpj;
}

export function localizacaoCorretora(corretora = {}) {
    const cidade = String(corretora.cidade ?? '').trim();
    const uf = String(corretora.uf ?? '').trim();
    if (cidade && uf) return `${cidade} • ${uf}`;
    return cidade || uf || 'Não informada';
}

export function enderecoCorretora(corretora = {}) {
    const linha = [corretora.logradouro, corretora.numero].filter(Boolean).join(', ');
    const detalhe = [corretora.bairro, localizacaoCorretora(corretora)].filter(Boolean).join(' • ');
    return [linha, detalhe, corretora.cep ? `CEP ${formatarCep(corretora.cep)}` : ''].filter(Boolean);
}

function textoComparavel(valor) {
    return String(valor ?? '').trim().toLocaleUpperCase('pt-BR');
}

export function situacaoAtiva(valor) {
    return textoComparavel(valor) === 'ATIVA';
}

export function ordenarCorretoras(corretoras = [], ordenacao = 'nome-asc') {
    const compararNome = (a, b) => nomeDaCorretora(a).localeCompare(nomeDaCorretora(b), 'pt-BR');
    return [...corretoras].sort((a, b) => {
        if (ordenacao === 'nome-desc') return compararNome(b, a);
        if (ordenacao === 'localizacao') {
            return localizacaoCorretora(a).localeCompare(localizacaoCorretora(b), 'pt-BR') || compararNome(a, b);
        }
        return compararNome(a, b);
    });
}

export function criarModeloCorretoras({
    loading = false,
    error = false,
    corretoras = [],
    total = 0,
    busca = '',
    situacao = SITUACAO_TODAS,
    ordenacao = 'nome-asc'
} = {}) {
    if (loading) return { estado: 'loading' };
    if (error) return { estado: 'error' };
    const lista = Array.isArray(corretoras) ? corretoras : [];
    if (!lista.length) return { estado: 'empty', corretoras: [], total: 0 };

    const termo = textoComparavel(busca);
    const cnpjBuscado = somenteDigitos(busca);
    const filtradas = lista.filter(corretora => {
        const correspondeSituacao = situacao === SITUACAO_TODAS
            || (situacao === SITUACAO_ATIVAS && situacaoAtiva(corretora.situacaoCadastral))
            || (situacao === SITUACAO_OUTRAS && !situacaoAtiva(corretora.situacaoCadastral));
        const nomes = `${corretora.nomeFantasia ?? ''} ${corretora.razaoSocial ?? ''}`;
        const correspondeBusca = !termo
            || textoComparavel(nomes).includes(termo)
            || Boolean(cnpjBuscado && somenteDigitos(corretora.cnpj).includes(cnpjBuscado));
        return correspondeSituacao && correspondeBusca;
    });

    return {
        estado: filtradas.length ? 'ready' : 'filtered-empty',
        corretoras: ordenarCorretoras(filtradas, ordenacao),
        total: Number(total) || lista.length,
        encontradas: filtradas.length
    };
}

export function resumoCorretoras(modelo, filtrosAtivos = false) {
    const quantidade = filtrosAtivos ? modelo.encontradas : modelo.total;
    if (quantidade === 1) return `1 instituição ${filtrosAtivos ? 'encontrada' : 'cadastrada'}`;
    return `${quantidade} instituições ${filtrosAtivos ? 'encontradas' : 'cadastradas'}`;
}

export function criarFormularioCorretora() {
    return { cnpj: '', cep: '', numero: '', complemento: '', email: '', telefone: '' };
}

export function validarFormularioCorretora(form = {}) {
    const erros = {};
    if (somenteDigitos(form.cnpj).length !== 14) erros.cnpj = 'Informe um CNPJ com 14 dígitos.';
    if (somenteDigitos(form.cep).length !== 8) erros.cep = 'Informe um CEP com 8 dígitos.';
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) erros.email = 'Informe um e-mail válido.';
    if (String(form.numero ?? '').length > 50) erros.numero = 'O número deve ter no máximo 50 caracteres.';
    if (String(form.complemento ?? '').length > 255) erros.complemento = 'O complemento deve ter no máximo 255 caracteres.';
    if (String(form.telefone ?? '').length > 50) erros.telefone = 'O telefone deve ter no máximo 50 caracteres.';
    return erros;
}

export function prepararCadastroCorretora(form = {}) {
    return {
        cnpj: somenteDigitos(form.cnpj, 14),
        cep: somenteDigitos(form.cep, 8),
        numero: String(form.numero ?? '').trim() || null,
        complemento: String(form.complemento ?? '').trim() || null,
        email: String(form.email ?? '').trim() || null,
        telefone: String(form.telefone ?? '').trim() || null
    };
}
