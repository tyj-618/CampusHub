import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/campushub';
import { saveToken } from '../api/http';

export function LoginPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const username = String(formData.get('username') || '').trim();
    const password = String(formData.get('password') || '');

    setLoading(true);
    setError(null);
    try {
      const result = await login(username, password);
      saveToken(result.token);
      navigate('/');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="form-panel">
        <p className="eyebrow">欢迎回来</p>
        <h1>登录 CampusHub</h1>
        {error && <div className="inline-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <label>
            用户名
            <input name="username" placeholder="alice" required />
          </label>
          <label>
            密码
            <input name="password" type="password" placeholder="至少 6 位" required />
          </label>
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
      </div>
    </section>
  );
}
