import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider } from './hooks/useTheme'
import { AuthProvider } from './context/AuthContext'
import { AppLayout } from './layouts/AppLayout'
import { Skeleton } from './components/common/Skeleton'

const Dashboard = lazy(() => import('./pages/Dashboard').then((m) => ({ default: m.Dashboard })))
const Transactions = lazy(() => import('./pages/Transactions').then((m) => ({ default: m.Transactions })))
const TransactionInvestigation = lazy(() =>
  import('./pages/TransactionInvestigation').then((m) => ({ default: m.TransactionInvestigation }))
)
const HistoryPage = lazy(() => import('./pages/History').then((m) => ({ default: m.HistoryPage })))
const ImportPage = lazy(() => import('./pages/Import').then((m) => ({ default: m.ImportPage })))
const InvestigationsPage = lazy(() =>
  import('./pages/Investigations').then((m) => ({ default: m.InvestigationsPage }))
)
const RiskIntelligencePage = lazy(() =>
  import('./pages/RiskIntelligence').then((m) => ({ default: m.RiskIntelligencePage }))
)

const PageLoader: React.FC = () => (
  <div className="p-6 space-y-4 max-w-7xl mx-auto">
    <Skeleton className="h-8 w-64 rounded-lg" />
    <Skeleton className="h-48 w-full rounded-xl" />
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Skeleton className="h-32 w-full rounded-xl" />
      <Skeleton className="h-32 w-full rounded-xl" />
      <Skeleton className="h-32 w-full rounded-xl" />
    </div>
  </div>
)

export const App: React.FC = () => {
  return (
    <ThemeProvider>
      {/* AuthProvider sits inside ThemeProvider, outside BrowserRouter.
          This ensures auth state is available to all routes while keeping
          the auth layer independent of React Router. */}
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<PageLoader />}>
            <Routes>
              <Route path="/" element={<AppLayout />}>
                <Route index element={<Dashboard />} />
                <Route path="transactions" element={<Transactions />} />
                <Route path="transactions/:id" element={<TransactionInvestigation />} />
                <Route path="intelligence" element={<RiskIntelligencePage />} />
                <Route path="investigations" element={<InvestigationsPage />} />
                <Route path="history" element={<HistoryPage />} />
                <Route path="import" element={<ImportPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Route>
            </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}

export default App
