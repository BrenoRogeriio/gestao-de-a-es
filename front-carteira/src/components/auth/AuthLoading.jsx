import { CandlestickChart, LoaderCircle } from 'lucide-react';

export default function AuthLoading() {
    return (
        <main className="auth-loading" aria-busy="true" aria-live="polite">
            <span className="brand-mark" aria-hidden="true"><CandlestickChart size={24} /></span>
            <LoaderCircle className="spin" size={22} aria-hidden="true" />
            <p>Validando sua sessão…</p>
        </main>
    );
}
