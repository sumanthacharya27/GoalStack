import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { api } from "../api.js";
import Alerts from "../components/Alerts.jsx";
import DeadlineLabel from "../components/DeadlineLabel.jsx";
import ProgressBar from "../components/ProgressBar.jsx";

export default function Dashboard() {
    const location = useLocation();
    const [goals, setGoals] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        api.listGoals().then(setGoals).catch((e) => setError(e.message));
    }, []);

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
                <div className="row g-3">
                    {goals.map((goal) => (
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
                                            <strong>{goal.progress}%</strong>
                                        </div>
                                        <ProgressBar progress={goal.progress} complete={goal.complete} />
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
