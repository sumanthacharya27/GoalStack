import { useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import Alerts from "../components/Alerts.jsx";
import TextField from "../components/TextField.jsx";

export default function Login() {
    const { user, login } = useAuth();
    const location = useLocation();

    const [form, setForm] = useState({ email: "", password: "" });
    const [fieldErrors, setFieldErrors] = useState({});
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    if (user) return <Navigate to="/" replace />;

    function handleChange(e) {
        setForm({ ...form, [e.target.name]: e.target.value });
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setFieldErrors({});
        setError("");
        try {
            await login(form.email, form.password);
            // On success the user is set and the redirect above takes over.
        } catch (err) {
            const errors = err.fieldErrors || {};
            setFieldErrors(errors);
            setError(Object.keys(errors).length > 0 ? "" : err.message);
            setSaving(false);
        }
    }

    return (
        <div className="gs-narrow gs-auth">
            <h1 className="h3 mb-4">Log in</h1>
            <Alerts message={location.state?.message} error={error} />

            <form onSubmit={handleSubmit} className="card gs-card card-body">
                <TextField id="email" name="email" type="email" label="Email" required autoComplete="email"
                           value={form.email} onChange={handleChange} error={fieldErrors.email} />
                <TextField id="password" name="password" type="password" label="Password" required
                           autoComplete="current-password" value={form.password} onChange={handleChange}
                           error={fieldErrors.password} />
                <button type="submit" className="btn btn-gs mt-2" disabled={saving}>
                    {saving ? "Logging in..." : "Log in"}
                </button>
            </form>

            <p className="text-muted mt-3">
                New here? <Link to="/register">Create an account</Link>
            </p>
        </div>
    );
}
