import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { api } from "../api.js";
import Alerts from "../components/Alerts.jsx";
import DeadlineLabel from "../components/DeadlineLabel.jsx";
import ProgressBar from "../components/ProgressBar.jsx";
import { formatHours, formatPercent } from "../utils.js";

export default function Dashboard() {
    const location = useLocation();
    const [goals, setGoals] = useState(null);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");
    const [filter, setFilter] = useState("all");

    useEffect(() => {
        api.listGoals().then(setGoals).catch((e) => setError(e.message));
    }, []);

    const visibleGoals = (goals || []).filter((goal) => {
        const matchesSearch = goal.title.toLowerCase().includes(search.trim().toLowerCase());
        const matchesFilter =
            filter === "all" ||
            (filter === "completed" && goal.complete) ||
            (filter === "overdue" && goal.overdue) ||
            (filter === "active" && !goal.complete);
        return matchesSearch && matchesFilter;
    });

    return (
        <>
            <Alerts message={location.state?.message} error={error} />

            <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
                <div>
                    <h1 className="h3 mb-1">My Goals</h1>
                    <p className="text-muted mb-0">Break big goals into small steps and track your progress.</p>
                </div>
                <Link to="/goals/new" className="btn btn-gs">+ Add Goal</Link>
            </div>

            {goals === null && !error && <p className="text-muted">Loading...</p>}

            {goals !== null && goals.length === 0 && (
                <div className="gs-empty text-center">
                    <h2 className="h5">No goals yet</h2>
                    <p className="text-muted">Create your first goal, then split it into subgoals.</p>
                    <Link to="/goals/new" className="btn btn-gs">Create your first goal</Link>
                </div>
            )}

            {goals !== null && goals.length > 0 && (
                <div className="row g-2 mb-3">
                    <div className="col-12 col-md-8">
                        <input
                            type="search"
                            className="form-control"
                            placeholder="Search goals by title"
                            aria-label="Search goals by title"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </div>
                    <div className="col-12 col-md-4">
                        <select
                            className="form-select"
                            aria-label="Filter goals"
                            value={filter}
                            onChange={(e) => setFilter(e.target.value)}
                        >
                            <option value="all">All goals</option>
                            <option value="active">Not completed</option>
                            <option value="completed">Completed</option>
                            <option value="overdue">Overdue</option>
                        </select>
                    </div>
                </div>
            )}

            {goals !== null && goals.length > 0 && visibleGoals.length === 0 && (
                <div className="gs-empty text-center">
                    <p className="text-muted mb-0">No goals match your search or filter.</p>
                </div>
            )}

            {goals !== null && visibleGoals.length > 0 && (
                <div className="row g-3">
                    {visibleGoals.map((goal) => (
                        <div className="col-12 col-md-6 col-lg-4" key={goal.id}>
                            <div className="card gs-card h-100">
                                <div className="card-body d-flex flex-column">
                                    <div className="d-flex justify-content-between align-items-start gap-2 mb-1">
                                        <h2 className="h5 mb-0">
                                            <Link to={`/goals/${goal.id}`} className="text-decoration-none gs-title">
                                                {goal.title}
                                            </Link>
                                        </h2>
                                        <div className="d-flex gap-1">
                                            {goal.complete && <span className="badge text-bg-success">Completed</span>}
                                            {goal.overdue && <span className="badge text-bg-danger">Overdue</span>}
                                        </div>
                                    </div>

                                    {goal.description && <p className="text-muted small gs-clamp">{goal.description}</p>}

                                    <div className="mt-auto">
                                        <div className="d-flex justify-content-between small mb-1">
                                            <span>{goal.completedCount} of {goal.totalCount} subgoals done</span>
                                            <strong>{formatPercent(goal.progress)}</strong>
                                        </div>
                                        <ProgressBar progress={goal.progress} complete={goal.complete} />
                                        {goal.totalEstimatedHours !== null && goal.totalEstimatedHours !== undefined && (
                                            <p className="small text-muted mt-2 mb-0">
                                                Estimated effort: {formatHours(goal.totalEstimatedHours)}
                                            </p>
                                        )}
                                        <div className="mt-3"><DeadlineLabel goal={goal} /></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </>
    );
}
