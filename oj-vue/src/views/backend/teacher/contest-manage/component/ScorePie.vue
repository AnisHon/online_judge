<template>
  <div class="chart-container">
    <Pie :data="chartData" :options="chartOptions" />
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {ArcElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Title, Tooltip} from 'chart.js';
import {Pie} from 'vue-chartjs';
import {round} from "lodash";

const {max, min = 0, data, precision = 0} = defineProps<{max: number, min?: number, data: number[], precision?: number}>()

const getPercent = (value: number) => {
  return (value - min) / (max - min)
}

const getLabel = (percentage: number) => {
  return round((max - min) * percentage + min, precision);
}

const label = computed(() => {
  return [
      `>=${getLabel(0.9)}`,
      `>=${getLabel(0.8)}`,
      `>=${getLabel(0.7)}`,
      `>=${getLabel(0.6)}`,
      `<${getLabel(0.6)}`,
  ]
})

const percentage = computed(() => {
  const temp = [0, 0, 0, 0, 0];
  data.forEach((item) => {
    if (getPercent(item) > 0.9) {
      temp[0]++;
    } else if (getPercent(item) >= 0.8) {
      temp[1]++;
    } else if (getPercent(item) >= 0.7) {
      temp[2]++;
    } else if (getPercent(item) >= 0.6) {
      temp[3]++;
    } else {
      temp[4]++;
    }
  });
  return temp;
})


// 注册 Chart.js 插件
ChartJS.register(Title, Tooltip, Legend, ArcElement, CategoryScale, LinearScale);
const chartData = ref({
  labels: ['Red', 'Blue', 'Yellow'],
  datasets: [
    {
      label: 'My First Dataset',
      data: percentage,
      backgroundColor: ['#67C23A', '#409EFF', '#E6A23C', '#909399','#F56C6C'],
      hoverOffset: 4,
    },
  ],
});

// 配置选项
const chartOptions = ref({
  responsive: true,
  plugins: {
    legend: {
      position: 'left',
    },
  },
});
</script>

<style scoped>
.chart-container {
  width: 400px;  /* 设置宽度 */
  height: 400px; /* 设置高度 */
}
</style>
