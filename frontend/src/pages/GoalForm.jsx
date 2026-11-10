import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api.js";
import Alerts from "../components/Alerts.jsx";
import TextField from "../components/TextField.jsx";

// One form for both "create goal" (/goals/new) and "edit goal" (/goals/:id/edit).
export default function GoalForm() {
    const { id } = useParams();
    const editing = Boolean(id);
    const navigate = useNavigate();

    const [form, setForm] = useState({ title: "", description: "", deadline: "" });
    const [loading, setLoading] = useState(editing);
    const [saving, setSaving] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [error, setError] = useState("");

    useEffect(() => {
        if (!editing) return;
        api
            .getGoal(id)
            .then((goal) =>
                setForm({
                    title: goal.title,
                    description: goal.description || "",
                    deadline: goal.deadline || "",
                })
            )
            .catch((e) => setError(e.message))
            .finally(() => setLoading(false));
    }, [id, editing]);

    function handleChange(e) {
        setForm({ ...form, [e.target.name]: e.target.value });
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setFieldErrors({});
        setError("");
        const body = { title: form.title, description: form.description, deadline: form.deadline || null };
        try {
            const saved = editing ? await api.updateGoal(id, body) : await api.createGoal(body);
            navigate(`/goals/${saved.id}`, { state: { message: editing ? "Goal updated." : "Goal created." } });
        } catch (err) {
            const errors = err.fieldErrors || {};
            setFieldErrors(errors);
            setError(Object.keys(errors).length > 0 ? "" : err.message);
            setSaving(false);
        }
    }

    if (loading) return <p className="text-muted">Loading...</p>;

    return (
        <div className="gs-narrow">
            <h1 className="h3 mb-4">{editing ? "Edit goal" : "Create a new goal"}</h1>
            <Alerts error={error} />

            <form onSubmit={handleSubmit} className="card gs-card card-body">
                <TextField id="title" name="title" label="Title" required maxLength={120}
                           placeholder="e.g. Learn Java" value={form.title} onChange={handleChange} error={fieldErrors.title} />
                <TextField id="description" name="description" label="Description" optional multiline rows={4}
                           value={form.description} onChange={handleChange} error={fieldErrors.description} />
                <TextField id="deadline" name="deadline" type="date" label="Deadline" optional
                           value={form.deadline} onChange={handleChange} error={fieldErrors.deadline} />

                <div className="d-flex gap-2 mt-2">
                    <button type="submit" className="btn btn-gs" disabled={saving}>
                        {saving ? "Saving..." : editing ? "Save changes" : "Create goal"}
                    </button>
                    <Link to={editing ? `/goals/${id}` : "/"} className="btn btn-outline-secondary">Cancel</Link>
                </div>
            </form>
        </div>
    );
}
