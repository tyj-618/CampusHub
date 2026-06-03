import { createBrowserRouter } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { PostDetailPage } from './pages/PostDetailPage';
import { PublishPage } from './pages/PublishPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'posts/:postId', element: <PostDetailPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'publish', element: <PublishPage /> }
    ]
  }
]);
