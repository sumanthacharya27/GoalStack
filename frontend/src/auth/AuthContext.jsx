import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, clearToken, getToken, setToken, setUnauthorizedHandler } from "../api.js";

// Holds "who is logged in" for the whole app. Any component can call useAuth().
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    // If a token is stored, wait until the server confirms it before deciding what to show.
    const [loading, setLoading] = useState(Boolean(getToken()));

    const logout = useCallback(
        (message) => {
            clearToken();
            setUser(null);
            navigate("/login", { replace: true, state: message ? { message } : undefined });
        },
        [navigate]
    );

    // The API layer calls this when any request comes back 401 (for example an expired token).
    useEffect(() => {
        setUnauthorizedHandler(() => logout("Your session has expired. Please log in again."));
    }, [logout]);

    useEffect(() => {
        if (!getToken()) {
            setLoading(false);
            return;
        }
        api
            .me()
            .then(setUser)
            .catch(() => clearToken())
            .finally(() => setLoading(false));
    }, []);

    async function handleAuthResult(result) {
        setToken(result.token);
        setUser(result.user);
    }

    async function login(email, password) {
        await handleAuthResult(await api.login({ email, password }));
    }

    async function register(name, email, password) {
        await handleAuthResult(await api.register({ name, email, password }));
    }

    return (
        <AuthContext.Provider value={{ user, loading, login, register, logout: () => logout("You have been logged out.") }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}
