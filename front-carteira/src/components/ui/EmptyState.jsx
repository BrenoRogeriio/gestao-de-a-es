import { WalletCards } from 'lucide-react';
import ContentState from './ContentState.jsx';

export default function EmptyState({ title, description, action }) {
    return <ContentState icon={WalletCards} title={title} description={description} action={action} />;
}
