import { createBrowserRouter, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import ListaIndicadores from './features/indicadores/ListaIndicadores'
import ListaCotacoes from './features/cotacoes/ListaCotacoes'
import TelaGrafico from './features/grafico/TelaGrafico'

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Navigate to="/indicadores" replace /> },

      { path: 'indicadores', element: <ListaIndicadores /> },
      { path: 'cotacoes', element: <ListaCotacoes /> },
      { path: 'grafico', element: <TelaGrafico /> },
    ],
  },
])

export default router
