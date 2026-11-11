import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";

export default function Navbar() {
    const { user, logout } = useAuth();

    return (
        <nav className="gs-navbar">
            <div className="container d-flex align-items-center justify-content-between py-3">
                <Link to="/" className="gs-brand text-decoration-none">
                    Goal<span>Stack</span>
                </Link>

                {user ? (
                    <div className="d-flex align-items-center gap-3">
                        <span className="small text-muted">{user.name}</span>
                        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={logout}>
                            Log out
                        </button>
                    </div>
                ) : (
                    <div className="d-flex align-items-center gap-3 small">
                        <Link to="/login" className="text-decoration-none">Log in</Link>
                        <Link to="/register" className="text-decoration-none">Register</Link>
                    </div>
                )}
            </div>
        </nav>
    );
}
