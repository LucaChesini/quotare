import axios from 'axios'

interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
}

export function extrairMensagemErro(
  erro: unknown,
  mensagemPadrao = 'Ocorreu um erro inesperado. Tente novamente.',
): string {
  if (axios.isAxiosError<ProblemDetail>(erro)) {
    return erro.response?.data?.detail ?? mensagemPadrao
  }
  return mensagemPadrao
}
