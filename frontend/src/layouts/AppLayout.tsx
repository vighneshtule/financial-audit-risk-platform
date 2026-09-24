import React, { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from '../components/layout/Sidebar'
import { Header } from '../components/layout/Header'

export const AppLayout: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen bg-zinc-50/50 dark:bg-[#0e0f12] text-zinc-900 dark:text-zinc-100 flex flex-col antialiased">
      {/* Sidebar for Desktop & Drawer for Mobile */}
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main Container */}
      <div className="flex-1 flex flex-col lg:pl-64">
        <Header onMenuClick={() => setSidebarOpen(true)} />
        
        <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
