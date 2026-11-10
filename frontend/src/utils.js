export const STATUSES = [
    { value: "PENDING", label: "Pending" },
    { value: "IN_PROGRESS", label: "In progress" },
    { value: "COMPLETED", label: "Completed" },
];

export function statusLabel(value) {
    return STATUSES.find((s) => s.value === value)?.label ?? value;
}

export function statusBadgeClass(value) {
    if (value === "COMPLETED") return "text-bg-success";
    if (value === "IN_PROGRESS") return "text-bg-primary";
    return "text-bg-secondary";
}

// The API sends dates as "yyyy-MM-dd". Adding a time avoids timezone shifts.
export function formatDate(isoDate) {
    return new Date(`${isoDate}T00:00:00`).toLocaleDateString("en-GB", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}
