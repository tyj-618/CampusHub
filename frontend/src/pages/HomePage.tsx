import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listCategories, listPosts } from '../api/campushub';
import { EmptyState } from '../components/EmptyState';
import { StatusView } from '../components/StatusView';
import type { Category, PostListItem } from '../types/api';
import { formatCount, formatDateTime } from '../utils/format';

export function HomePage() {
  const [posts, setPosts] = useState<PostListItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [keyword, setKeyword] = useState('');
  const [sort, setSort] = useState<'latest' | 'hot'>('latest');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch((err: Error) => setError(err.message));
  }, []);

  useEffect(() => {
    setLoading(true);
    setError(null);
    listPosts({ page: 1, size: 10, categoryId, keyword: keyword || undefined, sort })
      .then((page) => setPosts(page.records))
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [categoryId, keyword, sort]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    setKeyword(String(formData.get('keyword') || '').trim());
  }

  return (
    <div className="page-grid">
      <section className="feed">
        <div className="section-heading">
          <div>
            <p className="eyebrow">校园讨论</p>
            <h1>帖子 / 动态 / 问题</h1>
          </div>
          <Link className="primary-button" to="/publish">发布内容</Link>
        </div>

        <form className="toolbar" onSubmit={handleSearch}>
          <input name="keyword" placeholder="搜索标题或正文" defaultValue={keyword} />
          <select value={sort} onChange={(event) => setSort(event.target.value as 'latest' | 'hot')}>
            <option value="latest">最新</option>
            <option value="hot">热门</option>
          </select>
          <button type="submit">搜索</button>
        </form>

        <div className="category-tabs">
          <button className={!categoryId ? 'active' : ''} onClick={() => setCategoryId(undefined)}>全部</button>
          {categories.map((category) => (
            <button
              key={category.id}
              className={categoryId === category.id ? 'active' : ''}
              onClick={() => setCategoryId(category.id)}
            >
              {category.name}
            </button>
          ))}
        </div>

        <StatusView loading={loading} error={error} />

        {!loading && !error && posts.length === 0 && (
          <EmptyState title="暂无内容" description="换个分类或关键词看看。" />
        )}

        <div className="post-list">
          {posts.map((post) => (
            <article className="post-row" key={post.id}>
              <div className="post-main">
                <Link to={`/posts/${post.id}`} className="post-title">{post.title}</Link>
                <p>{post.summary || '这条帖子暂时没有摘要。'}</p>
                <div className="meta-line">
                  <span>{post.category.name}</span>
                  <span>{post.author.nickname}</span>
                  <span>{formatDateTime(post.createdAt)}</span>
                </div>
              </div>
              <div className="post-stats">
                <span>{formatCount(post.viewCount)} 浏览</span>
                <span>{formatCount(post.likeCount)} 点赞</span>
                <span>{formatCount(post.commentCount)} 评论</span>
              </div>
            </article>
          ))}
        </div>
      </section>

      <aside className="side-panel">
        <h2>社区概览</h2>
        <p>这里聚合课程交流、校园生活、二手闲置、失物招领、活动组队和求助问答。</p>
        <div className="quick-list">
          {categories.slice(0, 6).map((category) => (
            <button key={category.id} onClick={() => setCategoryId(category.id)}>
              {category.name}
            </button>
          ))}
        </div>
      </aside>
    </div>
  );
}
