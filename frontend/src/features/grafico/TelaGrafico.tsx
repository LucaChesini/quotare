import { useState } from 'react'
import {
  Box,
  Button,
  ButtonGroup,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Typography,
} from '@mui/material'
import type { SelectChangeEvent } from '@mui/material'
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider'
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns'
import { DatePicker } from '@mui/x-date-pickers/DatePicker'
import { endOfDay, startOfDay, subDays, subMonths, subYears } from 'date-fns'
import { useIndicadores } from '../indicadores/hooks/useIndicadores'
import { useSerie } from './hooks/useSerie'
import GraficoSerie from './GraficoSerie'
import type { GranularidadeSerie } from '../../types/comum'

const OPCOES_GRANULARIDADE: { valor: GranularidadeSerie; rotulo: string }[] = [
  { valor: 'HORA', rotulo: 'Hora' },
  { valor: 'DIA', rotulo: 'Dia' },
  { valor: 'SEMANA', rotulo: 'Semana' },
  { valor: 'MES', rotulo: 'Mês' },
  { valor: 'BRUTO', rotulo: 'Bruto' },
]

const INTERVALO_MAXIMO_DIAS = 1825

const ATALHOS_PERIODO: { rotulo: string; calcularInicio: (fim: Date) => Date }[] = [
  { rotulo: '7D', calcularInicio: (fim) => subDays(fim, 7) },
  { rotulo: '1M', calcularInicio: (fim) => subMonths(fim, 1) },
  { rotulo: '3M', calcularInicio: (fim) => subMonths(fim, 3) },
  { rotulo: '6M', calcularInicio: (fim) => subMonths(fim, 6) },
  { rotulo: '1A', calcularInicio: (fim) => subYears(fim, 1) },
  { rotulo: 'Tudo', calcularInicio: (fim) => subDays(fim, INTERVALO_MAXIMO_DIAS) },
]

function TelaGrafico() {
  const [indicadorId, setIndicadorId] = useState<number | undefined>(undefined)
  const [granularidade, setGranularidade] = useState<GranularidadeSerie>('DIA')

  const [dataInicio, setDataInicio] = useState<Date | null>(subDays(new Date(), 30))
  const [dataFim, setDataFim] = useState<Date | null>(new Date())

  const { data: dataIndicadores } = useIndicadores({ ativo: true, size: 100 })

  const intervaloValido = Boolean(dataInicio && dataFim && dataInicio <= dataFim)

  const { data: serie } = useSerie({
    indicadorId,
    inicio: intervaloValido ? startOfDay(dataInicio!).toISOString() : undefined,
    fim: intervaloValido ? endOfDay(dataFim!).toISOString() : undefined,
    granularidade,
  })

  function aplicarAtalho(calcularInicio: (fim: Date) => Date) {
    const fim = new Date()
    setDataFim(fim)
    setDataInicio(calcularInicio(fim))
  }

  function handleIndicadorChange(event: SelectChangeEvent<number | ''>) {
    const valor = event.target.value
    setIndicadorId(valor === '' ? undefined : Number(valor))
  }

  function handleGranularidadeChange(event: SelectChangeEvent<GranularidadeSerie>) {
    setGranularidade(event.target.value)
  }

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box>
        <Typography variant="h5" component="h1" sx={{ mb: 3 }}>
          Gráfico
        </Typography>

        <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
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

          <DatePicker
            label="Início"
            value={dataInicio}
            onChange={setDataInicio}
            slotProps={{ textField: { size: 'small' } }}
          />

          <DatePicker
            label="Fim"
            value={dataFim}
            onChange={setDataFim}
            slotProps={{ textField: { size: 'small' } }}
          />
        </Box>

        <ButtonGroup size="small" sx={{ mb: 3 }}>
          {ATALHOS_PERIODO.map((atalho) => (
            <Button key={atalho.rotulo} onClick={() => aplicarAtalho(atalho.calcularInicio)}>
              {atalho.rotulo}
            </Button>
          ))}
        </ButtonGroup>

        {serie && <GraficoSerie serie={serie} />}
      </Box>
    </LocalizationProvider>
  )
}

export default TelaGrafico
