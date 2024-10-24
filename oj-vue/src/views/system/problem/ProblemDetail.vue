<template>
  <div>
    <transition name="el-zoom-in-top">
      <el-row justify="center" v-if="problem !== undefined" :gutter="20">
        <el-col class="problem-content common-max-width-page" ref="contentRef" :span="12" v-show="!isFullScreen">
          <div class="header">
            <h1>{{ problem?.problemVo.title }}</h1>
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
                  {{ problem?.problemVo.source }}
                </el-tag>
                <el-tag>
                  {{ stringProblemType }}
                </el-tag>
                <el-tag v-for="item of problem?.tagVo" :key="item.tagId" :color="item.tagColor">
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
              <online-judge-problem :problem="ojProblem" v-if="isOjProblem"/>
              <fill-blank-problem v-if="isFillProblem" :judgeForm="judgeForm"/>
              <choice-choose-problem :problem-view="problem" :judgeForm="judgeForm" v-if="isChoiceProblem" />
            </div>


            <div>

            </div>
            <div v-if="!isOjProblem">
              <div class="submit">
                <el-button type="success" @click="onHandleSubmit" :loading="isLoading">提交</el-button>
              </div>

              <div class="result" v-show="showAnswers">
                <ProblemResult :type="problemType" :result="judgeResult"/>
              </div>

            </div>


            <div class="hint">
              <h2>提示</h2>
              <div>
                <markdown-preview :text="hint" />
              </div>


            </div>

          </div>


        </el-col>

        <el-col  :span="codeSpan" v-if="isShowCodeEditor" >
          <enhanced-code-editor :heightProp="height" @submit="onHandleSubmit" @full-screen="onHandleFullScreen" @on-ready="onEditorReady"/>
        </el-col>

      </el-row>


    </transition>

  </div>
</template>

<script setup lang="ts">
import {getDetailProblem, type OjProblemView, type ProblemDetailView, ProblemType,} from "@/api/problem";
import {useRoute} from "vue-router";
import {computed, onMounted, onUnmounted, reactive, ref} from "vue";
import EnhancedCodeEditor from './EnhancedCodeEdior/index.vue'
import OnlineJudgeProblem from "@/views/system/problem/OnlineJudgeProblem.vue";
import FillBlankProblem from "@/views/system/problem/FillBlank.vue";
import ChoiceChooseProblem from "@/views/system/problem/ChoiceChoose.vue";
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {getDebouncedJudge, judge, type JudgeForm, type JudgeResponse} from "@/api/problem/judge";
import {debounce} from "@/utils/debounce";
import ProblemResult from "@/views/system/problem/ProblemResult/ProblemResult.vue";
import useLoading from "@/hooks/useLoading";
import {useMitt} from "@/stores/useMitt";


const route = useRoute();

const problem = ref<ProblemDetailView>();

const isFullScreen = ref(false)

const contentRef = ref<InstanceType<typeof EnhancedCodeEditor> | null>(null);

const {loading, finish, isLoading} = useLoading()

const problemId = computed(() => {
  return  parseInt(<string>route.params.id)
})

const judgeForm = reactive<JudgeForm>({
  contestId: undefined,
  problemId: problemId.value,
  answers: [],
});

const judgeResult = ref<JudgeResponse>();

const emitter = useMitt().get();


const doJudge = getDebouncedJudge(judgeForm,
    (data: JudgeResponse) => {
      showAnswers.value = data.answers !== undefined && data.answers.length > 0;
      judgeResult.value = data;
      judgeResult.value?.answers?.sort((a, b) => a.index - b.index);
    },
    undefined,
    finish
)



const showAnswers = ref(false);

// 各种信息的计算属性

const problemType = computed(() => {
  return problem.value?.problemVo.type;
})

const isChoiceProblem = computed(() => {
  return problemType.value === ProblemType.CHOICE || problemType.value === ProblemType.MULTI_CHOICE;
})

const isOjProblem = computed(() => {
  return problemType.value === ProblemType.OJ;
})

const isFillProblem = computed(() => {
  return problemType.value === ProblemType.FILL;
})

const hint = computed(() => {
  const hint = problem.value?.problemVo.hint;
  if (hint === null || hint === undefined || hint === '' || hint.length === 0) {
    return "无";
  } else {
    return hint;
  }
})

const ojProblem = computed(():OjProblemView => {
  return <OjProblemView>problem?.value?.ojProblemVo
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
  return problem.value?.problemVo.description || "";
})

const codeSpan = computed(() => {
  return isFullScreen.value ? 16 : 12
})







// 三个事件

const onHandleSubmit = (codeForm: JudgeForm) => {
  // todo
  loading()
  if (problemType.value === ProblemType.OJ) {
    judgeForm.answers = codeForm.answers;
  } else {
    emitter.emit('judge');
  }

  doJudge();

}

const onHandleFullScreen = () => {
  isFullScreen.value = !isFullScreen.value;
}

const onEditorReady = () => {
  getHeight();
}

const height = ref(0)

const getHeight = () => {
  height.value = contentRef.value?.$el.offsetHeight || 0;
}




onMounted(() => {
  getDetailProblem(problemId.value)
      .then((detailProblem: ProblemDetailView) => {
        problem.value = detailProblem;
      })

});

onMounted(() => {

  const debounceFunc = debounce(getHeight, 100);
  window.onresize = () => {
    debounceFunc()
  }
})

onUnmounted(() => {
  window.onreset = null;
})
</script>


<style scoped>

.problem-content {

  height: var(--in-main-content-height);
  overflow: auto;
}

.submit {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.tags {
  margin: 5px 0;
}

</style>