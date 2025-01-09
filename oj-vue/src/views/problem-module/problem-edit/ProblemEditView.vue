<template>
  <div v-loading="isGetLoading">
    <el-row justify="center" :gutter="20">

      <el-col :span="editorSpan">
        <div class="absoluteCenter">
          <el-form :model="problem" label-width="100px" label-position="top" style="max-width: var(--page-max-width)">
            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="题目名称" prop="problem.title">
                  <el-input v-model="problem.problem.title" placeholder="请输入标签名称"/>
                </el-form-item>
              </el-col>
              <el-col :span="smallSpan">
                <el-form-item label="题目类型" prop="problem.type" >
                  <el-select v-model="problem.problem.type" placeholder="请选择类型" :disabled="!isAdd">
                    <el-option v-for="item of dict.problemType" :label="item.label" :value="item.value"/>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="smallSpan">
                <el-form-item label="公开权限" prop="problem.type" >
                  <el-select v-model="problem.problem.auth" placeholder="请选择类型" >
                    <el-option v-for="item of dict.problemAuth" :label="item.label" :value="item.value"/>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="smallSpan" v-if="isOjProblem">
                <el-form-item label="难度" prop="ojProblem.difficulty" >
                  <el-select v-model="problem.ojProblem.difficulty" placeholder="请选择难度">
                    <el-option v-for="item of dict.difficulty" :label="item.label" :value="item.value"/>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="smallSpan"  v-if="isOjProblem">
                <el-form-item label="空间限制KiB" prop="ojProblem.memoryLimit" >
                  <el-input-number :min="0" :controls="false" v-model="problem.ojProblem.memoryLimit" placeholder="空间限制"/>
                </el-form-item>
              </el-col>
              <el-col :span="smallSpan"  v-if="isOjProblem">
                <el-form-item label="时间限制ms" prop="ojProblem.timeLimit" >
                  <el-input-number :min="0" :controls="false" v-model="problem.ojProblem.timeLimit" placeholder="时间限制"/>
                </el-form-item>
              </el-col>
              <el-col :span="smallSpan" v-if="isOjProblem">
                <el-form-item label="栈限制MiB" prop="ojProblem.stackLimit"  >
                  <el-input-number :min="0" :controls="false" v-model="problem.ojProblem.stackLimit" placeholder="栈空间限制"/>
                </el-form-item>
              </el-col>
              <el-col :span="smallSpan">
                <el-form-item label="来源" prop="problem.source">
                  <el-input v-model="problem.problem.source" placeholder="请输入标签名称"/>
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="题目描述" prop="problem.description">
                  <!--                <el-input type="textarea"  v-model="problem.problem.description" placeholder="题目描述"/>-->
                  <MarkDownEditor v-model="problem.problem.description"/>
                </el-form-item>
              </el-col>
              <el-col :span="24" v-if="isOjProblem">
                <el-form-item label="输入描述" prop="ojProblem.input">
                  <MarkDownEditor v-model="problem.ojProblem.input"/>
                </el-form-item>
              </el-col>
              <el-col :span="24" v-if="isOjProblem">
                <el-form-item label="输出描述" prop="ojProblem.output">
                  <MarkDownEditor v-model="problem.ojProblem.output"/>
                </el-form-item>
              </el-col>
              <el-col :span="24" v-if="isOjProblem">
                <el-form-item label="输入用例" prop="ojProblem.inputExample">
                  <MarkDownEditor v-model="problem.ojProblem.inputExample"/>
                </el-form-item>
              </el-col>
              <el-col :span="24" v-if="isOjProblem">
                <el-form-item label="输出用例" prop="ojProblem.outputExample">
                  <MarkDownEditor v-model="problem.ojProblem.outputExample"/>
                </el-form-item>
              </el-col>

              <el-col :span="24">
                <el-form-item label="提示" prop="problem.hint">
                  <MarkDownEditor v-model="<string | undefined>problem.problem.hint"/>
                </el-form-item>
              </el-col>

              <el-col :span="24" v-if="isOjProblem">
                <el-col :span="24" v-for="item of problem.cases" >
                  <el-row>
                    <el-col :span="12">
                      <el-form-item label="测试用例" prop="problem.hint">
                        <el-input type="textarea" v-model="item.input" placeholder="输入用例"/>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="输出用例" prop="problem.hint">
                        <el-input type="textarea" v-model="item.output" placeholder="输出用例"/>
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-row>
                    <el-col :span="12">
                      <el-form-item label="分数" prop="problem.hint">
                        <el-input-number :min="0" :precision="2" v-model="item.score" placeholder="分数"/>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12" style="display: flex; justify-content: center;">
                      <el-button type="danger" @click="deleteCase(item)">删除</el-button>

                    </el-col>



                  </el-row>
                </el-col>
              </el-col>

              <el-col :span="24" v-if="isChoiceProblem">
                <div v-for="item of problem.choices" >
                  <el-row :gutter="20">

                    <el-col :span="12">
                      <el-form-item :label="numberToLetter(<number>item.blankIndex)" prop="problem.hint">
                        <el-input type="textarea" v-model="item.answerText" placeholder="输入用例"/>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="分数" prop="problem.hint">
                        <el-input-number :min="0" :precision="2" v-model="item.score" placeholder="分数"/>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item prop="item.isCorrect" >
                        <el-checkbox  v-model="item.isCorrect" label="是否是正确答案"/>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12" style="display: flex; justify-content: center;">
                      <el-button type="danger" @click="deleteChoice(item)">删除</el-button>
                    </el-col>
                  </el-row>
                </div>
              </el-col>

              <el-col :span="24" v-if="isFillProblem">
                <div v-for="item of problem.choices" >
                  <el-row>

                    <el-col :span="12">
                      <el-form-item label="填空索引" prop="problem.hint">
                        <el-input-number  v-model="item.blankIndex" :controls="false" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="答案" prop="problem.hint">
                        <el-input type="textarea" v-model="item.answerText" placeholder="输入用例"/>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="分数" prop="problem.hint">
                        <el-input-number :min="0" v-model="item.score" :precision="2" placeholder="分数"/>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12" style="display: flex; justify-content: center;">
                      <el-button type="danger" @click="deleteChoice(item)">删除</el-button>
                    </el-col>

                  </el-row>
                </div>
              </el-col>

              <el-col :span="24" style="display: flex; justify-content: center;">
                <el-button type="success" @click="addMore">
                  添加新数据
                </el-button>
              </el-col>




            </el-row>
          </el-form>

        </div>
      </el-col>

      <el-col :span="12" v-show="isShowPreview">
        <ProblemReviewer :problem="problem"/>
      </el-col>

    </el-row>


    <el-row >
      <el-col>
        <el-space alignment="center">
          <el-button type="primary" @click="back">返回</el-button>
          <el-button type="success" @click="submit" :loading="isUpdateLoading || isAddLoading">提交</el-button>
          <el-button type="warning" @click="isShowPreview = !isShowPreview" >显示/隐藏预览</el-button>
        </el-space>
      </el-col>

    </el-row>




  </div>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import {computed, reactive, ref, watch} from "vue";
import __ from "lodash";
import {
  type Answer, debouncedAddProblem,
  debouncedAdminGetProblem, debouncedUpdateProblem, dict, type OjCase,
  type ProblemForm, ProblemType,
} from "@/api/problem";
import ProblemReviewer from "@/views/problem-module/problem-edit/problem-reviewer/ProblemReviewer.vue";
import {numberToLetter} from "@/utils/stringUtils";
import MarkDownEditor from "@/components/MarkDownEditor/MarkDownEditor.vue";



const route = useRoute();

const problemId = computed(() => {
  if (route.query.id) {
    return parseInt(<string>route.query.id)
  } else {
    return undefined;
  }
});

const isShowPreview = ref(false);

const problem = reactive<ProblemForm>({
  problem: {
    problemId: undefined,
    title: undefined,
    description: undefined,
    source: undefined,
    type: undefined,
    auth: undefined,
    hint: undefined,
  },
  ojProblem: {
    problemId: undefined,
    difficulty: undefined,
    memoryLimit: undefined,
    stackLimit: undefined,
    timeLimit: undefined,
    input: undefined,
    output: undefined,
    inputExample: undefined,
    outputExample: undefined,
    createTime: undefined,
  },
  choices: [],
  cases: []
});

const editorSpan = computed(() => {
  return isShowPreview.value ? 12 : 24
})

const smallSpan = computed(() => {
  return isShowPreview.value ? 12 : 6
})

const isAdd = computed((): boolean => {
  return __.isUndefined(problemId.value);
})

const problemType = computed(() => {
  return problem.problem?.type;
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



const router = useRouter();
const back = () => {
  router.push({name: "problem-edit"});
}

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateProblem(problem, () => {})
const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddProblem(problem, (id: number) => {
  router.replace({name: "edit-problem", query: {id: id}});
})

const submit = () => {
  if (isAdd.value) {
    addLoading();
    add();
  } else {
    updateLoading();
    update();
  }
}

const deleteChoice = (item: Answer) => {
  const idx = problem.choices.indexOf(item);

  if (isChoiceProblem.value) {
    if (idx != -1) {
      for (let i = idx + 1; i < problem.choices.length; i++) {

        problem.choices[i].blankIndex = (problem.choices[i].blankIndex || 2) - 1;
      }
      problem.choices.splice(idx, 1);
    }
  } else if (isFillProblem.value) {
    if (idx != -1) {
      problem.choices.splice(idx, 1);
    }
  }

}

const deleteCase = (item: OjCase) => {
  const idx = problem.cases.indexOf(item);
  if (idx != -1) {
    problem.cases.splice(idx, 1);
  }
}

const addMore = () => {
  if (isFillProblem.value) {
    const newAnswer: Answer = {
      answerText: "",
      blankIndex: undefined,
      score: 1
    }
    problem.choices.push(newAnswer);
  } else if (isOjProblem.value) {

    const ojCase: OjCase = {
      input: "",
      output: "",
      score: 1
    }
    problem.cases.push(ojCase);
  } else if (isChoiceProblem.value) {
    const max = __.maxBy(problem.choices, (x) => x.blankIndex );
    const newAnswer: Answer = {
      answerText: "",
      blankIndex: 1,
      isCorrect: false,
      score: 1
    }
    if (max) {
      newAnswer.blankIndex = (max.blankIndex || 1) + 1;
    }
    problem.choices.push(newAnswer);
  }
}

const {isLoading: isGetLoading, loading: getLoading, get} = debouncedAdminGetProblem(problemId.value, (data) => {
  __.assign(problem.problem, data.problem);
  if (data.ojProblem) {
    __.assign(problem.ojProblem, data.ojProblem);
  }
  if (data.cases) {
    problem.cases?.push(...data.cases);

  }
  if (data.choices) {
    problem.choices?.push(...data.choices);
  }

})

// created
if (!isAdd.value) {
  getLoading();
  get();
}

if (isAdd.value) {
  watch(() => problem.problem.type, () => {
    problem.cases.length = 0;
    problem.choices.length = 0;
  })
}





</script>


<style scoped>

</style>