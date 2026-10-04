<template>
  <section class="activity-heatmap" aria-label="过去一年练习足迹">
    <header class="heatmap-header">
      <strong>练习足迹</strong>
      <span>{{ total.toLocaleString() }} 次提交 · {{ activeDays }} 个活跃日</span>
    </header>
    <div class="heatmap-scroll" tabindex="0" role="region" aria-label="按天提交次数，可横向滚动">
      <div class="heatmap-calendar">
        <div class="weekday-labels" aria-hidden="true"><span>一</span><span>三</span><span>五</span></div>
        <div class="heatmap-weeks">
          <div v-for="(week, index) in weeks" :key="index" class="heatmap-week">
            <span class="month-label" aria-hidden="true">{{ monthLabel(week, index) }}</span>
            <template v-for="(day, row) in week" :key="day?.date ?? `padding-${row}`">
              <el-tooltip v-if="day" :content="`${day.date}：${day.count} 次提交（北京时间）`" placement="top">
                <span class="heatmap-cell" :data-level="activityLevel(day.count)" tabindex="0"
                      :aria-label="`${day.date}，${day.count} 次提交`" />
              </el-tooltip>
              <span v-else class="heatmap-cell heatmap-cell--padding" aria-hidden="true" />
            </template>
          </div>
        </div>
      </div>
    </div>
    <footer class="heatmap-footer">
      <span>{{ heatmap.startDate }} — {{ heatmap.endDate }} · 北京时间</span>
      <span class="heatmap-legend" aria-label="颜色由浅到深：0、1至2、3至5、6至9、10次及以上提交">
        少 <i v-for="level in 5" :key="level" class="heatmap-cell" :data-level="level - 1" aria-hidden="true" /> 多
      </span>
    </footer>
    <p v-if="!activeDays" class="heatmap-empty">过去一年暂无提交，开始练习，留下你的足迹吧。</p>
  </section>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import type {ProfileActivityDay, ProfileActivityHeatmap} from '@/api/profile'
import {activityLevel, activityWeeks} from './heatmap'

const props = defineProps<{heatmap: ProfileActivityHeatmap}>()
const weeks = computed(() => activityWeeks(props.heatmap.days))
const total = computed(() => props.heatmap.days.reduce((sum, day) => sum + day.count, 0))
const activeDays = computed(() => props.heatmap.days.filter(day => day.count > 0).length)
const monthLabel = (week: (ProfileActivityDay | null)[], index: number) => {
  const firstOfMonth = week.find(day => day?.date.endsWith('-01'))
  const day = firstOfMonth ?? (index === 0 ? week.find(day => day !== null) : null)
  // 第一列不足一周且下一列即为月初时，避免相邻文字重叠。
  if (index === 0 && !firstOfMonth && weeks.value[1]?.some(day => day?.date.endsWith('-01'))) return ''
  return day ? `${Number(day.date.slice(5, 7))}月` : ''
}
</script>

<style scoped>
.activity-heatmap {
  min-width: 0;
  max-width: 100%;
  margin-bottom: 16px;
  padding: 15px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 13px;
  background: var(--el-bg-color);
}
.heatmap-header, .heatmap-footer { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; }
.heatmap-header { margin-bottom: 12px; font-size: 13px; }
.heatmap-header span, .heatmap-footer, .heatmap-empty { color: var(--el-text-color-secondary); font-size: 11px; }
.heatmap-scroll { max-width: 100%; overflow-x: auto; overscroll-behavior-x: contain; padding-bottom: 8px; }
.heatmap-calendar { display: flex; gap: 6px; width: max-content; padding-top: 20px; }
.weekday-labels { display: grid; grid-template-rows: repeat(7, 12px); gap: 3px; color: var(--el-text-color-secondary); font-size: 10px; }
.weekday-labels span:nth-child(1) { grid-row: 2; }
.weekday-labels span:nth-child(2) { grid-row: 4; }
.weekday-labels span:nth-child(3) { grid-row: 6; }
.heatmap-weeks { display: flex; gap: 3px; }
.heatmap-week { position: relative; display: grid; grid-template-rows: repeat(7, 12px); gap: 3px; }
.month-label { position: absolute; top: -20px; left: 0; white-space: nowrap; color: var(--el-text-color-secondary); font-size: 10px; }
.heatmap-cell { display: block; box-sizing: border-box; width: 12px; height: 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 3px; background: var(--el-fill-color); }
.heatmap-cell[data-level="1"] { background: var(--el-color-success-light-7); }
.heatmap-cell[data-level="2"] { background: var(--el-color-success-light-5); }
.heatmap-cell[data-level="3"] { background: var(--el-color-success); }
.heatmap-cell[data-level="4"] { background: var(--el-color-success-dark-2); }
.heatmap-cell--padding { visibility: hidden; }
.heatmap-cell:focus-visible, .heatmap-scroll:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 1px; }
.heatmap-footer { margin-top: 5px; }
.heatmap-legend { display: inline-flex; align-items: center; gap: 4px; }
.heatmap-empty { margin: 12px 0 0; }
</style>
