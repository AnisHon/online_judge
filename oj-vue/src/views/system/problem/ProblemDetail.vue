<template>
  <div >
    <el-row justify="center" v-if="problem !== undefined" :gutter="20">

      <el-col class="problem-content common-max-width-page" :span="12" v-show="!isFullScreen">
        <div class="header">
          <h1>{{ problem.problemVo.title }}</h1>
          <div class="tags">
            <el-space v-if="problemType === ProblemType.OJ">
              <el-tag type="danger">
                难度: {{ difficulty }}
              </el-tag>
              <el-tag type="info">
                空间限制: {{ memoryLimit }}MiB
              </el-tag>
              <el-tag type="info">
                时间限制: {{ timeLimit }}ms
              </el-tag>
              <el-tag type="info">
                栈空间限制: {{ stackLimit }}MiB
              </el-tag>
            </el-space>
          </div>
          <div class="tags">
            <el-space>
              <el-tag>
                {{ problem.problemVo.source }}
              </el-tag>
              <el-tag>
                {{ stringProblemType }}
              </el-tag>
              <el-tag v-for="item of problem.tagVo" :key="item.tagId" :color="item.tagColor">
                <span class="common-tag-text-color">
                  {{ item.tagName }}
                </span>
              </el-tag>
            </el-space>
          </div>

        </div>

        <el-divider/>
        <div class="content">
          <h2>题目描述</h2>
          <p class="description">
            <markdown-preview :text="description"></markdown-preview>
          </p>

          <div class="detail-problem">
            <online-judge-problem :problem="ojProblem" v-if="problemType === ProblemType.OJ"/>
            <fill-blank-problem v-if="problemType === ProblemType.FILL"/>
            <choice-choose-problem v-if="problemType === ProblemType.CHOICE"/>
          </div>

          <div class="hint">
            <h2>提示</h2>
            <div>
              <markdown-preview :text="hint" />
            </div>


          </div>

        </div>


      </el-col>

      <el-col  :span="codeSpan" v-if="isShowCodeEditor">

        <enhanced-code-editor :height="height" @submit="onHandleSubmit" @full-screen="onHandleFullScreen"/>


      </el-col>

    </el-row>




  </div>
</template>

<script setup lang="ts">
import {getDetailProblem, type ProblemDetailView, ProblemType, type OjProblemView} from "@/api/problem";
import {useRoute} from "vue-router";
import {computed, onMounted, ref, watch} from "vue";
import EnhancedCodeEditor from './EnhancedCodeEdior/index.vue'
import OnlineJudgeProblem from "@/views/system/problem/OnlineJudgeProblem.vue";
import FillBlankProblem from "@/views/system/problem/FillBlank.vue";
import ChoiceChooseProblem from "@/views/system/problem/ChoiceChoose.vue";
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import type {JudgeForm} from "@/api/problem/judege";


const route = useRoute();

const problem = ref<ProblemDetailView>();

const isFullScreen = ref(false)

const problemType = computed(() => {
  return problem.value?.problemVo.type;
})

const hint = computed(() => {
  const hint = problem.value!.problemVo.hint;
  if (hint === null || hint === undefined || hint === '' || hint.length === 0) {
    return "无";
  } else {
    return hint;
  }
})

const ojProblem = computed(():OjProblemView => {
  return <OjProblemView>problem!.value!.ojProblemVo
});

const isShowCodeEditor = computed((): boolean => {
  if (problem.value === undefined) {
    return false
  }
  const type = problem.value?.problemVo.type;
  return type === ProblemType.OJ
});

const difficulty = computed(() => {
  const array = ['不确定', '简单', '中等', '困难']
  if (problem.value === undefined || problem.value.ojProblemVo === undefined) {
    return "未知";
  }
  return array[problem.value.ojProblemVo.difficulty]
});

// mb
const memoryLimit = computed(() => {
  return Math.ceil(ojProblem.value.memoryLimit / 1024);
});

// ms
const timeLimit = computed(() => {
  return ojProblem.value.timeLimit
});

// mb
const stackLimit = computed(() => {
  return ojProblem.value.stackLimit;
})

const stringProblemType = computed(() => {
  return problemTypeToString(<ProblemType>problemType.value);
});

const description = computed(() => {
  return problem.value!.problemVo.description;
})

const codeSpan = computed(() => {
  return isFullScreen.value ? 16 : 12
})

const onHandleSubmit = (codeForm: JudgeForm) => {
  // todo
}

const onHandleFullScreen = () => {
  isFullScreen.value = !isFullScreen.value;
}

const height = computed(() => {
  const rootStyles = getComputedStyle(document.documentElement);
  const menuHeight = parseFloat(rootStyles.getPropertyValue('--menu-height')); // 访问 CSS 变量
  return  window.innerHeight - menuHeight;
})

watch(height, () => {
  console.log(height.value);
}, {immediate: true});

onMounted(() => {
  const problemId: number = parseInt(<string>route.params.id)

  getDetailProblem(problemId)
      .then((detailProblem: ProblemDetailView) => {
        problem.value = detailProblem;
      })

});

onMounted(() => {

  window.onresize = function() {
    console.log(height.value)
  };
})
</script>


<style scoped>

.problem-content {

  height: var(--content-height);
  overflow: auto;
}

.codeEditor {
  height: var(--content-height);
}

.tags {
  margin: 5px 0;
}

</style>