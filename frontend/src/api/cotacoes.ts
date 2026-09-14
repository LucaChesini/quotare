import { api } from './client'
import type { GranularidadeSerie, Pagina } from '../types/comum'
import type {
  AtualizarCotacaoRequest,
  Cotacao,
  CriarCotacaoRequest,
  Serie,
} from '../types/cotacao'

export async function criarCotacao(dados: CriarCotacaoRequest): Promise<Cotacao> {
  const response = await api.post<Cotacao>('/cotacoes', dados)
  return response.data
}

export async function atualizarCotacao(
  id: number,
  dados: AtualizarCotacaoRequest,
): Promise<Cotacao> {
  const response = await api.put<Cotacao>(`/cotacoes/${id}`, dados)
  return response.data
}

export async function buscarCotacaoPorId(id: number): Promise<Cotacao> {
  const response = await api.get<Cotacao>(`/cotacoes/${id}`)
  return response.data
}

export async function removerCotacao(id: number): Promise<void> {
  await api.delete(`/cotacoes/${id}`)
}

export interface ListarCotacoesParams {
  page?: number
  size?: number
  indicadorId?: number
  inicio?: string
  fim?: string
}

export async function listarCotacoes(
  params: ListarCotacoesParams = {},
): Promise<Pagina<Cotacao>> {
  const { page = 0, size = 20, indicadorId, inicio, fim } = params
  const response = await api.get<Pagina<Cotacao>>('/cotacoes', {
    params: { page, size, indicadorId, inicio, fim },
  })
  return response.data
}

export interface BuscarSerieParams {
  indicadorId: number
  inicio: string
  fim: string
  granularidade?: GranularidadeSerie
}

export async function buscarSerie(params: BuscarSerieParams): Promise<Serie> {
  const response = await api.get<Serie>('/cotacoes/serie', { params })
  return response.data
}
