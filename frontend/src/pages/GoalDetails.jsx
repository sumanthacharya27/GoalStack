import { useCallback, useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { api } from "../api.js";
import Alerts from "../components/Alerts.jsx";
import DeadlineLabel from "../components/DeadlineLabel.jsx";
import ProgressBar from "../components/ProgressBar.jsx";
import { STATUSES, statusBadgeClass, statusLabel } from "../utils.js";

export default function GoalDetails() {
    const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    const [goal, setGoal] = useState(null);
    const [message, setMessage] = useState(location.state?.message || "");
    const [error, setError] = useState("");

    const load = useCallback(async () => {
        try {
            setGoal(await api.getGoal(id));
        } catch (e) {
            setError(e.message);
        }
    }, [id]);

    useEffect(() => {
        load();
    }, [load]);

    async function changeStatus(subgoalId, status) {
        try {
            await api.updateSubgoalStatus(subgoalId, status);
            setError("");
            setMessage("Status updated.");
            await load();
        } catch (e) {
            setMessage("");
            setError(e.message);
        }
    }

    async function deleteSubgoal(subgoalId) {
        if (!window.confirm("Delete this subgoal?")) return;
        try {
            await api.deleteSubgoal(subgoalId);
            setError("");
            setMessage("Subgoal deleted.");
            await load();
        } catch (e) {
            setMessage("");
            setError(e.message);
        }
    }

    async function deleteGoal() {
        if (!window.confirm("Delete this goal and all of its subgoals?")) return;
        try {
            await api.deleteGoal(id);
            navigate("/", { state: { message: "Goal and its subgoals were deleted." } });
        } catch (e) {
            setMessage("");
            setError(e.message);
        }
    }

    if (!goal) {
        return (
            <>
                <Link to="/" className="text-decoration-none small">&larr; Back to dashboard</Link>
                <div className="mt-3">
                    {error ? <Alerts error={error} /> : <p className="text-muted">Loading...</p>}
                </div>
            </>
        );
    }

    return (
        <>
            <Link to="/" className="text-decoration-none small">&larr; Back to dashboard</Link>
            <div className="mt-3"><Alerts message={message} error={error} /></div>

            <div className="card gs-card mb-4">
                <div className="card-body">
                    <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                        <div>
                            <h1 className="h3 mb-2">{goal.title}</h1>
                            {goal.complete && <span className="badge text-bg-success me-1">Completed</span>}
                            {goal.overdue && <span className="badge text-bg-danger">Overdue</span>}
                        </div>
                        <div className="d-flex gap-2">
                            <Link to={`/goals/${goal.id}/edit`} className="btn btn-outline-secondary btn-sm">Edit goal</Link>
                            <button type="button" className="btn btn-outline-danger btn-sm" onClick={deleteGoal}>
                                Delete goal
                            </button>
                        </div>
                    </div>

                    {goal.description && <p className="text-muted mt-3 mb-2 gs-pre">{goal.description}</p>}

                    <div className="my-3"><DeadlineLabel goal={goal} /></div>

                    <div className="d-flex justify-content-between small mb-1">
                        <span>{goal.completedCount} of {goal.totalCount} subgoals completed</span>
                        <strong>{goal.progress}%</strong>
                    </div>
                    <ProgressBar progress={goal.progress} complete={goal.complete} />
                </div>
            </div>

            <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0">Subgoals</h2>
                <Link to={`/goals/${goal.id}/subgoals/new`} className="btn btn-gs btn-sm">+ Add Subgoal</Link>
            </div>

            {goal.subgoals.length === 0 ? (
                <div className="gs-empty text-center">
                    <p className="text-muted mb-0">No subgoals yet. Add one to start tracking progress.</p>
                </div>
            ) : (
                <div className="list-group gs-list">
                    {goal.subgoals.map((subgoal) => (
                        <div className="list-group-item" key={subgoal.id}>
                            <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                                <div className="flex-grow-1">
                                    <div className="d-flex align-items-center gap-2">
                    <span
                        className={`fw-semibold ${
                            subgoal.status === "COMPLETED" ? "text-decoration-line-through text-muted" : ""
                        }`}
                    >
                      {subgoal.title}
                    </span>
                                        <span className={`badge ${statusBadgeClass(subgoal.status)}`}>
                      {statusLabel(subgoal.status)}
                    </span>
                                    </div>
                                    {subgoal.description && (
                                        <p className="text-muted small mb-0 mt-1 gs-pre">{subgoal.description}</p>
                                    )}
                                </div>

                                <div className="d-flex flex-wrap align-items-center gap-2">
                                    <select
                                        className="form-select form-select-sm"
                                        aria-label="Change status"
                                        value={subgoal.status}
                                        onChange={(e) => changeStatus(subgoal.id, e.target.value)}
                                    >
                                        {STATUSES.map((s) => (
                                            <option key={s.value} value={s.value}>{s.label}</option>
                                        ))}
                                    </select>
                                    <Link to={`/subgoals/${subgoal.id}/edit`} className="btn btn-outline-secondary btn-sm">
                                        Edit
                                    </Link>
                                    <button
                                        type="button"
                                        className="btn btn-outline-danger btn-sm"
                                        onClick={() => deleteSubgoal(subgoal.id)}
                                    >
                                        Delete
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </>
    );
}
