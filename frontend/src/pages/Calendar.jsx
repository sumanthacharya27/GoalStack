import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api.js";
import Alerts from "../components/Alerts.jsx";
import { localDateValue, parseLocalDate } from "../utils.js";


const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

function addDays(date, amount) {
    const next = new Date(date);
    next.setDate(next.getDate() + amount);
    return next;
}

function startOfWeek(date) {
    return addDays(date, -date.getDay());
}

function sameDate(left, right) {
    return localDateValue(left) === localDateValue(right);
}

function monthLabel(date) {
    return date.toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

function dayLabel(date) {
    return date.toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" });
}

export default function Calendar() {
    const [goals, setGoals] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [selectedDate, setSelectedDate] = useState(() =>
        parseLocalDate(localDateValue(new Date()))
    );
    const [view, setView] = useState("week");
    const [savingId, setSavingId] = useState(null);

    const loadGoals = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const summaries = await api.listGoals();
            const details = await Promise.all(summaries.map((goal) => api.getGoal(goal.id)));
            setGoals(details);
        } catch (loadError) {
            setError(loadError.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadGoals();
    }, [loadGoals]);

    const scheduledSteps = useMemo(
        () => goals.flatMap((goal) => (goal.subgoals || [])
            .filter((subgoal) => subgoal.targetDate)
            .map((subgoal) => ({ ...subgoal, goalTitle: goal.title }))),
        [goals]
    );

    const calendarDays = useMemo(() => {
        if (view === "day") return [selectedDate];
        if (view === "month") {
            const monthStart = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1);
            return Array.from({ length: 42 }, (_, index) => addDays(startOfWeek(monthStart), index));
        }
        return Array.from({ length: 7 }, (_, index) => addDays(startOfWeek(selectedDate), index));
    }, [selectedDate, view]);

    const selectedSteps = scheduledSteps.filter((step) => step.targetDate === localDateValue(selectedDate));

    function moveCalendar(direction) {
        if (view === "day") setSelectedDate((date) => addDays(date, direction));
        else if (view === "month") {
            setSelectedDate((date) => new Date(date.getFullYear(), date.getMonth() + direction, date.getDate()));
        } else setSelectedDate((date) => addDays(date, direction * 7));
    }

    async function changeStatus(subgoalId, status) {
        setSavingId(subgoalId);
        setError("");
        try {
            await api.updateSubgoalStatus(subgoalId, status);
            const summaries = await api.listGoals();
            setGoals(await Promise.all(summaries.map((goal) => api.getGoal(goal.id))));
        } catch (updateError) {
            setError(updateError.message);
        } finally {
            setSavingId(null);
        }
    }

    return (
        <div className="gc-page">
            <div className="gc-page-heading">
                <div>
                    <span className="gc-eyebrow">Plan your next move</span>
                    <h1>Execution calendar</h1>
                    <p>See your goal steps by target date, update progress, and keep your plan on track.</p>
                </div>
                <div className="gc-heading-actions">
                    <Link to="/goals/new" className="btn btn-outline-secondary">+ New goal</Link>
                    <Link to="/goals/new" className="btn btn-gs">Generate a plan</Link>
                </div>
            </div>

            {error && <Alerts error={error} />}

            <div className="gc-toolbar">
                <div className="gc-toolbar-date">
                    <button type="button" className="gc-arrow" onClick={() => moveCalendar(-1)} aria-label="Previous period">‹</button>
                    <h2>{monthLabel(selectedDate)}</h2>
                    <button type="button" className="gc-arrow" onClick={() => moveCalendar(1)} aria-label="Next period">›</button>
                    <button type="button" className="gc-today-button" onClick={() => setSelectedDate(parseLocalDate(localDateValue(new Date())))}>Today</button>
                </div>
                <div className="gc-view-switcher" role="group" aria-label="Calendar view">
                    {["day", "week", "month"].map((item) => (
                        <button
                            type="button"
                            key={item}
                            className={view === item ? "active" : ""}
                            aria-pressed={view === item}
                            onClick={() => setView(item)}
                        >
                            {item[0].toUpperCase() + item.slice(1)}
                        </button>
                    ))}
                </div>
            </div>

            <div className="gc-layout">
                <aside className="gc-goal-rail">
                    <section className="gc-side-card">
                        <div className="gc-side-heading">
                            <h2>Your goals</h2>
                            <span>{goals.length}</span>
                        </div>
                        {loading ? <p className="gc-muted">Loading goals…</p> : goals.length === 0 ? (
                            <p className="gc-muted">Create a goal to start building your plan.</p>
                        ) : (
                            <div className="gc-goal-list">
                                {goals.map((goal, index) => (
                                    <Link className="gc-goal-link" to={`/goals/${goal.id}`} key={goal.id}>
                                        <span className={`gc-goal-dot tone-${index % 3}`} />
                                        <span className="gc-goal-link-copy">
                                            <strong>{goal.title}</strong>
                                            <small>{goal.completedCount} of {goal.totalCount} steps · {goal.progress}%</small>
                                        </span>
                                        <span className="gc-goal-progress" aria-hidden="true">
                                            <i style={{ width: `${goal.progress}%` }} />
                                        </span>
                                    </Link>
                                ))}
                            </div>
                        )}
                    </section>
                    <section className="gc-side-card gc-cadence-card">
                        <span className="gc-side-kicker">Scheduled steps</span>
                        <strong>{scheduledSteps.length}</strong>
                        <p>Target dates are estimates. Adjust them any time from a subgoal.</p>
                    </section>
                </aside>

                <section className="gc-calendar-card" aria-label={`${view} calendar`}>
                    <div className={`gc-weekday-row ${view === "day" ? "is-day" : ""}`}>
                        {view === "day" ? <span>{dayLabel(selectedDate)}</span> : WEEKDAYS.map((day) => <span key={day}>{day}</span>)}
                    </div>
                    <div className={`gc-days-grid ${view === "day" ? "is-day" : ""} ${view === "month" ? "is-month" : ""}`}>
                        {calendarDays.map((date, index) => {
                            const daySteps = scheduledSteps.filter((step) => step.targetDate === localDateValue(date));
                            const isToday = sameDate(date, parseLocalDate(localDateValue(new Date())));
                            const isSelected = sameDate(date, selectedDate);
                            const isInMonth = date.getMonth() === selectedDate.getMonth();
                            return (
                                <button
                                    type="button"
                                    className={`gc-day-cell ${isSelected ? "is-selected" : ""} ${isToday ? "is-today" : ""} ${!isInMonth ? "is-outside" : ""}`}
                                    key={`${localDateValue(date)}-${index}`}
                                    onClick={() => setSelectedDate(date)}
                                    aria-label={`${dayLabel(date)}, ${daySteps.length} scheduled steps`}
                                >
                                    <span className="gc-day-number">{view === "day" ? dayLabel(date) : date.getDate()}</span>
                                    <span className="gc-day-events">
                                        {daySteps.slice(0, view === "month" ? 2 : 5).map((step) => (
                                            <span className={`gc-event-pill tone-${Math.max(goals.findIndex((goal) => goal.id === step.goalId), 0) % 3}`} key={step.id}>
                                                {step.title}
                                            </span>
                                        ))}
                                        {daySteps.length > (view === "month" ? 2 : 5) && (
                                            <span className="gc-more-events">+{daySteps.length - (view === "month" ? 2 : 5)} more</span>
                                        )}
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                    {loading && <div className="gc-loading-overlay">Loading schedule…</div>}
                    {!loading && scheduledSteps.length === 0 && (
                        <div className="gc-empty-note">
                            <strong>No target dates yet</strong>
                            <span>Add target dates to your subgoals or create a plan with the AI planner to see it here.</span>
                            <Link to="/goals/new" className="btn btn-gs btn-sm">Create a goal</Link>
                        </div>
                    )}
                </section>

                <aside className="gc-today-rail">
                    <section className="gc-side-card gc-stack-card">
                        <div className="gc-stack-heading">
                            <div>
                                <span className="gc-side-kicker">Your schedule</span>
                                <h2>{sameDate(selectedDate, parseLocalDate(localDateValue(new Date()))) ? "Today’s stack" : dayLabel(selectedDate)}</h2>
                            </div>
                            <span className="gc-step-count">{selectedSteps.length} {selectedSteps.length === 1 ? "step" : "steps"}</span>
                        </div>
                        <div className="gc-stack-progress">
                            <span style={{
                                width: selectedSteps.length
                                    ? `${selectedSteps.filter((step) => step.status === "COMPLETED").length / selectedSteps.length * 100}%`
                                    : "0%",
                            }} />
                        </div>
                        {selectedSteps.length === 0 ? (
                            <p className="gc-muted">Nothing scheduled for this day. Select another date or add target dates to your subgoals.</p>
                        ) : (
                            <div className="gc-stack-list">
                                {selectedSteps.map((step) => (
                                    <article className={`gc-stack-item ${step.status === "COMPLETED" ? "is-complete" : ""}`} key={step.id}>
                                        <button
                                            type="button"
                                            className="gc-complete-button"
                                            onClick={() => changeStatus(step.id, step.status === "COMPLETED" ? "PENDING" : "COMPLETED")}
                                            disabled={savingId === step.id}
                                            aria-label={`${step.status === "COMPLETED" ? "Reopen" : "Complete"} ${step.title}`}
                                        >
                                            {step.status === "COMPLETED" ? "✓" : ""}
                                        </button>
                                        <div className="gc-stack-item-copy">
                                            <Link to={`/goals/${step.goalId}`}>{step.title}</Link>
                                            <span>{step.goalTitle}</span>
                                            <small>
                                                {step.estimatedHours != null ? `${Number(step.estimatedHours)} estimated hours` : "Target date"}
                                            </small>
                                        </div>
                                    </article>
                                ))}
                            </div>
                        )}
                    </section>
                    <section className="gc-side-card gc-focus-note">
                        <span aria-hidden="true">✦</span>
                        <div>
                            <strong>Keep it flexible</strong>
                            <p>AI dates are a starting point. Edit any step&apos;s target date to suit your plans.</p>
                        </div>
                    </section>
                </aside>
            </div>
        </div>
    );
}
