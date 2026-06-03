import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { createComment, getPostDetail, likePost, listComments, unlikePost } from '../api/campushub';
import { getToken } from '../api/http';
import { EmptyState } from '../components/EmptyState';
import { StatusView } from '../components/StatusView';
import type { CommentItem, PostDetail } from '../types/api';
import { formatDateTime } from '../utils/format';

export function PostDetailPage() {
  const { postId } = useParams();
  const numericPostId = Number(postId);
  const [post, setPost] = useState<PostDetail | null>(null);
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [commentError, setCommentError] = useState<string | null>(null);

  useEffect(() => {
    if (!numericPostId) {
      setError('帖子 ID 无效');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    Promise.all([getPostDetail(numericPostId), listComments(numericPostId)])
      .then(([detail, commentPage]) => {
        setPost(detail);
        setComments(commentPage.records);
      })
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [numericPostId]);

  async function handleLike() {
    if (!post) {
      return;
    }
    if (!getToken()) {
      setCommentError('请先登录后再点赞。');
      return;
    }
    try {
      const result = post.liked ? await unlikePost(post.id) : await likePost(post.id);
      setPost({ ...post, liked: result.liked, likeCount: result.likeCount });
      setCommentError(null);
    } catch (err) {
      setCommentError((err as Error).message);
    }
  }

  async function handleComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!post) {
      return;
    }
    if (!getToken()) {
      setCommentError('请先登录后再评论。');
      return;
    }
    const form = event.currentTarget;
    const formData = new FormData(form);
    const content = String(formData.get('content') || '').trim();
    if (!content) {
      setCommentError('评论内容不能为空。');
      return;
    }
    try {
      await createComment(post.id, content);
      const nextComments = await listComments(post.id);
      setComments(nextComments.records);
      setPost({ ...post, commentCount: post.commentCount + 1 });
      form.reset();
      setCommentError(null);
    } catch (err) {
      setCommentError((err as Error).message);
    }
  }

  return (
    <section className="detail-page">
      <Link className="back-link" to="/">返回首页</Link>
      <StatusView loading={loading} error={error} />

      {!loading && !error && post && (
        <>
          <article className="detail-card">
            <div className="meta-line">
              <span>{post.category.name}</span>
              <span>{post.author.nickname}</span>
              <span>{formatDateTime(post.createdAt)}</span>
            </div>
            <h1>{post.title}</h1>
            <p className="post-content">{post.content}</p>
            <div className="detail-actions">
              <button className={post.liked ? 'active' : ''} onClick={handleLike}>
                {post.liked ? '已点赞' : '点赞'} · {post.likeCount}
              </button>
              <span>{post.viewCount} 浏览</span>
              <span>{post.commentCount} 评论</span>
            </div>
          </article>

          <section className="comments-section">
            <h2>评论</h2>
            {commentError && <div className="inline-error">{commentError}</div>}
            <form className="comment-form" onSubmit={handleComment}>
              <textarea name="content" rows={3} placeholder="写下你的想法" />
              <button type="submit">发表评论</button>
            </form>
            {comments.length === 0 ? (
              <EmptyState title="暂无评论" description="成为第一个回复的人。" />
            ) : (
              <div className="comment-list">
                {comments.map((comment) => (
                  <article className="comment-item" key={comment.id}>
                    <div className="meta-line">
                      <span>{comment.author.nickname}</span>
                      <span>{formatDateTime(comment.createdAt)}</span>
                    </div>
                    <p>{comment.content}</p>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </section>
  );
}
