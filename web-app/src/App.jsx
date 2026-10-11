import { lazy, Suspense } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import LoadingSpinner from "./components/LoadingSpinner";

const HomePage = lazy(() => import("./pages/HomePage"));
const JobResultPage = lazy(() => import("./pages/JobResultPage"));
const NotFoundPage = lazy(() => import("./pages/NotFoundPage"));

function App() {
    return (
        <Router>
            <Layout>
                <Suspense fallback={<LoadingSpinner size="lg" className="min-h-[50vh]" />}>
                    <Routes>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/jobs/:jobId" element={<JobResultPage />} />
                        <Route path="*" element={<NotFoundPage />} />
                    </Routes>
                </Suspense>
            </Layout>
        </Router>
    );
}

export default App;
