import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider } from './hooks/useTheme'
import { AppLayout } from './layouts/AppLayout'
import { Dashboard } from './pages/Dashboard'
import { Transactions } from './pages/Transactions'
import { TransactionInvestigation } from './pages/TransactionInvestigation'
import { HistoryPage } from './pages/History'
import { ImportPage } from './pages/Import'
import { InvestigationsPage } from './pages/Investigations'

export const App: React.FC = () => {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<AppLayout />}>
            <Route index element={<Dashboard />} />
            <Route path="transactions" element={<Transactions />} />
            <Route path="transactions/:id" element={<TransactionInvestigation />} />
            <Route path="investigations" element={<InvestigationsPage />} />
            <Route path="history" element={<HistoryPage />} />
            <Route path="import" element={<ImportPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  )
}

export default App
