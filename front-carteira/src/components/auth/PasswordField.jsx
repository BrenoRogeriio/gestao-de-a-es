import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

export default function PasswordField({ id, label, value, onChange, error, autoComplete }) {
    const [visivel, setVisivel] = useState(false);
    const erroId = error ? `${id}-erro` : undefined;

    return (
        <div className="auth-field">
            <label htmlFor={id}>{label}</label>
            <div className="password-control">
                <input id={id} name={id} type={visivel ? 'text' : 'password'} value={value} onChange={onChange} autoComplete={autoComplete} aria-invalid={Boolean(error)} aria-describedby={erroId} required />
                <button type="button" onClick={() => setVisivel(atual => !atual)} aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}>
                    {visivel ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
                </button>
            </div>
            {error && <span id={erroId} className="field-error" role="alert">{error}</span>}
        </div>
    );
}
