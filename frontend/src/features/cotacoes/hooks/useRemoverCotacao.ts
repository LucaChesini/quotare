import { useMutation, useQueryClient } from '@tanstack/react-query'
import { removerCotacao } from '../../../api/cotacoes'

export function useRemoverCotacao() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: removerCotacao,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cotacoes'] })
    },
  })
}
