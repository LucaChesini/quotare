import { useMutation, useQueryClient } from '@tanstack/react-query'
import { criarCotacao } from '../../../api/cotacoes'

export function useCriarCotacao() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: criarCotacao,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cotacoes'] })
    },
  })
}
