import { http, unwrap } from './http';
import type {
  Category,
  CommentItem,
  CreatePostResponse,
  LikeResponse,
  LoginResponse,
  PageResponse,
  PostDetail,
  PostListItem
} from '../types/api';

export interface ListPostsParams {
  page?: number;
  size?: number;
  categoryId?: number;
  keyword?: string;
  sort?: 'latest' | 'hot';
}

export function listPosts(params: ListPostsParams) {
  return unwrap<PageResponse<PostListItem>>(http.get('/posts', { params }));
}

export function getPostDetail(postId: number) {
  return unwrap<PostDetail>(http.get(`/posts/${postId}`));
}

export function listCategories() {
  return unwrap<Category[]>(http.get('/categories'));
}

export function login(username: string, password: string) {
  return unwrap<LoginResponse>(http.post('/auth/login', { username, password }));
}

export function createPost(categoryId: number, title: string, content: string) {
  return unwrap<CreatePostResponse>(http.post('/posts', { categoryId, title, content }));
}

export function listComments(postId: number, page = 1, size = 10) {
  return unwrap<PageResponse<CommentItem>>(http.get(`/posts/${postId}/comments`, { params: { page, size } }));
}

export function createComment(postId: number, content: string) {
  return unwrap<{ commentId: number }>(http.post(`/posts/${postId}/comments`, { content }));
}

export function likePost(postId: number) {
  return unwrap<LikeResponse>(http.post(`/posts/${postId}/like`));
}

export function unlikePost(postId: number) {
  return unwrap<LikeResponse>(http.delete(`/posts/${postId}/like`));
}
