import { Box, Card, CardContent, Typography } from '@mui/material'
import type { ResumoSerie } from '../../types/cotacao'
import { formatadorMoeda } from '../../utils/formatadores'

const formatadorPercentual = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  signDisplay: 'exceptZero',
})

interface CardsResumoProps {
  resumo: ResumoSerie
}

function CardsResumo({ resumo }: CardsResumoProps) {
  const corVariacao =
    resumo.variacaoPercentual === null
      ? 'text.secondary'
      : resumo.variacaoPercentual > 0
        ? 'success.main'
        : resumo.variacaoPercentual < 0
          ? 'error.main'
          : 'text.secondary'

  const textoMinimo = resumo.minimo === null ? '—' : formatadorMoeda.format(resumo.minimo)
  const textoMaximo = resumo.maximo === null ? '—' : formatadorMoeda.format(resumo.maximo)
  const textoVariacao =
    resumo.variacaoPercentual === null
      ? '—'
      : `${formatadorPercentual.format(resumo.variacaoPercentual)}%`

  return (
    <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
      <Card sx={{ flex: 1, minWidth: 180 }}>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            Mínimo
          </Typography>
          <Typography variant="h6">{textoMinimo}</Typography>
        </CardContent>
      </Card>

      <Card sx={{ flex: 1, minWidth: 180 }}>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            Máximo
          </Typography>
          <Typography variant="h6">{textoMaximo}</Typography>
        </CardContent>
      </Card>

      <Card sx={{ flex: 1, minWidth: 180 }}>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            Variação
          </Typography>
          <Typography
            variant="h6"
            sx={{ color: corVariacao }}
          >
            {textoVariacao}
          </Typography>
        </CardContent>
      </Card>
    </Box>
  )
}

export default CardsResumo
