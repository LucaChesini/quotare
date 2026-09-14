import { useMemo, useState } from 'react'
import {
  Box,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Typography,
} from '@mui/material'
import type { SelectChangeEvent } from '@mui/material'
import { useIndicadores } from '../indicadores/hooks/useIndicadores'
import { useSerie } from './hooks/useSerie'
import type { GranularidadeSerie } from '../../types/comum'

const OPCOES_GRANULARIDADE: { valor: GranularidadeSerie; rotulo: string }[] = [
  { valor: 'HORA', rotulo: 'Hora' },
  { valor: 'DIA', rotulo: 'Dia' },
  { valor: 'SEMANA', rotulo: 'Semana' },
  { valor: 'MES', rotulo: 'Mês' },
  { valor: 'BRUTO', rotulo: 'Bruto' },
]

function TelaGrafico() {
  const [indicadorId, setIndicadorId] = useState<number | undefined>(undefined)
  const [granularidade, setGranularidade] = useState<GranularidadeSerie>('DIA')

  const { data: dataIndicadores } = useIndicadores({ ativo: true, size: 100 })

  const { intervaloTemporarioInicio, intervaloTemporarioFim } = useMemo(() => {
    const fim = new Date()
    const inicio = new Date()
    inicio.setDate(fim.getDate() - 30)
    return {
      intervaloTemporarioInicio: inicio.toISOString(),
      intervaloTemporarioFim: fim.toISOString(),
    }
  }, [])

  const { data: serie } = useSerie({
    indicadorId,
    inicio: intervaloTemporarioInicio,
    fim: intervaloTemporarioFim,
    granularidade,
  })

  function handleIndicadorChange(event: SelectChangeEvent<number | ''>) {
    const valor = event.target.value
    setIndicadorId(valor === '' ? undefined : Number(valor))
  }

  function handleGranularidadeChange(event: SelectChangeEvent<GranularidadeSerie>) {
    setGranularidade(event.target.value)
  }

  return (
    <Box>
      <Typography variant="h5" component="h1" sx={{ mb: 3 }}>
        Gráfico
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel id="grafico-indicador-label">Indicador</InputLabel>
          <Select<number | ''>
            labelId="grafico-indicador-label"
            label="Indicador"
            value={indicadorId ?? ''}
            onChange={handleIndicadorChange}
          >
            {(dataIndicadores?.itens ?? []).map((indicador) => (
              <MenuItem key={indicador.id} value={indicador.id}>
                {indicador.codigo} — {indicador.nome}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel id="grafico-granularidade-label">Granularidade</InputLabel>
          <Select<GranularidadeSerie>
            labelId="grafico-granularidade-label"
            label="Granularidade"
            value={granularidade}
            onChange={handleGranularidadeChange}
          >
            {OPCOES_GRANULARIDADE.map((opcao) => (
              <MenuItem key={opcao.valor} value={opcao.valor}>
                {opcao.rotulo}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {serie && (
        <Typography>{serie.pontos.length} pontos recebidos para {serie.indicador.codigo}</Typography>
      )}
    </Box>
  )
}

export default TelaGrafico
