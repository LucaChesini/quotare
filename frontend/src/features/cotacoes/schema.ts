import { z } from 'zod'

export const cotacaoSchema = z.object({
  indicadorId: z.number({ error: 'Selecione um indicador' }),
  valor: z.number({ error: 'O valor é obrigatório' }).positive('O valor deve ser maior que zero'),
  dataHora: z
    .string()
    .min(1, 'A data e hora são obrigatórias')
    .refine((valor) => new Date(valor).getTime() <= Date.now(), {
      message: 'A data e hora não podem estar no futuro',
    }),
})

export type CotacaoFormValues = z.infer<typeof cotacaoSchema>
