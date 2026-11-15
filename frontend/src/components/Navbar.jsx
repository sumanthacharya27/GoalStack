import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";

export default function Navbar() {
    const { user, logout } = useAuth();

    return (
        <nav className="gs-navbar" aria-label="Main navigation">
            <div className="gs-navbar-inner">
                <Link to="/" className="gs-brand text-decoration-none" aria-label="GoalStack home">
                    <span className="gs-brand-mark" aria-hidden="true">
                        <span className="material-symbols-outlined">layers</span>
                    </span>
                    <span>GoalStack</span>
                </Link>

                {user ? (
                    <>
                        <div className="gs-nav-links">
                            <NavLink to="/" end className={({ isActive }) => `gs-nav-link${isActive ? " active" : ""}`}>
                                My goals
                            </NavLink>
                            <NavLink to="/calendar" className={({ isActive }) => `gs-nav-link${isActive ? " active" : ""}`}>
                                Calendar
                            </NavLink>
                            <NavLink to="/ai-planner" className={({ isActive }) => `gs-nav-link${isActive ? " active" : ""}`}>
                                AI planner
                            </NavLink>
                        </div>
                        <div className="gs-nav-actions">
                            <span className="gs-user-name">{user.name}</span>
                            <span className="gs-user-avatar" aria-hidden="true">
                                <span className="material-symbols-outlined">person</span>
                            </span>
                            <button type="button" className="btn btn-outline-secondary btn-sm gs-logout" onClick={logout}>
                                Log out
                            </button>
                        </div>
                    </>
                ) : (
                    <div className="gs-nav-actions">
                        <Link to="/login" className="gs-nav-link">Log in</Link>
                        <Link to="/register" className="btn btn-gs gs-nav-cta">Get started</Link>
                    </div>
                )}
            </div>
        </nav>
    );
}
