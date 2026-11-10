import { Link } from "react-router-dom";

export default function NotFound() {
    return (
        <div className="text-center py-5">
            <p className="display-4 fw-bold gs-accent mb-1">404</p>
            <h1 className="h4">Page not found</h1>
            <p className="text-muted">The page you requested does not exist.</p>
            <Link to="/" className="btn btn-gs">Back to dashboard</Link>
        </div>
    );
}
