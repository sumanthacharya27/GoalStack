import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api.js";
import Alerts from "../components/Alerts.jsx";
import TextField from "../components/TextField.jsx";
import { STATUSES } from "../utils.js";

// One form for both "add subgoal" (/goals/:goalId/subgoals/new) and "edit subgoal" (/subgoals/:id/edit).
export default function SubgoalForm() {
    const { goalId: goalIdParam, id } = useParams();
    const editing = Boolean(id);
    const navigate = useNavigate();

    const [goalId, setGoalId] = useState(goalIdParam || null);
    const [goalTitle, setGoalTitle] = useState("");
    const [form, setForm] = useState({ title: "", description: "", status: "PENDING" });
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [error, setError] = useState("");

    useEffect(() => {
        async function load() {
            try {
                let parentId = goalIdParam;
                if (editing) {
                    const subgoal = await api.getSubgoal(id);
                    parentId = subgoal.goalId;
                    setGoalId(parentId);
                    setForm({ title: subgoal.title, description: subgoal.description || "", status: subgoal.status });
                }
                const goal = await api.getGoal(parentId);
                setGoalTitle(goal.title);
            } catch (e) {
                setError(e.message);
            } finally {
                setLoading(false);
            }
        }
        load();
    }, [id, goalIdParam, editing]);

    function handleChange(e) {
        setForm({ ...form, [e.target.name]: e.target.value });
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setFieldErrors({});
        setError("");
        try {
            if (editing) {
                await api.updateSubgoal(id, form);
            } else {
                await api.createSubgoal(goalId, { title: form.title, description: form.description });
            }
            navigate(`/goals/${goalId}`, { state: { message: editing ? "Subgoal updated." : "Subgoal added." } });
        } catch (err) {
            const errors = err.fieldErrors || {};
            setFieldErrors(errors);
            setError(Object.keys(errors).length > 0 ? "" : err.message);
            setSaving(false);
        }
    }

    if (loading) return <p className="text-muted">Loading...</p>;

    // The parent goal could not be loaded (for example a wrong id in the address bar).
    if (!goalTitle) {
        return (
            <>
                <Alerts error={error || "Goal not found."} />
                <Link to="/" className="btn btn-gs">Back to dashboard</Link>
            </>
        );
    }

    return (
        <div className="gs-narrow">
            <Link to={`/goals/${goalId}`} className="text-decoration-none small">&larr; Back to goal</Link>
            <h1 className="h3 mt-2 mb-1">{editing ? "Edit subgoal" : "Add a subgoal"}</h1>
            <p className="text-muted mb-4">Goal: {goalTitle}</p>
            <Alerts error={error} />

            <form onSubmit={handleSubmit} className="card gs-card card-body">
                <TextField id="title" name="title" label="Title" required maxLength={120}
                           placeholder="e.g. Collections Framework" value={form.title} onChange={handleChange}
                           error={fieldErrors.title} />
                <TextField id="description" name="description" label="Description" optional multiline rows={3}
                           value={form.description} onChange={handleChange} error={fieldErrors.description} />

                {editing ? (
                    <div className="mb-3">
                        <label htmlFor="status" className="form-label">Status <span className="text-danger">*</span></label>
                        <select id="status" name="status" className="form-select" value={form.status} onChange={handleChange}>
                            {STATUSES.map((s) => (
                                <option key={s.value} value={s.value}>{s.label}</option>
                            ))}
                        </select>
                    </div>
                ) : (
                    <p className="text-muted small">New subgoals start as Pending.</p>
                )}

                <div className="d-flex gap-2 mt-2">
                    <button type="submit" className="btn btn-gs" disabled={saving}>
                        {saving ? "Saving..." : editing ? "Save changes" : "Add subgoal"}
                    </button>
                    <Link to={`/goals/${goalId}`} className="btn btn-outline-secondary">Cancel</Link>
                </div>
            </form>
        </div>
    );
}
