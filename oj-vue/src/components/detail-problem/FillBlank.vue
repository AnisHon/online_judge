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

const judgeForm = defineModel<JudgeForm>({required: true});

const itemLabel = (index: number) => {
  return `第${index + 1}项`
}


</script>

<style scoped>

</style>