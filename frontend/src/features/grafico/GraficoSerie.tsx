import ReactApexChart from 'react-apexcharts'
import type { ApexOptions } from 'apexcharts'
import type { Serie } from '../../types/cotacao'
import { formatadorMoeda } from '../../utils/formatadores'

interface GraficoSerieProps {
  serie: Serie
}

function GraficoSerie({ serie }: GraficoSerieProps) {
  const dados = serie.pontos.map((ponto) => ({
    x: new Date(ponto.t).getTime(),
    y: ponto.v,
  }))

  const series = [
    {
      name: serie.indicador.codigo,
      data: dados,
    },
  ]

  const options: ApexOptions = {
    chart: {
      type: 'area',
      height: 380,
      zoom: {
        enabled: true,
      },
      toolbar: {
        show: true,
      },
    },
    dataLabels: {
      enabled: false,
    },
    stroke: {
      curve: 'smooth',
      width: 2,
    },
    xaxis: {
      type: 'datetime',
      labels: {
        datetimeUTC: false,
      },
    },
    tooltip: {
      x: {
        format: 'dd/MM/yyyy HH:mm',
      },
      y: {
        formatter: (valor) => formatadorMoeda.format(valor),
      },
    },
  }

  return <ReactApexChart options={options} series={series} type="area" height={380} />
}

export default GraficoSerie
