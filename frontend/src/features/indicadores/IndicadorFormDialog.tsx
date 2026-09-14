import { Controller, useForm } from 'react-hook-form'
import type { DefaultValues, Resolver } from 'react-hook-form'
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
  FormControlLabel,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  TextField,
} from '@mui/material'
import { atualizarIndicadorSchema, criarIndicadorSchema } from './schema'
import type { AtualizarIndicadorFormValues } from './schema'
import { useCriarIndicador } from './hooks/useCriarIndicador'
import { useAtualizarIndicador } from './hooks/useAtualizarIndicador'
import { extrairMensagemErro } from '../../api/erro'
import type { Indicador } from '../../types/indicador'

interface IndicadorFormDialogProps {
  open: boolean
  indicador?: Indicador
  onClose: () => void
}

type IndicadorFormValues = AtualizarIndicadorFormValues

function valoresIniciais(indicador?: Indicador): DefaultValues<IndicadorFormValues> {
  if (indicador) {
    return {
      codigo: indicador.codigo,
      nome: indicador.nome,
      fonte: indicador.fonte,
      ativo: indicador.ativo,
    }
  }
  return { codigo: '', nome: '', fonte: undefined, ativo: true }
}

function IndicadorFormDialog({ open, indicador, onClose }: IndicadorFormDialogProps) {
  const modoEdicao = indicador !== undefined

  const criar = useCriarIndicador()
  const atualizar = useAtualizarIndicador()
  const mutacaoEmAndamento = modoEdicao ? atualizar.isPending : criar.isPending
  const erroMutacao = modoEdicao ? atualizar.error : criar.error

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<IndicadorFormValues>({
    resolver: zodResolver(
      modoEdicao ? atualizarIndicadorSchema : criarIndicadorSchema,
    ) as unknown as Resolver<IndicadorFormValues>,
    defaultValues: valoresIniciais(indicador),
  })

  function onSubmit(valores: IndicadorFormValues) {
    if (modoEdicao && indicador) {
      atualizar.mutate({ id: indicador.id, request: valores }, { onSuccess: onClose })
      return
    }

    criar.mutate({ codigo: valores.codigo, nome: valores.nome, fonte: valores.fonte }, { onSuccess: onClose })
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{modoEdicao ? 'Editar Indicador' : 'Novo Indicador'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)}>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {erroMutacao && <Alert severity="error">{extrairMensagemErro(erroMutacao)}</Alert>}

          <Controller
            name="codigo"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Código"
                helperText={errors.codigo?.message ?? 'Será salvo em maiúsculas'}
                error={!!errors.codigo}
                fullWidth
                autoFocus
              />
            )}
          />

          <Controller
            name="nome"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Nome"
                helperText={errors.nome?.message}
                error={!!errors.nome}
                fullWidth
              />
            )}
          />

          <Controller
            name="fonte"
            control={control}
            render={({ field }) => (
              <FormControl fullWidth error={!!errors.fonte}>
                <InputLabel id="fonte-label">Fonte</InputLabel>
                <Select {...field} labelId="fonte-label" label="Fonte" value={field.value ?? ''}>
                  <MenuItem value="LOCAL">Local</MenuItem>
                  <MenuItem value="EXTERNA">Externa</MenuItem>
                </Select>
                {errors.fonte && <FormHelperText>{errors.fonte.message}</FormHelperText>}
              </FormControl>
            )}
          />

          {modoEdicao && (
            <Controller
              name="ativo"
              control={control}
              render={({ field }) => (
                <FormControlLabel
                  control={
                    <Switch checked={field.value} onChange={(event) => field.onChange(event.target.checked)} />
                  }
                  label="Ativo"
                />
              )}
            />
          )}
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

export default IndicadorFormDialog
