import { useQuery } from '@tanstack/react-query'
import { buscarSerie } from '../../../api/cotacoes'
import type { BuscarSerieParams } from '../../../api/cotacoes'

export type UseSerieParams = Partial<BuscarSerieParams>

function possuiParametrosCompletos(params: UseSerieParams): params is BuscarSerieParams {
  return params.indicadorId !== undefined && params.inicio !== undefined && params.fim !== undefined
}

export function useSerie(params: UseSerieParams) {
  return useQuery({
    queryKey: ['serie', params],
    queryFn: () => {
      if (!possuiParametrosCompletos(params)) {
        throw new Error('Parâmetros obrigatórios da série ausentes')
      }
      return buscarSerie(params)
    },
    enabled: possuiParametrosCompletos(params),
  })
}
