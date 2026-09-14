import { createBrowserRouter, Navigate } from 'react-router-dom'
import Layout from './components/Layout'

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Navigate to="/indicadores" replace /> },

      { path: 'indicadores', element: <div>Indicadores</div> },
      { path: 'cotacoes', element: <div>Cotações</div> },
    ],
  },
])

export default router
