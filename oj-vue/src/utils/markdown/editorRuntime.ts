import katex from 'katex'
import mermaid from 'mermaid'
import type {MermaidConfig} from 'mermaid'
import {config as configureMarkdownEditor} from 'md-editor-v3'
import 'katex/dist/katex.min.css'

const LIGHT_MERMAID_THEME = {
  background: '#ffffff',
  primaryColor: '#e8eef9',
  mainBkg: '#e8eef9',
  nodeBkg: '#e8eef9',
  primaryTextColor: '#172033',
  nodeTextColor: '#172033',
  textColor: '#172033',
  primaryBorderColor: '#5574aa',
  nodeBorder: '#5574aa',
  lineColor: '#64748b',
  defaultLinkColor: '#64748b',
  arrowheadColor: '#64748b',
  secondaryColor: '#f4f7fb',
  secondaryTextColor: '#24324a',
  secondaryBorderColor: '#94a3b8',
  tertiaryColor: '#eef6ff',
  tertiaryTextColor: '#1e293b',
  tertiaryBorderColor: '#93c5fd',
  clusterBkg: '#f8fafc',
  clusterBorder: '#cbd5e1',
  actorBkg: '#e8eef9',
  actorTextColor: '#172033',
  edgeLabelBackground: '#ffffff',
  labelBoxBkgColor: '#ffffff',
  labelTextColor: '#172033',
  noteBkgColor: '#fff7d6',
  noteTextColor: '#422006'
}

const DARK_MERMAID_THEME = {
  background: '#0f172a',
  primaryColor: '#29456f',
  mainBkg: '#29456f',
  nodeBkg: '#29456f',
  primaryTextColor: '#f8fafc',
  nodeTextColor: '#f8fafc',
  textColor: '#f8fafc',
  primaryBorderColor: '#8fb4ff',
  nodeBorder: '#8fb4ff',
  lineColor: '#a8bddb',
  defaultLinkColor: '#a8bddb',
  arrowheadColor: '#a8bddb',
  secondaryColor: '#334155',
  secondaryTextColor: '#f1f5f9',
  secondaryBorderColor: '#94a3b8',
  tertiaryColor: '#4b3b70',
  tertiaryTextColor: '#f8fafc',
  tertiaryBorderColor: '#c4b5fd',
  clusterBkg: '#1e293b',
  clusterBorder: '#64748b',
  actorBkg: '#29456f',
  actorTextColor: '#f8fafc',
  edgeLabelBackground: '#111827',
  labelBoxBkgColor: '#111827',
  labelTextColor: '#f8fafc',
  noteBkgColor: '#5b481c',
  noteTextColor: '#fff7d6'
}

/**
 * md-editor-v3 会把 theme 传给这个回调，但默认 Mermaid 主题的节点颜色
 * 在暗色背景上对比度偏低。这里将站点的明暗主题转换成明确的 Mermaid
 * 主题变量，避免同一张图在不同渲染时机拿到一套不一致的颜色。
 */
export const createMermaidConfig = (config: MermaidConfig): MermaidConfig => {
  const themeVariables = config.theme === 'dark' ? DARK_MERMAID_THEME : LIGHT_MERMAID_THEME

  return {
    ...config,
    themeVariables: {
      ...themeVariables,
      ...(config.themeVariables || {})
    }
  }
}

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
    },
    mermaid: {
      instance: mermaid,
      enableZoom: true
    }
  },
  mermaidConfig: createMermaidConfig
})
