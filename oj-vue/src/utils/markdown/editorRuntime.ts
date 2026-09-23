import katex from 'katex'
import {config as configureMarkdownEditor} from 'md-editor-v3'
import 'katex/dist/katex.min.css'

/**
 * md-editor-v3 默认从 unpkg 动态加载 KaTeX。这样公式是否能正常排版会
 * 依赖运行时网络，而且加载完成前复杂公式会先以普通文本渲染。
 *
 * 统一在应用启动时注入本地 KaTeX 实例，让编辑器和预览使用同一份版本，
 * 也避免两个 Markdown 组件分别配置造成行为不一致。
 */
configureMarkdownEditor({
  editorExtensions: {
    katex: {
      instance: katex
    }
  }
})
