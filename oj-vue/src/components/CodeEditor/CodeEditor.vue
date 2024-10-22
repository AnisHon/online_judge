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

import 'codemirror/mode/javascript/javascript.js';
import 'codemirror/mode/css/css.js';
import 'codemirror/mode/xml/xml.js';
import 'codemirror/mode/clike/clike.js';
import 'codemirror/mode/markdown/markdown.js';
import 'codemirror/mode/python/python.js';
import 'codemirror/mode/r/r.js';
import 'codemirror/mode/shell/shell.js';
import 'codemirror/mode/sql/sql.js';
import 'codemirror/mode/swift/swift.js';
import 'codemirror/mode/vue/vue.js';

// 引入代码自动提示插件
import 'codemirror/addon/hint/show-hint.css';
import 'codemirror/addon/hint/sql-hint';
import 'codemirror/addon/hint/show-hint';
import 'codemirror/addon/hint/javascript-hint'
import 'codemirror/addon/hint/xml-hint'
import 'codemirror/addon/hint/anyword-hint'

const code = ref("");

const {language, height} = defineProps<{
  language: 'java' | 'c' | 'c++' | 'python' | string,
  height: number,
}>()

const modeMap = {
  'java' :'text/x-java',
  'c': "text/x-csrc",
  'c++': 'text/x-c++src',
  'python': 'text/x-python',
}

const mode = computed((): string => {
  // @ts-ignore
  return modeMap[language]
})



const cmOptions: EditorConfiguration = reactive({

  mode: mode,
  theme: "dracula",
  readOnly: false,
  tabSize: 4,
  line: true,
  lineNumbers: true,
  lineWiseCopyCut: true,
  gutters: ["CodeMirror-lint-markers"],
  lint: true,
  autofocus: true,
  autoCloseBrackets: true,
  autoCloseTags: true,
  matchBrackets: true,
  indentWithTabs: true,
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
  console.log(language)
})

defineExpose({code})


</script>
<style scoped>
.cm-component {
  font-family: monospace;
}
</style>
