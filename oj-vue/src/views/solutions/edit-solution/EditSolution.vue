<template>
  <div class="common-max-width-page app-container">
    <el-page-header @back="router.back" title="返回" content="编辑题解"/>
    <div class="header">
      <el-form
          size="large"
          label-position="left"
          :rules="rules"
          :model="form"
          ref="formRef"
      >
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item prop="title" required>
              <div class="solution-title">
                <el-input
                    style="flex-grow: 1; margin-right: 20px"
                    v-model="form.title"
                    placeholder="请输入标题"
                />
                <el-button
                    icon="upload"
                    @click="submit"
                    type="success"
                    :loading="isLoading"
                >发布题解</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item prop="problemId">
              <el-input v-model="form.problemId" :disabled="fromProblemPage" placeholder="题目"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仅自己可见" style="margin: 0">
              <el-switch v-model="form.private_"/>
            </el-form-item>
          </el-col>
          <el-col v-has="['problem:solution:add', 'problem:solution:edit']" :span="12">
            <el-form-item label="置顶" style="margin: 0">
              <el-switch v-model="form.topUp"/>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>
    <el-divider style="margin-top: 0"/>
    <MarkDownEditor v-model="form.content" class="editor"/>

  </div>
</template>

<script setup lang="ts">
import MarkDownEditor from "@/components/MarkDownEditor/MarkDownEditor.vue";
import {computed, reactive, ref} from "vue";
import {
  debouncedAddSolution,
  debouncedAddSolutionAdmin, debouncedEditSolution, debouncedEditSolutionAdmin,
  getSolution,
  getSolutionAdmin,
  type SolutionForm
} from "@/api/solution";
import {useRoute, useRouter} from "vue-router";
import __ from "lodash";
import {hasPerm} from "@/utils/authUtil.ts";
import useLoading from "@/hooks/useLoading.ts";
import type {IdType} from "@/api/common.ts";
import {type FormInstance, type FormRules} from "element-plus";
import type {ValidateFieldsError} from "async-validator";
import {notifyValidate} from "@/utils/validate.ts";

const route = useRoute();
const router = useRouter();

const form = reactive<SolutionForm>({
  solutionId: undefined,
  problemId: undefined,
  topUp: undefined,
  title: "",
  private_: false,
  content: `# 思路

你选用何种方法解题？

# 解题过程

这些方法具体怎么运用？

# 复杂度

- 时间复杂度: $ O(∗) $

- 空间复杂度: $ O(∗) $

# Code

\`\`\`c++
/* 代码 */
\`\`\``,
})

const formRef = ref<FormInstance>()

const rules = reactive<FormRules<typeof form>>({
  title: [{required: true, message: "请填写题目", trigger: "blur"}],
  problemId:[
    {required: true, message: "请填写题目ID", trigger: "blur"},
    {pattern: /^\d+$/, message: '题目ID只能是数字', trigger: "blur" },
  ],
})

const {loading, isLoading, finish} = useLoading();

const addSolutionAdmin = debouncedAddSolutionAdmin((data: boolean) => {
  if (data) {
    router.back();
  }
}, finish);

const addSolution = debouncedAddSolution((data: boolean) => {
  if (data) {
    router.back();
  }
}, finish);

const editSolutionAdmin = debouncedEditSolutionAdmin((data: boolean) => {
  if (data) {
    router.back();
  }
}, finish);

const editSolution = debouncedEditSolution((data: boolean) => {
  if (data) {
    router.back();
  }
}, finish);

const fromProblemPage = computed(() => {
  return !!route.query.problemId;
})

const isAdd = computed(() => {
  return !route.query.solutionId;
})


const submit = async () => {

  let ok = false;
  if (!formRef.value) return
  await formRef.value.validate((valid, invalidFields?: ValidateFieldsError) => {
    ok = valid;
    notifyValidate(invalidFields);
  })
  if (!ok) {
    return;
  }
  loading();
  const isAdmin = hasPerm("problem:solution:list");
  if (isAdd.value) {
    if (isAdmin) {
      addSolutionAdmin(form);
    } else {
      addSolution(form);
    }
  } else {
    if (isAdmin) {
      editSolutionAdmin(form);
    } else {
      editSolution(form);
    }
  }
}


const fillForm = async () => {
  if (route.query.problemId) {
    form.problemId = <string>route.query.problemId;
  }
  if (route.query.solutionId) {
    form.solutionId = <string>route.query.solutionId;
  }
  if (!isAdd.value) {
    let solution;
    if (hasPerm("problem:solution:list")) {
      solution = await getSolutionAdmin(<IdType>form.solutionId);
    } else {
      solution = await getSolution(<IdType>form.solutionId);
    }
    __.assign(form, solution);
  }
}

fillForm();

</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  margin: auto;
  height: var(--in-main-content-height);
}

.editor {
  flex-grow: 1;
}
.solution-title {
  display: flex;
  width: 100%;
  margin: 5px 0;
}
</style>


<style lang="scss">
.solution-title .el-input__wrapper {
  box-shadow: none;
  height: 32px;
  font-size: 32px;
  font-weight: 700;
}
</style>
