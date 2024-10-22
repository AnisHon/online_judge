<template>
  <div >
    <el-row justify="center" v-if="problem !== undefined">

      <el-col class="problem-content common-max-width-page" :span="12">
        <div class="header">
          <h1>{{ problem.problemVo.title }}</h1>
          <el-space v-if="problemType === ProblemType.OJ">
            <el-tag>
              {{  }}
            </el-tag>
          </el-space>
          <el-space>
            <el-tag v-for="item of problem.tagVo" :key="item.tagId" :color="item.tagColor">
              <span class="common-tag-text-color">
                {{ item.tagName }}
              </span>
            </el-tag>
          </el-space>
        </div>

        <el-divider/>
        <div class="content">
          <p class="description">
            {{ problem.problemVo.description }}
          </p>

          <div class="detail-problem">
            <online-judge-problem v-if="problemType === ProblemType.OJ"/>
            <fill-blank-problem v-if="problemType === ProblemType.FILL"/>
            <choice-choose-problem v-if="problemType === ProblemType.CHOICE"/>
          </div>

        </div>


      </el-col>

      <el-col class="codeEditor" :span="12" v-if="isShowCodeEditor">
        <code-editor language="c++" :height="100"/>
      </el-col>

    </el-row>




  </div>
</template>

<script setup lang="ts">
import {getDetailProblem, type ProblemDetailView, ProblemType} from "@/api/problem";
import {useRoute} from "vue-router";
import {computed, onMounted, ref} from "vue";
import CodeEditor from "@/components/CodeEditor/CodeEditor.vue";
import OnlineJudgeProblem from "@/views/system/problem/OnlineJudgeProblem.vue";
import FillBlankProblem from "@/views/system/problem/FillBlank.vue";
import ChoiceChooseProblem from "@/views/system/problem/ChoiceChoose.vue";

const route = useRoute();

let problem = ref<ProblemDetailView>();

const problemType = computed(() => {
  return problem.value?.problemVo.type;
})

const isShowCodeEditor = computed((): boolean => {
  if (problem.value === undefined) {
    return false
  }
  const type = problem.value?.problemVo.type;
  return type === ProblemType.OJ
});


onMounted(() => {
  const problemId: number = parseInt(<string>route.params.id)

  getDetailProblem(problemId)
      .then((detailProblem: ProblemDetailView) => {
        problem.value = detailProblem;
      })

})


</script>


<style scoped>

</style>