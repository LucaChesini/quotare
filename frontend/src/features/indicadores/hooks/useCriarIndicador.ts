import { useMutation, useQueryClient } from '@tanstack/react-query'
import { criarIndicador } from '../../../api/indicadores'

export function useCriarIndicador() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: criarIndicador,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['indicadores'] })
    },
  })
}
