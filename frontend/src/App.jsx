import { Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar.jsx";
import ProtectedRoute from "./auth/ProtectedRoute.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import GoalDetails from "./pages/GoalDetails.jsx";
import GoalForm from "./pages/GoalForm.jsx";
import SubgoalForm from "./pages/SubgoalForm.jsx";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import NotFound from "./pages/NotFound.jsx";

export default function App() {
    return (
        <>
            <Navbar />
            <main className="container py-4">
                <Routes>
                    {/* Public pages */}
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />

                    {/* Pages that need a logged-in user */}
                    <Route element={<ProtectedRoute />}>
                        <Route path="/" element={<Dashboard />} />
                        <Route path="/goals/new" element={<GoalForm />} />
                        <Route path="/goals/:id" element={<GoalDetails />} />
                        <Route path="/goals/:id/edit" element={<GoalForm />} />
                        <Route path="/goals/:goalId/subgoals/new" element={<SubgoalForm />} />
                        <Route path="/subgoals/:id/edit" element={<SubgoalForm />} />
                    </Route>

                    <Route path="*" element={<NotFound />} />
                </Routes>
            </main>
        </>
    );
}
