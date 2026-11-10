// All communication with the Spring Boot REST API lives in this file.

async function request(path, options = {}) {
    let response;
    try {
        response = await fetch(`/api${path}`, {
            headers: { "Content-Type": "application/json" },
            ...options,
        });
    } catch {
        throw makeError("Cannot reach the server. Is the backend running?", 0, {});
    }

    if (response.status === 204) {
        return null;
    }

    let data = null;
    try {
        data = await response.json();
    } catch {
        // the body was empty or not JSON
    }

    if (!response.ok) {
        throw makeError(
            data?.message || "Something went wrong. Please try again.",
            response.status,
            data?.fieldErrors || {}
        );
    }
    return data;
}

function makeError(message, status, fieldErrors) {
    const error = new Error(message);
    error.status = status;
    error.fieldErrors = fieldErrors;
    return error;
}

const json = (method, body) => ({ method, body: JSON.stringify(body) });

export const api = {
    listGoals: () => request("/goals"),
    getGoal: (id) => request(`/goals/${id}`),
    createGoal: (body) => request("/goals", json("POST", body)),
    updateGoal: (id, body) => request(`/goals/${id}`, json("PUT", body)),
    deleteGoal: (id) => request(`/goals/${id}`, { method: "DELETE" }),

    getSubgoal: (id) => request(`/subgoals/${id}`),
    createSubgoal: (goalId, body) => request(`/goals/${goalId}/subgoals`, json("POST", body)),
    updateSubgoal: (id, body) => request(`/subgoals/${id}`, json("PUT", body)),
    updateSubgoalStatus: (id, status) => request(`/subgoals/${id}/status`, json("PATCH", { status })),
    deleteSubgoal: (id) => request(`/subgoals/${id}`, { method: "DELETE" }),
};
