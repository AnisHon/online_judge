<template>
  <Codemirror
      v-model:value="code"
      :options="cmOptions"
      :height="height"
      width="100%"
      class="cm-component"
      :border="true"
      @ready="onReady"
  />
</template>
<script lang="ts" setup>
import {ref, reactive, computed, watch} from "vue";
import { type Editor, type EditorConfiguration } from "codemirror";
import Codemirror from "codemirror-editor-vue3";

import 'codemirror/lib/codemirror.css';


// mode
import "codemirror/mode/python/python.js"
import "codemirror/mode/clike/clike.js"

// theme

import "codemirror/theme/dracula.css"
import 'codemirror/theme/blackboard.css';
import 'codemirror/theme/cobalt.css';
import 'codemirror/theme/eclipse.css';
import 'codemirror/theme/material-darker.css';

import 'codemirror/mode/javascript/javascript.js';
import 'codemirror/mode/css/css.js';
import 'codemirror/mode/clike/clike.js';
import 'codemirror/mode/python/python.js';
import 'codemirror/mode/go/go.js';


// 引入代码自动提示插件
import 'codemirror/addon/hint/show-hint.css';
import 'codemirror/addon/hint/sql-hint';
import 'codemirror/addon/hint/show-hint';
import 'codemirror/addon/hint/javascript-hint'
import 'codemirror/addon/hint/xml-hint'
import 'codemirror/addon/hint/anyword-hint'

import 'codemirror/addon/edit/matchbrackets'
import 'codemirror/addon/edit/closebrackets'

import {useDark} from "@vueuse/core";

const isDark = useDark();

const code = defineModel<string>({required: true})



const {language, height} = defineProps<{
  language: string | 'Java' | 'C' | 'C++' | 'Python2' | 'Python3' | 'Golang',
  height: number,
}>()

const modeMap = {
  'Java' :'text/x-java',
  'C': "text/x-csrc",
  'C With O2': "text/x-csrc",
  'C++': 'text/x-c++src',
  'Python2': 'text/x-python',
  'Python3': 'text/x-python',
  'Golang': 'text/x-go',
}

const mode = computed((): string => {
  // @ts-ignore
  if (language.includes("C++")) {
    return modeMap["C++"];
  }


  //@ts-ignore
  return modeMap[language];
})

const theme = computed(() => {
  return isDark.value ? "material-darker" : "eclipse";
})



const cmOptions: EditorConfiguration = reactive({

  mode: mode.value,
  theme: theme.value,
  readOnly: false,
  tabSize: 4,
  indentUnit: 4,
  indentWithTabs: true,
  lineNumbers: true,
  lineWiseCopyCut: true,
  gutters: ["CodeMirror-lint-markers"],
  lint: true,
  autofocus: true,
  autoCloseBrackets: true,
  autoCloseTags: true,
  matchBrackets: true,
  extraKeys: { // 触发按键
    'Ctrl': 'autocomplete'
  },
  hintOptions: { // 自定义提示选项
    completeSingle: false, // 当匹配只有一项的时候是否自动补全
    tables: {
      users: ['name', 'score', 'birthDate'],
      countries: ['name', 'population', 'size'],
      score: ['zooao']
    }
  }
});


const cminstance = ref<Editor | null>(null);
const onReady = (cm: Editor) => {
  cminstance.value = cm;
  cm.on('keypress', () => {
    //编译器内容更改事件
    cm.showHint();
  })
};

watch(mode, () => {
  cminstance.value?.setOption('mode', mode.value);
}, {immediate: true});

watch(theme, () => {
  cminstance.value?.setOption('theme', theme.value);
  cminstance.value?.refresh()
})



defineExpose({code})


</script>
<style scoped>
.cm-component {
  font-family: monospace;
}
</style>
