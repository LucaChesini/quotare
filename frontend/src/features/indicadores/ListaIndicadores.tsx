import { useState } from 'react'
import type { ChangeEvent } from 'react'
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
} from '@mui/material'
import type { SelectChangeEvent } from '@mui/material'
import { useIndicadores } from './hooks/useIndicadores'
import type { FonteDados } from '../../types/comum'

const TODAS_FONTES = 'TODAS'
const TODOS_ATIVOS = 'TODOS'

function ListaIndicadores() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [fonte, setFonte] = useState<FonteDados | undefined>(undefined)
  const [ativo, setAtivo] = useState<boolean | undefined>(undefined)

  const { data, isPending, isError, error } = useIndicadores({ page, size, fonte, ativo })

  function handleFonteChange(event: SelectChangeEvent) {
    const valor = event.target.value
    setFonte(valor === TODAS_FONTES ? undefined : (valor as FonteDados))
    setPage(0)
  }

  function handleAtivoChange(event: SelectChangeEvent) {
    const valor = event.target.value
    setAtivo(valor === TODOS_ATIVOS ? undefined : valor === 'true')
    setPage(0)
  }

  function handlePageChange(_event: unknown, novaPagina: number) {
    setPage(novaPagina)
  }

  function handleRowsPerPageChange(event: ChangeEvent<HTMLInputElement>) {
    setSize(Number(event.target.value))
    setPage(0)
  }

  return (
    <Box>
      <Typography variant="h5" component="h1" sx={{ mb: 3 }}>
        Indicadores
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel id="filtro-fonte-label">Fonte</InputLabel>
          <Select
            labelId="filtro-fonte-label"
            label="Fonte"
            value={fonte ?? TODAS_FONTES}
            onChange={handleFonteChange}
          >
            <MenuItem value={TODAS_FONTES}>Todas</MenuItem>
            <MenuItem value="LOCAL">Local</MenuItem>
            <MenuItem value="EXTERNA">Externa</MenuItem>
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel id="filtro-ativo-label">Status</InputLabel>
          <Select
            labelId="filtro-ativo-label"
            label="Status"
            value={ativo === undefined ? TODOS_ATIVOS : String(ativo)}
            onChange={handleAtivoChange}
          >
            <MenuItem value={TODOS_ATIVOS}>Todos</MenuItem>
            <MenuItem value="true">Ativos</MenuItem>
            <MenuItem value="false">Inativos</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {isPending && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {isError && (
        <Alert severity="error">
          {error instanceof Error ? error.message : 'Erro ao carregar indicadores'}
        </Alert>
      )}

      {!isPending && !isError && data && (
        <>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Nome</TableCell>
                  <TableCell>Fonte</TableCell>
                  <TableCell>Ativo</TableCell>
                  <TableCell>Criado em</TableCell>
                  <TableCell align="right">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.itens.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      Nenhum indicador encontrado
                    </TableCell>
                  </TableRow>
                )}

                {data.itens.map((indicador) => (
                  <TableRow key={indicador.id}>
                    <TableCell>{indicador.codigo}</TableCell>
                    <TableCell>{indicador.nome}</TableCell>
                    <TableCell>{indicador.fonte}</TableCell>
                    <TableCell>
                      <Chip
                        label={indicador.ativo ? 'Sim' : 'Não'}
                        color={indicador.ativo ? 'success' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{new Date(indicador.criadoEm).toLocaleString('pt-BR')}</TableCell>
                    <TableCell align="right" />
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          <TablePagination
            component="div"
            count={data.totalItens}
            page={page}
            rowsPerPage={size}
            rowsPerPageOptions={[10, 20, 50]}
            onPageChange={handlePageChange}
            onRowsPerPageChange={handleRowsPerPageChange}
            labelRowsPerPage="Itens por página:"
            labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
          />
        </>
      )}
    </Box>
  )
}

export default ListaIndicadores
