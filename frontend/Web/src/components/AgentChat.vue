<template>
  <el-card class="agent-chat">
    <template #header>
      <div class="head">
        <div class="title-wrap">
          <span class="title">{{ title }}</span>
          <el-tag size="small" effect="plain" class="scope-tag">{{ scopeText }}</el-tag>
        </div>
        <div class="actions">
          <span class="hint">模型自己决定查哪些数据，可以连续追问</span>
          <el-button v-if="messages.length" text size="small" @click="resetChat">新对话</el-button>
        </div>
      </div>
    </template>

    <div ref="chatBox" class="chat">
      <div v-if="!messages.length" class="chat-empty">
        <p>{{ emptyHintText }}</p>
        <div class="chips">
          <el-tag v-for="q in suggestions" :key="q" class="chip" @click="ask(q)">{{ q }}</el-tag>
        </div>
      </div>

      <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
        <div v-if="m.role === 'user'" class="bubble user">{{ m.text }}</div>
        <div v-else class="bubble assistant">
          <el-alert
            v-if="m.data.degraded"
            type="warning" :closable="false" show-icon style="margin-bottom:10px"
            :title="(m.data.warnings && m.data.warnings[0]) || '模型不可用，已降级为规则摘要'"
          />

          <div v-if="m.data.offTopic" class="scope-block">
            <div class="scope-title">这个问题不在我的范围内</div>
            <p class="scope-note">{{ m.data.summary }}</p>
            <div class="scope-actions">
              <el-tag
                v-for="q in ((m.data.suggestions && m.data.suggestions.length) ? m.data.suggestions : suggestions)"
                :key="q" class="chip" @click="ask(q)"
              >{{ q }}</el-tag>
            </div>
          </div>

          <div v-else-if="m.data.outOfScope" class="scope-block">
            <div class="scope-title">这个问题我答不了</div>
            <p class="scope-note">
              {{ m.data.scopeNote || '它涉及病因、危害、严重程度或治疗选择，属于医学判断；我只能提供数据，不给结论。' }}
            </p>
            <div class="scope-actions">
              <el-tag
                v-for="q in scopeSuggestions(m)" :key="q"
                class="chip" @click="ask(q)"
              >{{ q }}</el-tag>
            </div>
          </div>

          <p v-if="!m.data.offTopic && !m.data.outOfScope" class="summary">{{ m.data.summary }}</p>

          <div v-if="m.data.outOfScope" class="data-head">
            {{ (m.data.highlights || []).length
                ? '与你问题相关的数据'
                : ((m.data.warnings && m.data.warnings[0]) || '这次记录里没有与它直接相关的指标') }}
          </div>

          <div v-for="(h, j) in (m.data.highlights || [])" :key="j" class="item">
            <span class="name">{{ h.indicator }}</span>
            <span class="text">{{ h.text }}</span>
            <span v-if="h.basis" class="basis" :title="h.basis">依据</span>
          </div>

          <div v-if="m.data.trace && m.data.trace.length" class="trace">
            <span class="trace-label">{{ m.data.turns }} 轮 · 工具 {{ m.data.trace.length }} 次</span>
            <el-tag v-for="(t, j) in m.data.trace" :key="j" size="small" class="tool">
              {{ t.tool }}{{ t.ok ? '' : '(失败)' }}
            </el-tag>
          </div>

          <p v-if="m.data.disclaimer" class="disclaimer">{{ m.data.disclaimer }}</p>
        </div>
      </div>

      <div v-if="asking" class="msg assistant">
        <div class="bubble assistant thinking">
          <el-icon class="is-loading"><Loading /></el-icon>
          正在查数据…
        </div>
      </div>
    </div>

    <div class="ask-row">
      <el-input
        v-model="draft" type="textarea" :rows="2" resize="none" :disabled="asking"
        :placeholder="placeholder"
        @keydown.enter.exact.prevent="send"
      />
      <el-button type="primary" :loading="asking" :disabled="!draft.trim()" @click="send">发送</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { insightApi } from '@/api/insight'

const props = defineProps({
  /** 有值 = 锁定这条记录；不传 = 全局助手，范围是当前用户全部记录 */
  recordId: { type: Number, default: null },
  title: { type: String, default: 'AI 解读' },
  emptyHint: { type: String, default: '' },
  placeholder: { type: String, default: '例如：这次有哪几项异常？和上次比变化大吗？（Enter 发送，Shift+Enter 换行）' }
})

const messages = ref([])
const draft = ref('')
const asking = ref(false)
const chatBox = ref(null)

const scopeText = computed(() => props.recordId ? '本次记录' : '全部记录')
const suggestions = computed(() => props.recordId
  ? ['这次有哪几项超出参考区间？', '和上次比有什么变化？', '我的血糖最近怎么样？']
  : ['我一共查过几次检查？', '哪项指标有问题？', '我的血糖最近怎么样？'])
const emptyHintText = computed(() => props.emptyHint || (props.recordId
  ? '问点什么，模型会自己决定查本次结果、历史趋势还是区间规则。'
  : '可以问全部记录：一共查过几次、哪项一直偏高、某个指标这几年怎么变。'))

const chatKey = computed(() => 'insight-chat-' + (props.recordId ?? 'all'))

function persist() {
  try { sessionStorage.setItem(chatKey.value, JSON.stringify(messages.value)) } catch (e) { /* 忽略配额 */ }
}

function restore() {
  try {
    const raw = sessionStorage.getItem(chatKey.value)
    messages.value = raw ? (JSON.parse(raw) || []) : []
  } catch (e) {
    messages.value = []
  }
  scrollToEnd()
}

watch(() => props.recordId, restore, { immediate: true })
onMounted(scrollToEnd)

/** 把一条解读压成纯文本，作为下一轮的上下文 */
function flatten(data) {
  const parts = [data.summary || '']
  for (const h of data.highlights || []) parts.push((h.indicator || '') + ' ' + (h.text || ''))
  return parts.join('\n')
}

function historyPayload() {
  return messages.value.map(m => ({
    role: m.role,
    content: m.role === 'user' ? m.text : flatten(m.data)
  }))
}

/** 越界时给「能做什么」的可点选项 */
function scopeSuggestions(m) {
  const ind = (m.data.highlights || [])[0] && m.data.highlights[0].indicator
  const list = []
  if (ind) list.push(ind + '最近几次怎么样？')
  list.push('这次有哪几项超出参考区间？')
  if (ind) list.push(ind + '的参考区间是多少？')
  return list
}

async function ask(q) {
  draft.value = q
  await send()
}

async function send() {
  const question = draft.value.trim()
  if (!question || asking.value) return

  const history = historyPayload()
  messages.value.push({ role: 'user', text: question })
  draft.value = ''
  asking.value = true
  persist()
  scrollToEnd()

  try {
    const res = await insightApi.report(props.recordId, 'agent', question, history)
    messages.value.push({ role: 'assistant', data: res.data })
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      data: {
        summary: '这次请求失败了：' + e.message,
        highlights: [], warnings: [e.message], degraded: true, disclaimer: ''
      }
    })
  } finally {
    asking.value = false
    persist()
    scrollToEnd()
  }
}

function resetChat() {
  messages.value = []
  draft.value = ''
  try { sessionStorage.removeItem(chatKey.value) } catch (e) { /* 忽略 */ }
}

function scrollToEnd() {
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

defineExpose({ resetChat })
</script>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; }
.title-wrap { display: flex; align-items: center; gap: 8px; }
.title { font-weight: 600; }
.scope-tag { color: var(--text-hint); }
.actions { display: flex; align-items: center; gap: 10px; }
.hint { font-size: var(--font-xs); font-weight: 400; color: var(--text-hint); }

.chat { max-height: 460px; overflow-y: auto; padding-right: 4px; }
.chat-empty { padding: 8px 0 4px; color: var(--text-hint); font-size: var(--font-sm); }
.chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.chip { cursor: pointer; }
.chip:hover { color: var(--green-dark); border-color: var(--green); }

.msg { display: flex; margin-bottom: 12px; }
.msg.user { justify-content: flex-end; }
.bubble { border-radius: var(--radius-md); padding: 10px 14px; }
.bubble.user {
  max-width: 86%;
  background: var(--green-light);
  color: var(--text-primary);
  font-size: var(--font-sm);
  line-height: 1.6;
}
.bubble.assistant { width: 100%; background: var(--bg-white); border: 1px solid var(--border); }
.bubble.thinking { display: flex; align-items: center; gap: 8px; color: var(--text-hint); font-size: var(--font-sm); }

.summary { font-size: var(--font-sm); line-height: 1.7; color: var(--text-primary); }
.item { display: flex; align-items: baseline; gap: 8px; padding: 6px 0; border-top: 1px solid var(--border-light); }
.name { font-weight: 600; white-space: nowrap; font-size: var(--font-sm); }
.text { color: var(--text-secondary); font-size: var(--font-sm); }
.basis { font-size: var(--font-xs); color: var(--text-hint); border-bottom: 1px dashed var(--border-strong); cursor: help; }

.scope-block {
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background: var(--bg-hover, #f7f8fa);
  border-left: 3px solid var(--border-strong, #c8c9cc);
}
.scope-title { font-size: var(--font-sm); font-weight: 600; color: var(--text-primary); }
.scope-note { margin-top: 4px; font-size: var(--font-sm); line-height: 1.65; color: var(--text-secondary); }
.scope-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.data-head { margin: 12px 0 2px; font-size: var(--font-xs); color: var(--text-hint); }

.trace { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 10px; }
.trace-label { font-size: var(--font-xs); color: var(--text-hint); margin-right: 4px; }
.tool { font-family: ui-monospace, monospace; }
.disclaimer { margin-top: 10px; font-size: var(--font-xs); color: var(--text-hint); }

.ask-row { display: flex; align-items: flex-end; gap: 10px; margin-top: 14px; }
.ask-row .el-textarea { flex: 1; }
</style>
