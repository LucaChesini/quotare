import { z } from 'zod'

export const criarIndicadorSchema = z.object({
  codigo: z
    .string()
    .trim()
    .min(1, 'O código é obrigatório')
    .max(20, 'Máximo de 20 caracteres')
    .regex(/^[A-Za-z0-9]+$/, 'Apenas letras e números, sem espaços ou símbolos'),
  nome: z.string().trim().min(1, 'O nome é obrigatório').max(120, 'Máximo de 120 caracteres'),
  fonte: z.enum(['LOCAL', 'EXTERNA'], { error: 'Selecione uma fonte' }),
})

export const atualizarIndicadorSchema = criarIndicadorSchema.extend({
  ativo: z.boolean(),
})

export type CriarIndicadorFormValues = z.infer<typeof criarIndicadorSchema>
export type AtualizarIndicadorFormValues = z.infer<typeof atualizarIndicadorSchema>
