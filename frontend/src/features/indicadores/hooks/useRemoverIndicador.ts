import { useMutation, useQueryClient } from '@tanstack/react-query'
import { removerIndicador } from '../../../api/indicadores'

export function useRemoverIndicador() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: removerIndicador,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['indicadores'] })
    },
  })
}
