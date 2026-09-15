import { useState } from 'react'
import type { ChangeEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControl,
  IconButton,
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
  Tooltip,
  Typography,
} from '@mui/material'
import type { SelectChangeEvent } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import { useCotacoes } from './hooks/useCotacoes'
import { useRemoverCotacao } from './hooks/useRemoverCotacao'
import { useIndicadores } from '../indicadores/hooks/useIndicadores'
import CotacaoFormDialog from './CotacaoFormDialog'
import ConfirmDialog from '../../components/ConfirmDialog'
import { extrairMensagemErro } from '../../api/erro'
import type { Cotacao } from '../../types/cotacao'

const TODOS_INDICADORES = 'TODOS'

const formatadorValor = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 6,
})

interface DialogoFormularioState {
  aberto: boolean
  cotacao?: Cotacao
  abertura: number
}

interface DialogoExclusaoState {
  aberto: boolean
  cotacao?: Cotacao
}

const DIALOGO_FECHADO: DialogoFormularioState = { aberto: false, cotacao: undefined, abertura: 0 }
const EXCLUSAO_FECHADA: DialogoExclusaoState = { aberto: false, cotacao: undefined }

function ListaCotacoes() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [indicadorId, setIndicadorId] = useState<number | undefined>(undefined)
  const [inicio, setInicio] = useState<string | undefined>(undefined)
  const [fim, setFim] = useState<string | undefined>(undefined)

  const [dialogoFormulario, setDialogoFormulario] = useState<DialogoFormularioState>(DIALOGO_FECHADO)
  const [dialogoExclusao, setDialogoExclusao] = useState<DialogoExclusaoState>(EXCLUSAO_FECHADA)

  const { data: dataIndicadores } = useIndicadores({ size: 100 })
  const removerCotacao = useRemoverCotacao()

  const inicioIso = inicio ? new Date(`${inicio}T00:00:00`).toISOString() : undefined
  const fimIso = fim ? new Date(`${fim}T23:59:59.999`).toISOString() : undefined

  const { data, isPending, isError, error } = useCotacoes({
    page,
    size,
    indicadorId,
    inicio: inicioIso,
    fim: fimIso,
  })

  function abrirCriacao() {
    setDialogoFormulario((atual) => ({ aberto: true, cotacao: undefined, abertura: atual.abertura + 1 }))
  }

  function abrirEdicao(cotacao: Cotacao) {
    setDialogoFormulario((atual) => ({ aberto: true, cotacao, abertura: atual.abertura + 1 }))
  }

  function fecharFormulario() {
    setDialogoFormulario((atual) => ({ ...atual, aberto: false }))
  }

  function abrirExclusao(cotacao: Cotacao) {
    removerCotacao.reset()
    setDialogoExclusao({ aberto: true, cotacao })
  }

  function fecharExclusao() {
    setDialogoExclusao(EXCLUSAO_FECHADA)
  }

  function confirmarExclusao() {
    if (!dialogoExclusao.cotacao) return
    removerCotacao.mutate(dialogoExclusao.cotacao.id, {
      onSuccess: fecharExclusao,
    })
  }

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
        <Button variant="contained" startIcon={<AddIcon />} onClick={abrirCriacao}>
          Nova Cotação
        </Button>
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
                    <TableCell align="right">
                      {cotacao.fonte === 'EXTERNA' ? (
                        <>
                          <Tooltip title="Cotação gerida por integração automática, não pode ser editada manualmente">
                            <span>
                              <IconButton size="small" aria-label="Editar" disabled>
                                <EditIcon fontSize="small" />
                              </IconButton>
                            </span>
                          </Tooltip>
                          <Tooltip title="Cotação gerida por integração automática, não pode ser excluída manualmente">
                            <span>
                              <IconButton size="small" aria-label="Excluir" disabled>
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </span>
                          </Tooltip>
                        </>
                      ) : (
                        <>
                          <IconButton size="small" aria-label="Editar" onClick={() => abrirEdicao(cotacao)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton size="small" aria-label="Excluir" onClick={() => abrirExclusao(cotacao)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </>
                      )}
                    </TableCell>
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

      <CotacaoFormDialog
        key={dialogoFormulario.abertura}
        open={dialogoFormulario.aberto}
        cotacao={dialogoFormulario.cotacao}
        onClose={fecharFormulario}
      />

      <ConfirmDialog
        open={dialogoExclusao.aberto}
        titulo="Excluir cotação"
        mensagem={
          dialogoExclusao.cotacao
            ? `Tem certeza que deseja excluir a cotação de "${dialogoExclusao.cotacao.indicador.codigo}" em ${new Date(
                dialogoExclusao.cotacao.dataHora,
              ).toLocaleString('pt-BR')}? Essa ação não pode ser desfeita.`
            : ''
        }
        erro={removerCotacao.isError ? extrairMensagemErro(removerCotacao.error) : null}
        carregando={removerCotacao.isPending}
        onConfirmar={confirmarExclusao}
        onCancelar={fecharExclusao}
      />
    </Box>
  )
}

export default ListaCotacoes
