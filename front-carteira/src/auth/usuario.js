export function identidadeUsuario(usuario) {
    const nome = typeof usuario?.nome === 'string' && usuario.nome.trim() ? usuario.nome.trim() : 'Investidor';
    const email = typeof usuario?.email === 'string' ? usuario.email.trim() : '';
    return { nome, email };
}

