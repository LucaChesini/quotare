import { useQuery } from '@tanstack/react-query'
import { listarCotacoes } from '../../../api/cotacoes'
import type { ListarCotacoesParams } from '../../../api/cotacoes'

export function useCotacoes(params: ListarCotacoesParams = {}) {
  return useQuery({
    queryKey: ['cotacoes', params],
    queryFn: () => listarCotacoes(params),
  })
}
