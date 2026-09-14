import { Box, Card, CardContent, Typography } from '@mui/material'
import type { ResumoSerie } from '../../types/cotacao'

const formatadorValor = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 6,
})

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
    resumo.variacaoPercentual > 0
      ? 'success.main'
      : resumo.variacaoPercentual < 0
        ? 'error.main'
        : 'text.secondary'

  return (
    <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
      <Card sx={{ flex: 1, minWidth: 180 }}>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            Mínimo
          </Typography>
          <Typography variant="h6">{formatadorValor.format(resumo.minimo)}</Typography>
        </CardContent>
      </Card>

      <Card sx={{ flex: 1, minWidth: 180 }}>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            Máximo
          </Typography>
          <Typography variant="h6">{formatadorValor.format(resumo.maximo)}</Typography>
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
            {formatadorPercentual.format(resumo.variacaoPercentual)}%
          </Typography>
        </CardContent>
      </Card>
    </Box>
  )
}

export default CardsResumo
