import { api } from './client'
import type { FonteDados, Pagina } from '../types/comum'
import type {
  AtualizarIndicadorRequest,
  CriarIndicadorRequest,
  Indicador,
} from '../types/indicador'

export async function criarIndicador(dados: CriarIndicadorRequest): Promise<Indicador> {
  const response = await api.post<Indicador>('/indicadores', dados)
  return response.data
}

export async function atualizarIndicador(
  id: number,
  dados: AtualizarIndicadorRequest,
): Promise<Indicador> {
  const response = await api.put<Indicador>(`/indicadores/${id}`, dados)
  return response.data
}

export async function buscarIndicadorPorId(id: number): Promise<Indicador> {
  const response = await api.get<Indicador>(`/indicadores/${id}`)
  return response.data
}

export async function removerIndicador(id: number): Promise<void> {
  await api.delete(`/indicadores/${id}`)
}

export interface ListarIndicadoresParams {
  page?: number
  size?: number
  fonte?: FonteDados
  ativo?: boolean
}

export async function listarIndicadores(
  params: ListarIndicadoresParams = {},
): Promise<Pagina<Indicador>> {
  const { page = 0, size = 20, fonte, ativo } = params
  const response = await api.get<Pagina<Indicador>>('/indicadores', {
    params: { page, size, fonte, ativo },
  })
  return response.data
}
