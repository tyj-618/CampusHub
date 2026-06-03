export function formatDateTime(value: string) {
  if (!value) {
    return '';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

export function formatCount(value: number) {
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`;
  }
  return String(value);
}
