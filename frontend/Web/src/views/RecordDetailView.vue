<template>
  <div>
    <PageHeader
      title="检查详情"
      :subtitle="record ? fmtDate(record.examDate) + ' · ' + (record.categoryName || '') : ''"
      back back-text="返回记录列表" back-to="/records"
    />

    <el-card v-loading="loading">
      <template v-if="record">
        <el-descriptions :column="2" border title="检查信息" style="margin-bottom:20px">
          <el-descriptions-item label="检查日期">{{ fmtDate(record.examDate) }}</el-descriptions-item>
          <el-descriptions-item label="检查类型">{{ record.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="检查医院">{{ record.hospital || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="主治医生">{{ record.doctor || '未填写' }}</el-descriptions-item>
          <el-descriptions-item v-if="record.notes" label="备注" :span="2">{{ record.notes }}</el-descriptions-item>
        </el-descriptions>

        <el-card style="margin-bottom:16px" class="insight-card">
          <template #header>
            <div class="ins-head">
              <span>AI 解读</span>
              <div class="ins-actions">
                <span class="mode-hint">模型自己决定查哪些数据，可以连续追问</span>
                <el-button v-if="messages.length" text size="small" @click="resetChat">新对话</el-button>
              </div>
            </div>
          </template>

          <div ref="chatBox" class="chat">
            <div v-if="!messages.length" class="chat-empty">
              <p>问点什么，模型会自己决定查本次结果、历史趋势还是区间规则。</p>
              <div class="chips">
                <el-tag v-for="q in suggestions" :key="q" class="chip" @click="ask(q)">{{ q }}</el-tag>
              </div>
            </div>

            <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
              <div v-if="m.role === 'user'" class="bubble user">{{ m.text }}</div>
              <div v-else class="bubble assistant">
                <el-alert
                  v-if="m.data.outOfScope"
                  type="info" :closable="false" show-icon style="margin-bottom:10px"
                  :title="m.data.scopeNote || '这个问题涉及医学判断，我这边只能提供与它相关的数据'"
                />
                <el-alert
                  v-if="m.data.degraded"
                  type="warning" :closable="false" show-icon style="margin-bottom:10px"
                  :title="(m.data.warnings && m.data.warnings[0]) || '模型不可用，已降级为规则摘要'"
                />
                <p class="ins-summary">{{ m.data.summary }}</p>
                <div v-for="(h, j) in (m.data.highlights || [])" :key="j" class="ins-item">
                  <span class="ins-name">{{ h.indicator }}</span>
                  <span class="ins-text">{{ h.text }}</span>
                  <span v-if="h.basis" class="ins-basis" :title="h.basis">依据</span>
                </div>
                <div v-if="m.data.trace && m.data.trace.length" class="ins-trace">
                  <span class="ins-trace-label">{{ m.data.turns }} 轮 · 工具 {{ m.data.trace.length }} 次</span>
                  <el-tag v-for="(t, j) in m.data.trace" :key="j" size="small" class="ins-tool">
                    {{ t.tool }}{{ t.ok ? '' : '(失败)' }}
                  </el-tag>
                </div>
                <p v-if="m.data.disclaimer" class="ins-disclaimer">{{ m.data.disclaimer }}</p>
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
              placeholder="例如：这次有哪几项异常？和上次比变化大吗？（Enter 发送，Shift+Enter 换行）"
              @keydown.enter.exact.prevent="send"
            />
            <el-button type="primary" :loading="asking" :disabled="!draft.trim()" @click="send">发送</el-button>
          </div>
        </el-card>

        <el-card style="margin-bottom:16px">
          <template #header>
            <span>检查结果</span>
          </template>

          <div class="table-hint">
            参考区间优先级：报告标注 &gt; 按性别年龄规则 &gt; 指标默认值
          </div>

          <el-table :data="record.values" stripe size="small">
            <el-table-column label="指标" min-width="170">
              <template #default="{ row }">
                <div>{{ row.indicatorName }}</div>
                <div v-if="hasRawInfo(row)" class="sub">
                  报告原文：{{ row.rawName || row.indicatorName }}
                  <span v-if="row.rawUnit"> · {{ row.rawUnit }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="结果" width="170">
              <template #default="{ row }">
                <span :class="valueClass(row.isAbnormal)">
                  {{ row.valueText || row.valueDecimal }}
                  <el-tag
                    v-if="row.isAbnormal !== 0"
                    :type="row.isAbnormal === 1 ? 'danger' : 'primary'"
                    size="small"
                    style="margin-left:6px"
                  >
                    {{ row.isAbnormal === 1 ? '↑ 偏高' : '↓ 偏低' }}
                  </el-tag>
                </span>
              </template>
            </el-table-column>

            <el-table-column label="参考区间" width="200">
              <template #default="{ row }">
                <div v-if="row.normalMin != null || row.normalMax != null">
                  {{ row.normalMin ?? '-' }} ~ {{ row.normalMax ?? '-' }}
                </div>
                <div v-else class="text-hint">-</div>
                <el-tooltip
                  v-if="row.refText"
                  :content="'报告印刷：' + row.refText"
                  placement="top"
                >
                  <el-tag :type="sourceTagType(row.refSource)" size="small" effect="plain" style="margin-top:2px">
                    {{ sourceText(row.refSource) }}
                  </el-tag>
                </el-tooltip>
                <el-tag
                  v-else
                  :type="sourceTagType(row.refSource)"
                  size="small"
                  effect="plain"
                  style="margin-top:2px"
                >
                  {{ sourceText(row.refSource) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="单位" width="100">
              <template #default="{ row }">{{ row.unit || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <div v-if="record.imageUrl" style="margin-bottom:16px">
          <el-card header="报告照片">
            <el-image
              :src="fileUrl(record.imageUrl)"
              :preview-src-list="[fileUrl(record.imageUrl)]"
              fit="cover"
              style="max-height:400px"
            />
          </el-card>
        </div>

        <div style="display:flex;gap:12px">
          <el-button type="success" @click="goEdit">编辑记录</el-button>
          <el-button type="danger" @click="handleDelete">删除此记录</el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { fileUrl } from '@/utils/file'
import { ref, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { recordApi } from '@/api/record'
import { insightApi } from '@/api/insight'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const messages = ref([])
const draft = ref('')
/** 只表示「这一轮正在问」，避免与页面加载态同名 */
const asking = ref(false)
const chatBox = ref(null)

const suggestions = [
  '这次有哪几项超出参考区间？',
  '和上次比有什么变化？',
  '我的血糖最近怎么样？'
]

/** 会话只存在当前标签页：刷新还在，关掉就清空 */
const chatKey = 'insight-chat-' + route.params.id

function persistChat() {
  try {
    sessionStorage.setItem(chatKey, JSON.stringify(messages.value))
  } catch (e) { /* 配额满了就放弃缓存，不影响功能 */ }
}

function restoreChat() {
  try {
    const raw = sessionStorage.getItem(chatKey)
    if (raw) messages.value = JSON.parse(raw) || []
  } catch (e) {
    messages.value = []
  }
  scrollToEnd()
}

/** 把一条解读压成纯文本，作为下一轮的上下文 */
function flatten(data) {
  const parts = [data.summary || '']
  for (const h of data.highlights || []) {
    parts.push((h.indicator || '') + ' ' + (h.text || ''))
  }
  return parts.join('\n')
}

/** 已完成的问答（不含本轮） */
function historyPayload() {
  return messages.value.map(m => ({
    role: m.role,
    content: m.role === 'user' ? m.text : flatten(m.data)
  }))
}

async function ask(q) {
  draft.value = q
  await send()
}

async function send() {
  const question = draft.value.trim()
  if (!question || asking.value || !record.value) return

  const history = historyPayload()
  messages.value.push({ role: 'user', text: question })
  draft.value = ''
  asking.value = true
  persistChat()
  scrollToEnd()

  try {
    const res = await insightApi.report(record.value.id, 'agent', question, history)
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
    persistChat()
    scrollToEnd()
  }
}

function resetChat() {
  messages.value = []
  draft.value = ''
  try { sessionStorage.removeItem(chatKey) } catch (e) { /* 忽略 */ }
}

function scrollToEnd() {
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

const record = ref(null)
const loading = ref(true)

function fmtDate(d) { return d ? d.replace('T', ' ') : '' }

function valueClass(isAbnormal) {
  if (isAbnormal === 1) return 'value-abnormal-high'
  if (isAbnormal === 2) return 'value-abnormal-low'
  return ''
}

/** 只有原文与规范名/单位不一致时才展示，避免噪音 */
function hasRawInfo(row) {
  if (!row.rawName && !row.rawUnit) return false
  const nameDiff = row.rawName && row.rawName !== row.indicatorName
  const unitDiff = row.rawUnit && row.rawUnit !== row.unit
  return Boolean(nameDiff || unitDiff)
}

function sourceText(source) {
  if (source === 'report') return '报告标注'
  if (source === 'rule') return '按性别年龄'
  return '默认值'
}

function sourceTagType(source) {
  if (source === 'report') return 'success'
  if (source === 'rule') return 'warning'
  return 'info'
}

function goEdit() { router.push(`/records/${route.params.id}/edit`) }

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除该检查记录？', '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await recordApi.remove(route.params.id)
    router.push('/records')
  } catch (e) { if (e !== 'cancel') alert(e.message) }
}

onMounted(async () => {
  try {
    const res = await recordApi.detail(route.params.id)
    record.value = res.data
    restoreChat()
  } catch (e) { console.error(e) }
  loading.value = false
})
</script>

<style scoped>
.ins-head { display: flex; align-items: center; justify-content: space-between; }
.ins-actions { display: flex; align-items: center; gap: 10px; }
.mode-hint { font-size: var(--font-xs); font-weight: 400; color: var(--text-hint); }

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
.bubble.assistant {
  width: 100%;
  background: var(--bg-white);
  border: 1px solid var(--border);
}
.bubble.thinking {
  display: flex; align-items: center; gap: 8px;
  color: var(--text-hint); font-size: var(--font-sm);
}

.ins-summary { font-size: var(--font-sm); line-height: 1.7; color: var(--text-primary); }
.ins-item { display: flex; align-items: baseline; gap: 8px; padding: 6px 0; border-top: 1px solid var(--border-light); }
.ins-name { font-weight: 600; white-space: nowrap; font-size: var(--font-sm); }
.ins-text { color: var(--text-secondary); font-size: var(--font-sm); }
.ins-basis {
  font-size: var(--font-xs); color: var(--text-hint);
  border-bottom: 1px dashed var(--border-strong); cursor: help;
}
.ins-trace { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 10px; }
.ins-trace-label { font-size: var(--font-xs); color: var(--text-hint); margin-right: 4px; }
.ins-tool { font-family: ui-monospace, monospace; }
.ins-disclaimer { margin-top: 10px; font-size: var(--font-xs); color: var(--text-hint); }

.ask-row { display: flex; align-items: flex-end; gap: 10px; margin-top: 14px; }
.ask-row .el-textarea { flex: 1; }

.table-hint {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
