export type FonteDados = 'LOCAL' | 'EXTERNA'

export type GranularidadeSerie = 'HORA' | 'DIA' | 'SEMANA' | 'MES' | 'BRUTO'

export interface Pagina<T> {
  itens: T[]
  pagina: number
  tamanho: number
  totalItens: number
  totalPaginas: number
}
