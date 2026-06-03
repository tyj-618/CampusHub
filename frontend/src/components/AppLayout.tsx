import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { clearToken, getToken } from '../api/http';

export function AppLayout() {
  const navigate = useNavigate();
  const loggedIn = Boolean(getToken());

  function handleLogout() {
    clearToken();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/" className="brand">
          <span className="brand-mark">C</span>
          <span>CampusHub</span>
        </NavLink>
        <nav className="nav-links">
          <NavLink to="/">首页</NavLink>
          <NavLink to="/publish">发布</NavLink>
          {loggedIn ? (
            <button className="link-button" onClick={handleLogout}>退出</button>
          ) : (
            <NavLink to="/login">登录</NavLink>
          )}
        </nav>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
