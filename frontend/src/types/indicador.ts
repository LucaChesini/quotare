import type { FonteDados } from './comum'

export interface Indicador {
  id: number
  codigo: string
  nome: string
  fonte: FonteDados
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarIndicadorRequest {
  codigo: string
  nome: string
  fonte: FonteDados
}

export interface AtualizarIndicadorRequest {
  codigo: string
  nome: string
  fonte: FonteDados
  ativo: boolean
}

export interface IndicadorResumo {
  id: number
  codigo: string
  nome: string
}
