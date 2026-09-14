import { useMutation, useQueryClient } from '@tanstack/react-query'
import { atualizarCotacao } from '../../../api/cotacoes'
import type { AtualizarCotacaoRequest } from '../../../types/cotacao'

interface AtualizarCotacaoVariaveis {
  id: number
  request: AtualizarCotacaoRequest
}

export function useAtualizarCotacao() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, request }: AtualizarCotacaoVariaveis) => atualizarCotacao(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cotacoes'] })
    },
  })
}
