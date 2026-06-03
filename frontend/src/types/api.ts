export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  page: number;
  size: number;
  total: number;
  pages: number;
  records: T[];
}

export interface Category {
  id: number;
  name: string;
  code: string;
  sortOrder: number;
}

export interface Author {
  id: number;
  nickname: string;
  avatarUrl: string | null;
}

export interface PostCategory {
  id: number;
  name: string;
  code: string;
}

export interface PostListItem {
  id: number;
  title: string;
  summary: string;
  category: PostCategory;
  author: Author;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  hotScore: number;
  createdAt: string;
}

export interface PostDetail {
  id: number;
  title: string;
  content: string;
  category: PostCategory;
  author: Author;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  liked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CommentItem {
  id: number;
  content: string;
  author: Author;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  expiresIn: number;
  user: {
    id: number;
    username: string;
    nickname: string;
    avatarUrl: string | null;
    role: number;
  };
}

export interface CreatePostResponse {
  postId: number;
}

export interface LikeResponse {
  liked: boolean;
  likeCount: number;
}
