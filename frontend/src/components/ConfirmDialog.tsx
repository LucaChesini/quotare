import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material'

interface ConfirmDialogProps {
  open: boolean
  titulo: string
  mensagem: string
  erro?: string | null
  carregando?: boolean
  onConfirmar: () => void
  onCancelar: () => void
}

function ConfirmDialog({
  open,
  titulo,
  mensagem,
  erro,
  carregando = false,
  onConfirmar,
  onCancelar,
}: ConfirmDialogProps) {
  return (
    <Dialog open={open} onClose={onCancelar} maxWidth="xs" fullWidth>
      <DialogTitle>{titulo}</DialogTitle>
      <DialogContent>
        <DialogContentText>{mensagem}</DialogContentText>
        {erro && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {erro}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancelar} disabled={carregando}>
          Cancelar
        </Button>
        <Button
          onClick={onConfirmar}
          color="error"
          variant="contained"
          disabled={carregando}
          startIcon={carregando ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          Confirmar
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default ConfirmDialog
