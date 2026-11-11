import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";

// Wraps every page that needs a logged-in user. Anyone else is sent to the login page.
export default function ProtectedRoute() {
    const { user, loading } = useAuth();

    if (loading) return <p className="text-muted">Loading...</p>;
    return user ? <Outlet /> : <Navigate to="/login" replace />;
}
