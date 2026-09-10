import { LoaderCircle } from 'lucide-react';
import ContentState from './ContentState.jsx';

export default function LoadingState({ title = 'Carregando dados', description = 'Aguarde enquanto buscamos as informações mais recentes.' }) {
    return <ContentState className="state-loading" icon={LoaderCircle} title={title} description={description} ariaLive="polite" />;
}
