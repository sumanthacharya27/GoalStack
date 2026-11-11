import { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import Alerts from "../components/Alerts.jsx";
import TextField from "../components/TextField.jsx";

export default function Register() {
    const { user, register } = useAuth();

    const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
    const [fieldErrors, setFieldErrors] = useState({});
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    if (user) return <Navigate to="/" replace />;

    function handleChange(e) {
        setForm({ ...form, [e.target.name]: e.target.value });
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");

        if (form.password !== form.confirm) {
            setFieldErrors({ confirm: "Passwords do not match." });
            return;
        }

        setSaving(true);
        setFieldErrors({});
        try {
            await register(form.name, form.email, form.password);
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
            <h1 className="h3 mb-4">Create your account</h1>
            <Alerts error={error} />

            <form onSubmit={handleSubmit} className="card gs-card card-body">
                <TextField id="name" name="name" label="Name" required maxLength={60} autoComplete="name"
                           value={form.name} onChange={handleChange} error={fieldErrors.name} />
                <TextField id="email" name="email" type="email" label="Email" required autoComplete="email"
                           value={form.email} onChange={handleChange} error={fieldErrors.email} />
                <TextField id="password" name="password" type="password" label="Password (8 to 72 characters)" required
                           minLength={8} maxLength={72} autoComplete="new-password" value={form.password}
                           onChange={handleChange} error={fieldErrors.password} />
                <TextField id="confirm" name="confirm" type="password" label="Confirm password" required
                           autoComplete="new-password" value={form.confirm} onChange={handleChange} error={fieldErrors.confirm} />
                <button type="submit" className="btn btn-gs mt-2" disabled={saving}>
                    {saving ? "Creating account..." : "Create account"}
                </button>
            </form>

            <p className="text-muted mt-3">
                Already have an account? <Link to="/login">Log in</Link>
            </p>
        </div>
    );
}
