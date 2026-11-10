import { Link } from "react-router-dom";

export default function Navbar() {
    return (
        <nav className="gs-navbar">
            <div className="container d-flex align-items-center justify-content-between py-3">
                <Link to="/" className="gs-brand text-decoration-none">
                    Goal<span>Stack</span>
                </Link>
                <Link to="/" className="text-decoration-none small text-muted">
                    Dashboard
                </Link>
            </div>
        </nav>
    );
}
