import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api.js";
import Alerts from "../components/Alerts.jsx";
import { localDateValue } from "../utils.js";

const WELCOME = {
    role: "ASSISTANT",
    content: "Hi! I can help break a goal into smaller steps. What would you like to achieve?",
};

function addDays(date, days) {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return localDateValue(next);
}

export default function AIPlanner() {
    const navigate = useNavigate();
    const [mode, setMode] = useState("chat");
    const [providerInfo, setProviderInfo] = useState(null);

    useEffect(() => {
        api.aiStatus().then(setProviderInfo).catch(() => setProviderInfo(null));
    }, []);
    const [messages, setMessages] = useState([WELCOME]);
    const [message, setMessage] = useState("");
    const [quickGoal, setQuickGoal] = useState("");
    const [deadline, setDeadline] = useState("");
    const [plan, setPlan] = useState(null);
    const [ready, setReady] = useState(false);
    const [busy, setBusy] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    async function sendMessage(event) {
        event.preventDefault();
        const text = message.trim();
        if (!text || busy) return;

        const next = [...messages, { role: "USER", content: text }];
        setMessages(next);
        setMessage("");
        setBusy(true);
        setError("");
        setNotice("");
        try {
            const turn = await api.chatWithPlanner(next.slice(-12));
            setMessages((current) => [...current, { role: "ASSISTANT", content: turn.reply }]);
            setReady(Boolean(turn.readyToGenerate));
        } catch (e) {
            setError(e.message);
        } finally {
            setBusy(false);
        }
    }

    async function generateFromChat() {
        setBusy(true);
        setError("");
        try {
            const generated = await api.generatePlan(messages.slice(-12));
            setPlan(generated);
            setNotice("Review and edit the suggested plan before saving it.");
        } catch (e) {
            setError(e.message);
        } finally {
            setBusy(false);
        }
    }

    async function quickGenerate(event) {
        event.preventDefault();
        if (!quickGoal.trim()) return;
        setBusy(true);
        setError("");
        setNotice("");
        setPlan(null);
        const quickMessages = [{ role: "USER", content: quickGoal.trim() }];
        try {
            const generated = await api.generatePlan(quickMessages);
            setPlan(generated);
            setNotice("Your suggested goal is ready. Review it before saving.");
        } catch (e) {
            setError(e.message);
        } finally {
            setBusy(false);
        }
    }

    function updatePlanField(field, value) {
        setPlan((current) => ({ ...current, [field]: value }));
    }

    function updateSubgoal(index, field, value) {
        setPlan((current) => ({
            ...current,
            subgoals: current.subgoals.map((item, i) => i === index
                ? { ...item, [field]: field === "estimatedHours" ? Number(value) : value }
                : item),
        }));
    }

    async function savePlan() {
        if (!plan || !plan.goalTitle.trim() || plan.subgoals.length === 0) return;
        const invalidStep = plan.subgoals.some((item) =>
            !item.title?.trim() || !Number.isFinite(Number(item.estimatedHours))
            || Number(item.estimatedHours) < 0.25 || Number(item.estimatedHours) > 1000
        );
        if (invalidStep) {
            setError("Each subgoal needs a title and an estimate between 0.25 and 1000 hours.");
            return;
        }
        if (deadline && deadline < localDateValue(new Date())) {
            setError("The deadline cannot be in the past.");
            return;
        }
        setSaving(true);
        setError("");
        try {
            const goal = await api.createGoal({
                title: plan.goalTitle.trim(),
                description: plan.goalDescription || "",
                deadline: deadline || null,
            });

            const totalHours = plan.subgoals.reduce((sum, item) => sum + Number(item.estimatedHours || 0), 0);
            const start = new Date();
            const endDate = deadline || addDays(start, 30);
            const availableDays = Math.max(0, Math.round(
                (new Date(`${endDate}T00:00:00`).getTime() - new Date(`${localDateValue(start)}T00:00:00`).getTime())
                / 86400000
            ));
            let cumulativeHours = 0;

            for (const item of plan.subgoals) {
                cumulativeHours += Number(item.estimatedHours || 0);
                const targetDate = addDays(start, Math.round(cumulativeHours / Math.max(totalHours, 0.25) * availableDays));
                await api.createSubgoal(goal.id, {
                    title: item.title.trim(),
                    description: item.description || "",
                    estimatedHours: Number(item.estimatedHours),
                    targetDate,
                });
            }
            navigate(`/goals/${goal.id}`, { state: { message: "AI-generated goal and scheduled subgoals saved." } });
        } catch (e) {
            setError(`${e.message} If the goal was created, check My Goals before trying again.`);
            setSaving(false);
        }
    }

    return (
        <div className="ai-planner-page">
            <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
                <div>
                    <span className="ai-eyebrow">GoalStack assistant</span>
                    <h1 className="h3 mb-1">AI Goal Planner</h1>
                    <p className="text-muted mb-0">Describe what you want to achieve, then review a practical plan.</p>
                </div>
                <span className="ai-demo-tag">
          {providerInfo
              ? `${providerInfo.provider} · ${providerInfo.availability.toLowerCase().replace("_", " ")}`
              : "AI provider status unavailable"}
        </span>
            </div>

            <Alerts error={error} message={notice} />

            <div className="ai-mode-tabs" role="tablist" aria-label="Planner mode">
                <button type="button" className={mode === "chat" ? "active" : ""} onClick={() => setMode("chat")}>Chat planner</button>
                <button type="button" className={mode === "quick" ? "active" : ""} onClick={() => setMode("quick")}>Quick goal generator</button>
            </div>

            {mode === "chat" ? (
                <section className="ai-chat-card">
                    <div className="ai-chat-heading">
                        <div><strong>GoalStack Assistant</strong><small>Tell me your goal and I’ll help shape it.</small></div>
                        <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => {
                            setMessages([WELCOME]); setReady(false); setPlan(null); setError(""); setNotice("");
                        }}>New chat</button>
                    </div>
                    <div className="ai-chat-messages" aria-live="polite">
                        {messages.map((item, index) => (
                            <div className={`ai-message ${item.role === "USER" ? "from-user" : "from-assistant"}`} key={`${index}-${item.role}`}>
                                <span>{item.role === "USER" ? "You" : "Assistant"}</span>
                                <p>{item.content}</p>
                            </div>
                        ))}
                        {busy && <div className="ai-typing">Assistant is thinking…</div>}
                    </div>
                    <form className="ai-chat-composer" onSubmit={sendMessage}>
                        <input value={message} onChange={(e) => setMessage(e.target.value)}
                               placeholder="e.g. I want to learn Spring Boot in 8 weeks…" maxLength={2000} />
                        <button className="btn btn-gs" type="submit" disabled={busy || !message.trim()}>Send</button>
                    </form>
                    {ready && !plan && (
                        <div className="ai-generate-row">
                            <span>The assistant has enough information for a first draft.</span>
                            <button type="button" className="btn btn-gs btn-sm" onClick={generateFromChat} disabled={busy}>Generate plan</button>
                        </div>
                    )}
                </section>
            ) : (
                <form className="card gs-card card-body ai-quick-form" onSubmit={quickGenerate}>
                    <label htmlFor="quickGoal" className="form-label fw-semibold">What goal do you want to achieve?</label>
                    <textarea id="quickGoal" className="form-control mb-3" rows={4} maxLength={2000}
                              placeholder="Describe your goal. For example: I want to prepare for a Java developer interview, covering Core Java, Spring Boot, SQL and coding practice."
                              value={quickGoal} onChange={(e) => setQuickGoal(e.target.value)} />
                    <div className="d-flex flex-wrap align-items-end gap-3">
                        <div className="ai-deadline-field">
                            <label htmlFor="quickDeadline" className="form-label">Target deadline (optional)</label>
                            <input id="quickDeadline" className="form-control" type="date" min={localDateValue(new Date())}
                                   value={deadline} onChange={(e) => setDeadline(e.target.value)} />
                        </div>
                        <button type="submit" className="btn btn-gs" disabled={busy || !quickGoal.trim()}>
                            {busy ? "Generating…" : "Generate subgoals"}
                        </button>
                    </div>
                </form>
            )}

            {plan && (
                <section className="card gs-card ai-plan-card mt-4">
                    <div className="card-body">
                        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
                            <div><span className="ai-eyebrow">Review before saving</span><h2 className="h5 mb-0">Suggested goal plan</h2></div>
                            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setPlan(null)}>Discard draft</button>
                        </div>
                        <label className="form-label">Goal title</label>
                        <input className="form-control mb-3" maxLength={120} value={plan.goalTitle}
                               onChange={(e) => updatePlanField("goalTitle", e.target.value)} />
                        <label className="form-label">Description</label>
                        <textarea className="form-control mb-3" rows={2} value={plan.goalDescription || ""}
                                  onChange={(e) => updatePlanField("goalDescription", e.target.value)} />
                        {mode === "chat" && (
                            <div className="ai-deadline-field mb-3">
                                <label htmlFor="chatDeadline" className="form-label">Target deadline (optional)</label>
                                <input id="chatDeadline" className="form-control" type="date" min={localDateValue(new Date())}
                                       value={deadline} onChange={(e) => setDeadline(e.target.value)} />
                            </div>
                        )}
                        <div className="d-flex justify-content-between align-items-center mb-2">
                            <strong>Subgoals</strong><span className="text-muted small">{plan.subgoals.length} steps</span>
                        </div>
                        <div className="ai-plan-steps">
                            {plan.subgoals.map((item, index) => (
                                <article className="ai-plan-step" key={index}>
                                    <span className="ai-step-number">{index + 1}</span>
                                    <div className="flex-grow-1">
                                        <input className="form-control form-control-sm mb-2" maxLength={120} value={item.title}
                                               onChange={(e) => updateSubgoal(index, "title", e.target.value)} aria-label={`Subgoal ${index + 1} title`} />
                                        <textarea className="form-control form-control-sm mb-2" rows={2} value={item.description || ""}
                                                  onChange={(e) => updateSubgoal(index, "description", e.target.value)} aria-label={`Subgoal ${index + 1} description`} />
                                        <div className="ai-hours-input">
                                            <label className="form-label small mb-0">Estimated hours</label>
                                            <input className="form-control form-control-sm" type="number" required min="0.25" max="1000" step="0.25"
                                                   value={item.estimatedHours} onChange={(e) => updateSubgoal(index, "estimatedHours", e.target.value)} />
                                        </div>
                                    </div>
                                </article>
                            ))}
                        </div>
                        {plan.assumptions?.length > 0 && <p className="text-muted small mt-3 mb-0">Assumptions: {plan.assumptions.join(" · ")}</p>}
                        <div className="d-flex flex-wrap gap-2 mt-4">
                            <button type="button" className="btn btn-gs" onClick={savePlan} disabled={saving || !plan.goalTitle.trim()}>
                                {saving ? "Saving goal…" : "Save goal and schedule subgoals"}
                            </button>
                            <button type="button" className="btn btn-outline-secondary" onClick={() => setPlan(null)}>Edit conversation</button>
                        </div>
                    </div>
                </section>
            )}
            <p className="ai-demo-footnote">
                {providerInfo?.message || "The planner uses the provider configured in application.properties."}
                {" "}For a real local model, set AI_PROVIDER=ollama and install the configured Ollama model.
            </p>
        </div>
    );
}
