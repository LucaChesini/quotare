import type { FonteDados, GranularidadeSerie } from './comum'
import type { IndicadorResumo } from './indicador'

export interface Cotacao {
  id: number
  indicadorId: number
  indicador: IndicadorResumo
  valor: number
  dataHora: string
  fonte: FonteDados
  criadoEm: string
}

export interface CriarCotacaoRequest {
  indicadorId: number
  valor: number
  dataHora: string
}

export interface AtualizarCotacaoRequest {
  indicadorId: number
  valor: number
  dataHora: string
}

export interface Ponto {
  t: string
  v: number
}

export interface ResumoSerie {
  minimo: number | null
  maximo: number | null
  variacaoPercentual: number | null
}

export interface Serie {
  indicador: IndicadorResumo
  granularidade: GranularidadeSerie
  pontos: Ponto[]
  resumo: ResumoSerie
}
