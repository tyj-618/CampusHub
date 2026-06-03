import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createPost, listCategories } from '../api/campushub';
import { getToken } from '../api/http';
import type { Category } from '../types/api';

export function PublishPage() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch((err: Error) => setError(err.message));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!getToken()) {
      setError('请先登录后再发布。');
      return;
    }
    const formData = new FormData(event.currentTarget);
    const categoryId = Number(formData.get('categoryId'));
    const title = String(formData.get('title') || '').trim();
    const content = String(formData.get('content') || '').trim();

    setLoading(true);
    setError(null);
    try {
      const result = await createPost(categoryId, title, content);
      navigate(`/posts/${result.postId}`);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="publish-page">
      <div className="form-panel wide">
        <p className="eyebrow">创建内容</p>
        <h1>发布新帖子</h1>
        {error && <div className="inline-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <label>
            分类
            <select name="categoryId" required>
              <option value="">请选择分类</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </label>
          <label>
            标题
            <input name="title" maxLength={100} placeholder="例如：高数复习资料怎么整理？" required />
          </label>
          <label>
            正文
            <textarea name="content" maxLength={5000} rows={10} placeholder="写下你的问题、经验或校园动态。" required />
          </label>
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? '发布中...' : '发布帖子'}
          </button>
        </form>
      </div>
    </section>
  );
}
