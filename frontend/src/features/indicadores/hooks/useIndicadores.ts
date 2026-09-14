import { useQuery } from '@tanstack/react-query'
import { listarIndicadores } from '../../../api/indicadores'
import type { ListarIndicadoresParams } from '../../../api/indicadores'

export function useIndicadores(params: ListarIndicadoresParams = {}) {
  return useQuery({
    queryKey: ['indicadores', params],
    queryFn: () => listarIndicadores(params),
  })
}
