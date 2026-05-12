export function formatTimeAgo(dateInput) {
  if (!dateInput) return "—";
  const ms = Date.now() - new Date(dateInput).getTime();
  const sec = Math.floor(ms / 1000);
  if (sec < 10)  return "just now";
  if (sec < 60)  return `${sec}s ago`;
  const min = Math.floor(sec / 60);
  if (min < 60)  return `${min}m ago`;
  const hr  = Math.floor(min / 60);
  if (hr  < 24)  return `${hr}h ago`;
  const day = Math.floor(hr / 24);
  return `${day}d ago`;
}
