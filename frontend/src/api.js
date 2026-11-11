// All communication with the Spring Boot REST API lives in this file.
// After login, the token is kept in localStorage and sent with every request.

const TOKEN_KEY = "goalstack_token";

export function getToken() {
    try {
        return localStorage.getItem(TOKEN_KEY);
    } catch {
        return null;
    }
}

export function setToken(token) {
    try {
        localStorage.setItem(TOKEN_KEY, token);
    } catch {
        // storage is blocked (private mode); the user stays logged in until the page is closed
    }
}

export function clearToken() {
    try {
        localStorage.removeItem(TOKEN_KEY);
    } catch {
        // nothing to clear
    }
}

// AuthContext registers a function here that logs the user out when the server answers 401.
let unauthorizedHandler = null;
export function setUnauthorizedHandler(handler) {
    unauthorizedHandler = handler;
}

async function request(path, options = {}) {
    // Login and register must NOT send a token: an old or expired token would make the server reject them.
    const isAuthEndpoint = path === "/auth/login" || path === "/auth/register";
    const headers = { "Content-Type": "application/json" };
    const token = getToken();
    if (token && !isAuthEndpoint) {
        headers.Authorization = `Bearer ${token}`;
    }

    let response;
    try {
        response = await fetch(`/api${path}`, { ...options, headers });
    } catch {
        throw makeError("Cannot reach the server. Is the backend running?", 0, {});
    }

    if (response.status === 401 && !isAuthEndpoint) {
        clearToken();
        if (unauthorizedHandler) unauthorizedHandler();
        throw makeError("Your session has expired. Please log in again.", 401, {});
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
    register: (body) => request("/auth/register", json("POST", body)),
    login: (body) => request("/auth/login", json("POST", body)),
    me: () => request("/auth/me"),

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
