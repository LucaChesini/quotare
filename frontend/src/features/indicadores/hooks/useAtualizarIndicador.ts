import { useMutation, useQueryClient } from '@tanstack/react-query'
import { atualizarIndicador } from '../../../api/indicadores'
import type { AtualizarIndicadorRequest } from '../../../types/indicador'

interface AtualizarIndicadorVariaveis {
  id: number
  request: AtualizarIndicadorRequest
}

export function useAtualizarIndicador() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, request }: AtualizarIndicadorVariaveis) => atualizarIndicador(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['indicadores'] })
    },
  })
}
