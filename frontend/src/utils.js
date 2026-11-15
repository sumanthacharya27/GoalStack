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

// Progress comes from the server with two decimals (for example 33.34). It is shown rounded DOWN to one
// decimal, so "100%" only appears when everything is really complete. Whole numbers show without a decimal.
export function formatPercent(value) {
    const cents = Math.round(Number(value) * 100); // exact: the server sends at most 2 decimals
    const tenths = Math.floor(cents / 10) / 10;
    return `${Number.isInteger(tenths) ? tenths : tenths.toFixed(1)}%`;
}

export function formatHours(value) {
    return `${Number(value)}h`;
}

// The API sends dates as "yyyy-MM-dd". Adding a time avoids timezone shifts.
export function formatDate(isoDate) {
    return new Date(`${isoDate}T00:00:00`).toLocaleDateString("en-GB", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}


// Keep calendar dates in local time. Avoid UTC conversion shifting a selected day.
export function localDateValue(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

export function parseLocalDate(value) {
    const [year, month, day] = value.split("-").map(Number);
    return new Date(year, month - 1, day);
}
