import { Controller, useForm } from 'react-hook-form'
import type { DefaultValues } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material'
import { cotacaoSchema } from './schema'
import type { CotacaoFormValues } from './schema'
import { useCriarCotacao } from './hooks/useCriarCotacao'
import { useAtualizarCotacao } from './hooks/useAtualizarCotacao'
import { useIndicadores } from '../indicadores/hooks/useIndicadores'
import { extrairMensagemErro } from '../../api/erro'
import type { Cotacao } from '../../types/cotacao'

interface CotacaoFormDialogProps {
  open: boolean
  cotacao?: Cotacao
  onClose: () => void
}

function paraDatetimeLocal(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function valoresIniciais(cotacao?: Cotacao): DefaultValues<CotacaoFormValues> {
  if (cotacao) {
    return {
      indicadorId: cotacao.indicadorId,
      valor: cotacao.valor,
      dataHora: paraDatetimeLocal(cotacao.dataHora),
    }
  }
  return { indicadorId: undefined, valor: undefined, dataHora: '' }
}

function CotacaoFormDialog({ open, cotacao, onClose }: CotacaoFormDialogProps) {
  const modoEdicao = cotacao !== undefined

  const criar = useCriarCotacao()
  const atualizar = useAtualizarCotacao()
  const mutacaoEmAndamento = modoEdicao ? atualizar.isPending : criar.isPending
  const erroMutacao = modoEdicao ? atualizar.error : criar.error

  const {
    data: dataIndicadores,
    isPending: carregandoIndicadores,
    isError: erroIndicadores,
  } = useIndicadores({ fonte: 'LOCAL', ativo: true, size: 100 })
  const indicadores = dataIndicadores?.itens ?? []
  const mensagemSemIndicadores = carregandoIndicadores
    ? 'Carregando indicadores...'
    : erroIndicadores
      ? 'Erro ao carregar indicadores'
      : 'Nenhum indicador local encontrado'

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<CotacaoFormValues>({
    resolver: zodResolver(cotacaoSchema),
    defaultValues: valoresIniciais(cotacao),
  })

  function onSubmit(valores: CotacaoFormValues) {
    const payload = {
      indicadorId: valores.indicadorId,
      valor: valores.valor,
      dataHora: new Date(valores.dataHora).toISOString(),
    }

    if (modoEdicao && cotacao) {
      atualizar.mutate({ id: cotacao.id, request: payload }, { onSuccess: onClose })
      return
    }

    criar.mutate(payload, { onSuccess: onClose })
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{modoEdicao ? 'Editar Cotação' : 'Nova Cotação'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)}>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {erroMutacao && <Alert severity="error">{extrairMensagemErro(erroMutacao)}</Alert>}

          <Controller
            name="indicadorId"
            control={control}
            render={({ field }) => (
              <FormControl fullWidth error={!!errors.indicadorId}>
                <InputLabel id="indicador-label">Indicador</InputLabel>
                <Select<number | ''>
                  {...field}
                  labelId="indicador-label"
                  label="Indicador"
                  value={field.value ?? ''}
                  onChange={(event) => {
                    const valor = event.target.value
                    field.onChange(valor === '' ? undefined : valor)
                  }}
                >
                  {indicadores.length === 0 && (
                    <MenuItem disabled value="">
                      {mensagemSemIndicadores}
                    </MenuItem>
                  )}
                  {indicadores.map((indicador) => (
                    <MenuItem key={indicador.id} value={indicador.id}>
                      {indicador.codigo} — {indicador.nome}
                    </MenuItem>
                  ))}
                </Select>
                {errors.indicadorId && <FormHelperText>{errors.indicadorId.message}</FormHelperText>}
              </FormControl>
            )}
          />

          <Controller
            name="valor"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                type="number"
                label="Valor"
                helperText={errors.valor?.message}
                error={!!errors.valor}
                fullWidth
                value={field.value === undefined || Number.isNaN(field.value) ? '' : field.value}
                onChange={(event) => {
                  const valor = event.target.value
                  field.onChange(valor === '' ? undefined : Number(valor))
                }}
                slotProps={{ htmlInput: { step: 'any' } }}
              />
            )}
          />

          <Controller
            name="dataHora"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                type="datetime-local"
                label="Data e hora"
                helperText={errors.dataHora?.message}
                error={!!errors.dataHora}
                fullWidth
                slotProps={{ inputLabel: { shrink: true } }}
              />
            )}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={mutacaoEmAndamento}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={mutacaoEmAndamento}
            startIcon={mutacaoEmAndamento ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Salvar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default CotacaoFormDialog
