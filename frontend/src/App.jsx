import { Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import GoalDetails from "./pages/GoalDetails.jsx";
import GoalForm from "./pages/GoalForm.jsx";
import SubgoalForm from "./pages/SubgoalForm.jsx";
import NotFound from "./pages/NotFound.jsx";

export default function App() {
    return (
        <>
            <Navbar />
            <main className="container py-4">
                <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/goals/new" element={<GoalForm />} />
                    <Route path="/goals/:id" element={<GoalDetails />} />
                    <Route path="/goals/:id/edit" element={<GoalForm />} />
                    <Route path="/goals/:goalId/subgoals/new" element={<SubgoalForm />} />
                    <Route path="/subgoals/:id/edit" element={<SubgoalForm />} />
                    <Route path="*" element={<NotFound />} />
                </Routes>
            </main>
        </>
    );
}
