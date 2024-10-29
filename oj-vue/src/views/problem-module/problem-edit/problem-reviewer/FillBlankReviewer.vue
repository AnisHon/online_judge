<template>
  <div class="fill-blank">

    <el-form>
      <el-form-item v-for="item of judgeForm.answers" :label="itemLabel(item.index)">
        <el-input
            type="textarea"

            :key="item.index"
            v-model="item.answer"
        />
      </el-form-item>

    </el-form>

  </div>

</template>

<script setup lang="ts">
import type {JudgeForm} from "@/api/problem/judge";
import {reactive, ref, toRefs} from "vue";

const {judgeForm, count} = defineProps<{
  judgeForm: JudgeForm,
  count: number
}>()

const itemLabel = (index: number) => {
  return `第${index + 1}项`
}

const judgeForm_ = toRefs(judgeForm)

for (let i = 0; i < count; i++) {
  judgeForm_.answers.value.push(reactive({
    index: i,
    answer: ""
  }))
}


</script>

<style scoped>

</style>