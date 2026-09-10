import { CircleAlert, RefreshCw } from 'lucide-react';
import ContentState from './ContentState.jsx';

export default function ErrorState({ title = 'Não foi possível carregar os dados', description, onRetry }) {
    const action = onRetry ? (
        <button className="button button-secondary" type="button" onClick={onRetry}>
            <RefreshCw size={17} aria-hidden="true" /> Tentar novamente
        </button>
    ) : null;

    return <ContentState className="state-error" icon={CircleAlert} title={title} description={description} action={action} role="alert" />;
}
