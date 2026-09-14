import type { CSSProperties } from 'react'
import {
  AppBar,
  Box,
  CssBaseline,
  Toolbar,
  Typography,
  ThemeProvider,
  createTheme,
} from '@mui/material'
import { NavLink, Outlet } from 'react-router-dom'

const theme = createTheme()

const navLinkStyle = ({ isActive }: { isActive: boolean }): CSSProperties => ({
  color: 'inherit',
  textDecoration: isActive ? 'underline' : 'none',
  fontWeight: isActive ? 700 : 400,
})

function Layout() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />

      <AppBar position="fixed">
        <Toolbar sx={{ gap: 3 }}>
          <Typography variant="h6" component="div" sx={{ mr: 2 }}>
            Quotare
          </Typography>

          <Box component="nav" sx={{ display: 'flex', gap: 3 }}>
            <NavLink to="/indicadores" style={navLinkStyle}>
              Indicadores
            </NavLink>
            <NavLink to="/cotacoes" style={navLinkStyle}>
              Cotações
            </NavLink>
          </Box>
        </Toolbar>
      </AppBar>

      <Toolbar />

      <Box component="main" sx={{ p: 3 }}>
        <Outlet />
      </Box>
    </ThemeProvider>
  )
}

export default Layout
