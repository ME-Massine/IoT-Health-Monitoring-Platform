import { Inbox } from "lucide-react";

export function EmptyState({ icon: Icon = Inbox, title, subtitle }) {
  return (
    <div className="empty-state">
      <div className="empty-state__icon">
        <Icon size={28} />
      </div>
      <div className="empty-state__title">{title}</div>
      {subtitle && <div className="empty-state__subtitle">{subtitle}</div>}
    </div>
  );
}
