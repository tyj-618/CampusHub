interface StatusViewProps {
  loading?: boolean;
  error?: string | null;
}

export function StatusView({ loading, error }: StatusViewProps) {
  if (loading) {
    return <div className="status-box">正在加载内容...</div>;
  }
  if (error) {
    return <div className="status-box error">{error}</div>;
  }
  return null;
}
