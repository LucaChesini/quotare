import { createBrowserRouter, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import ListaIndicadores from './features/indicadores/ListaIndicadores'

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Navigate to="/indicadores" replace /> },

      { path: 'indicadores', element: <ListaIndicadores /> },
      { path: 'cotacoes', element: <div>Cotações</div> },
    ],
  },
])

export default router
