<template>
  <div>
<!--    <transition name="el-zoom-in-top">-->
<!--      <el-row justify="center" v-if="problem !== undefined" :gutter="20">-->
<!--        <el-col class="problem-content common-max-width-page" ref="contentRef" :span="12" v-show="!isFullScreen">-->
<!--          <div class="header">-->
<!--            <h1>{{ problem?.problemVo.title }}</h1>-->
<!--            <div class="tags">-->
<!--              <el-space v-if="problemType === ProblemType.OJ">-->
<!--                <el-tag type="danger">-->
<!--                  难度: {{ difficulty }}-->
<!--                </el-tag>-->
<!--                <el-tag type="info">-->
<!--                  空间限制: {{ memoryLimit }}MiB-->
<!--                </el-tag>-->
<!--                <el-tag type="info">-->
<!--                  时间限制: {{ timeLimit }}ms-->
<!--                </el-tag>-->
<!--                <el-tag type="info">-->
<!--                  栈空间限制: {{ stackLimit }}MiB-->
<!--                </el-tag>-->
<!--              </el-space>-->
<!--            </div>-->
<!--            <div class="tags">-->
<!--              <el-space>-->
<!--                <el-tag>-->
<!--                  {{ problem?.problemVo.source }}-->
<!--                </el-tag>-->
<!--                <el-tag>-->
<!--                  {{ stringProblemType }}-->
<!--                </el-tag>-->
<!--                <el-tag v-for="item of problem?.tagVo" :key="item.tagId" :color="item.tagColor">-->
<!--                  <span class="common-tag-text-color">-->
<!--                    {{ item.tagName }}-->
<!--                  </span>-->
<!--                </el-tag>-->
<!--              </el-space>-->
<!--            </div>-->

<!--          </div>-->

<!--          <el-divider/>-->
<!--          <div class="content">-->
<!--            <h2>题目描述</h2>-->
<!--            <p class="description">-->
<!--              <markdown-preview :text="description"></markdown-preview>-->
<!--            </p>-->

<!--            <div class="detail-problem">-->
<!--              <online-judge-problem :problem="ojProblem" v-if="isOjProblem"/>-->
<!--              <fill-blank-problem v-if="isFillProblem" :count="count" :judgeForm="judgeForm"/>-->
<!--              <choice-choose-problem :problem-view="problem" :judgeForm="judgeForm" v-if="isChoiceProblem" />-->
<!--            </div>-->


<!--            <div>-->

<!--            </div>-->
<!--            <div v-if="!isOjProblem">-->
<!--              <div class="submit">-->
<!--                <el-button type="success" @click="onHandleSubmit" :loading="isLoading">提交</el-button>-->
<!--              </div>-->

<!--              <div class="result" v-show="showAnswers">-->
<!--                <ProblemResult :type="problemType" :result="judgeResult"/>-->
<!--              </div>-->

<!--            </div>-->


<!--            <div class="hint">-->
<!--              <h2>提示</h2>-->
<!--              <div>-->
<!--                <markdown-preview :text="hint" />-->
<!--              </div>-->


<!--            </div>-->

<!--          </div>-->


<!--        </el-col>-->

<!--        <el-col  :span="codeSpan" v-if="isShowCodeEditor" >-->
<!--          <enhanced-code-editor :heightProp="height" v-model="judgeForm" @submit="onHandleSubmit" @full-screen="onHandleFullScreen" @on-ready="onEditorReady"/>-->
<!--        </el-col>-->

<!--      </el-row>-->


<!--    </transition>-->

    <detail-problem :problem-id="problemId" :contest-id="undefined"/>
  </div>
</template>

<script setup lang="ts">

import {useRoute} from "vue-router";
import {computed} from "vue";
import DetailProblem from "@/components/detail-problem/DetailProblem.vue";



const route = useRoute();

const problemId = computed(() => {
  return  parseInt(<string>route.params.id)
})

</script>


<style scoped>

</style>