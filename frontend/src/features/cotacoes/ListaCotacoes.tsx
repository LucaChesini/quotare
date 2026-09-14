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
  TextField,
  Typography,
} from '@mui/material'
import type { SelectChangeEvent } from '@mui/material'
import { useCotacoes } from './hooks/useCotacoes'
import { useIndicadores } from '../indicadores/hooks/useIndicadores'

const TODOS_INDICADORES = 'TODOS'

const formatadorValor = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 6,
})

function ListaCotacoes() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [indicadorId, setIndicadorId] = useState<number | undefined>(undefined)
  const [inicio, setInicio] = useState<string | undefined>(undefined)
  const [fim, setFim] = useState<string | undefined>(undefined)

  const { data: dataIndicadores } = useIndicadores({ size: 100 })

  const inicioIso = inicio ? new Date(`${inicio}T00:00:00`).toISOString() : undefined
  const fimIso = fim ? new Date(`${fim}T23:59:59.999`).toISOString() : undefined

  const { data, isPending, isError, error } = useCotacoes({
    page,
    size,
    indicadorId,
    inicio: inicioIso,
    fim: fimIso,
  })

  function handleIndicadorChange(event: SelectChangeEvent) {
    const valor = event.target.value
    setIndicadorId(valor === TODOS_INDICADORES ? undefined : Number(valor))
    setPage(0)
  }

  function handleInicioChange(event: ChangeEvent<HTMLInputElement>) {
    setInicio(event.target.value || undefined)
    setPage(0)
  }

  function handleFimChange(event: ChangeEvent<HTMLInputElement>) {
    setFim(event.target.value || undefined)
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
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" component="h1">
          Cotações
        </Typography>
      </Box>

      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel id="filtro-indicador-label">Indicador</InputLabel>
          <Select
            labelId="filtro-indicador-label"
            label="Indicador"
            value={indicadorId === undefined ? TODOS_INDICADORES : String(indicadorId)}
            onChange={handleIndicadorChange}
          >
            <MenuItem value={TODOS_INDICADORES}>Todos</MenuItem>
            {(dataIndicadores?.itens ?? []).map((indicador) => (
              <MenuItem key={indicador.id} value={String(indicador.id)}>
                {indicador.codigo} — {indicador.nome}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <TextField
          label="De"
          type="date"
          size="small"
          value={inicio ?? ''}
          onChange={handleInicioChange}
          slotProps={{ inputLabel: { shrink: true } }}
        />

        <TextField
          label="Até"
          type="date"
          size="small"
          value={fim ?? ''}
          onChange={handleFimChange}
          slotProps={{ inputLabel: { shrink: true } }}
        />
      </Box>

      {isPending && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {isError && (
        <Alert severity="error">
          {error instanceof Error ? error.message : 'Erro ao carregar cotações'}
        </Alert>
      )}

      {!isPending && !isError && data && (
        <>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Indicador</TableCell>
                  <TableCell>Valor</TableCell>
                  <TableCell>Data/Hora</TableCell>
                  <TableCell>Fonte</TableCell>
                  <TableCell>Criado em</TableCell>
                  <TableCell align="right">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.itens.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      Nenhuma cotação encontrada
                    </TableCell>
                  </TableRow>
                )}

                {data.itens.map((cotacao) => (
                  <TableRow key={cotacao.id}>
                    <TableCell>
                      {cotacao.indicador.codigo} — {cotacao.indicador.nome}
                    </TableCell>
                    <TableCell>{formatadorValor.format(cotacao.valor)}</TableCell>
                    <TableCell>{new Date(cotacao.dataHora).toLocaleString('pt-BR')}</TableCell>
                    <TableCell>
                      <Chip
                        label={cotacao.fonte}
                        color={cotacao.fonte === 'EXTERNA' ? 'info' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{new Date(cotacao.criadoEm).toLocaleString('pt-BR')}</TableCell>
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

export default ListaCotacoes
