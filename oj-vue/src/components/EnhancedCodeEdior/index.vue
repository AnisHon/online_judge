<template>
  <div>
    <el-form :inline="true" ref="formRef" :model="codeForm">
      <el-form-item>
        <el-button-group>
<!--          <el-button type="success" @click="emit('test')" :icon="IconBug" :loading="loading">-->
<!--            测试-->
<!--          </el-button>-->
          <el-button type="primary" @click="emit('submit')" :icon="Upload" :loading="loading">
            提交
          </el-button>
          <el-button type="success" @click="emit('open-log')" :icon="Upload">
            提交记录
          </el-button>
        </el-button-group>


      </el-form-item>
      <el-form-item label="语言">
        <el-select
            v-model="codeForm.languageId"
            placeholder="Select"
            default-first-option
            style="width: 150px"
        >
          <el-option
              v-for="item in languages"
              :key="item.languageId"
              :label="item.languageName"
              :value="item.languageId"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :icon="FullScreen" @click="onHandleFullScreen">全屏</el-button>
      </el-form-item>


    </el-form>
    <code-editor v-model="codeForm.code" :language="currLang" :theme="theme" :height="codeEditHeight" ref="codeEditorRef" />
  </div>
</template>

<script setup lang="ts">
import CodeEditor from "@/components/CodeEditor/CodeEditor.vue";
import {computed, onMounted, ref, watch} from "vue";
import type {JudgeForm} from "@/api/problem/judge";
import useLanguage from "@/stores/useLanguage";
import type {LanguageView} from "@/api/language";
import {FullScreen, Upload} from "@element-plus/icons-vue";
import IconBug from "@/assets/icons/IconBug.vue";

const languageStore = useLanguage();

const codeEditorRef = ref<InstanceType<typeof CodeEditor> | null>(null);

const formRef = ref<InstanceType<any>>(null)

const codeForm = defineModel<JudgeForm>({required: true})

const theme = ref("eclipse");

const currLang = ref<string>("");

const languages = ref<LanguageView[] | null>(null);

const {heightProp, loading} = defineProps<{heightProp: number, loading: boolean}>()

const emit = defineEmits<{
  (e: 'open-log'): void,
  (e: 'submit'): void;
  (e: 'test'): void;
  (e: 'fullScreen'): void;
  (e: 'onReady'): void;
}>()

const codeEditHeight = computed(() => {
  return heightProp - formRef.value?.$el.offsetHeight - 10;
})


const onHandleFullScreen = () => {
  emit('fullScreen');
};

const setLanguage = () => {
  languages.value?.forEach((item) => {
    if (item.languageId === codeForm.value.languageId) {
      currLang.value = item.languageName;
    }
  })
}

const initLanguages = () => {
  languageStore.getLanguages()
      .then((languageArray) => {
        languages.value = languageArray;
        codeForm.value.languageId = languageArray[0].languageId;
        setLanguage();
      })
}

watch(() => codeForm.value.languageId, () => {
  setLanguage();
})

onMounted(() => {
  emit("onReady")
})

// created
initLanguages();




</script>

<style scoped>

</style>