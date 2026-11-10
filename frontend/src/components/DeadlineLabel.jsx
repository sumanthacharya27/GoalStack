import { formatDate } from "../utils.js";

export default function DeadlineLabel({ goal }) {
    const className = goal.overdue ? "text-danger fw-semibold" : "text-muted";
    return (
        <p className={`small mb-0 ${className}`}>
            {goal.deadline ? `Deadline: ${formatDate(goal.deadline)}` : "No deadline"}
        </p>
    );
}
