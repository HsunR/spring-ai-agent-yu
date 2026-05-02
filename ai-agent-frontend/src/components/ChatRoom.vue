<template>
  <div class="chat-container">
    <div class="chat-messages" ref="messagesContainer">
      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
        <div v-if="msg.type === 'retrieved-docs'" class="message ai-message retrieved-docs-message">
          <div class="avatar ai-avatar">
            <AiAvatarFallback :type="aiType" />
          </div>
          <div class="message-bubble docs-bubble">
            <div class="docs-header" @click="toggleDocsExpanded">
              <span class="docs-icon">📚</span>
              <span class="docs-title">召回的知识片段 ({{ msg.docs?.length || 0 }})</span>
              <span class="docs-toggle">{{ docsExpanded ? '▼' : '▶' }}</span>
            </div>
            <div v-show="docsExpanded" class="docs-list">
              <div v-for="(doc, docIndex) in msg.docs" :key="docIndex" class="doc-card">
                <div class="doc-header">
                  <span class="doc-title">{{ doc.title || '未命名片段' }}</span>
                  <span class="doc-score" :class="getScoreClass(doc.score)">
                    {{ (doc.score * 100).toFixed(1) }}%
                  </span>
                </div>
                <div class="doc-content">{{ truncateContent(doc.content) }}</div>
                <div class="doc-source">来源: {{ doc.source || 'Spring AI 文档' }}</div>
              </div>
            </div>
            <div v-if="msg.waitingForUserAction" class="docs-action-bar">
              <button @click="handleAnswerWithDocs" class="answer-with-docs-btn">让 AI 基于召回片段回答</button>
            </div>
          </div>
        </div>

        <div v-else-if="!msg.isUser"
             class="message ai-message"
             :class="[msg.type]">
          <div class="avatar ai-avatar">
            <AiAvatarFallback :type="aiType" />
          </div>
          <div class="message-bubble">
            <div class="message-content">
              <template v-if="shouldCollapse(msg.content) && !isMessageExpanded(index)">
                {{ getTruncatedContent(msg.content) }}
              </template>
              <template v-else>
                {{ msg.content }}
              </template>
              <span v-if="connectionStatus === 'connecting' && index === messages.length - 1" class="typing-indicator">▋</span>
            </div>
            <div v-if="shouldCollapse(msg.content)" class="expand-btn-container">
              <button @click="toggleMessageExpand(index)" class="expand-btn">
                {{ isMessageExpanded(index) ? '收起' : '展开全部' }}
              </button>
            </div>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>

        <div v-else class="message user-message" :class="[msg.type]">
          <div class="message-bubble">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
          <div class="avatar user-avatar">
            <div class="avatar-placeholder">我</div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-container">
      <div class="chat-input">
        <textarea
          v-model="inputMessage"
          @keydown.enter.prevent="sendMessage"
          placeholder="请输入消息..."
          class="input-box"
          :disabled="connectionStatus === 'connecting'"
        ></textarea>
        <button
          @click="sendMessage"
          class="send-button"
          :disabled="connectionStatus === 'connecting' || !inputMessage.trim()"
        >发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed, reactive } from 'vue'
import AiAvatarFallback from './AiAvatarFallback.vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  connectionStatus: {
    type: String,
    default: 'disconnected'
  },
  aiType: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['send-message', 'answer-with-docs'])

const inputMessage = ref('')
const messagesContainer = ref(null)
const docsExpanded = ref(true)

const COLLAPSE_THRESHOLD = 300
const expandedMessages = reactive({})

const shouldCollapse = (content) => {
  return content && content.length > COLLAPSE_THRESHOLD
}

const isMessageExpanded = (index) => {
  return expandedMessages[index] === true
}

const toggleMessageExpand = (index) => {
  expandedMessages[index] = !expandedMessages[index]
}

const getTruncatedContent = (content) => {
  if (!content) return ''
  return content.substring(0, COLLAPSE_THRESHOLD) + '...'
}

const aiAvatar = computed(() => {
  const avatarMap = {
    'love': '/ai-love-avatar.png',
    'super': '/ai-super-avatar.png',
    'spring-ai': '/ai-spring-avatar.png'
  }
  return avatarMap[props.aiType] || '/ai-default-avatar.png'
})

const sendMessage = () => {
  if (!inputMessage.value.trim()) return

  emit('send-message', inputMessage.value)
  inputMessage.value = ''
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

watch(() => props.messages.length, () => {
  scrollToBottom()
})

watch(() => props.messages.map(m => m.content).join(''), () => {
  scrollToBottom()
})

onMounted(() => {
  scrollToBottom()
})

const toggleDocsExpanded = () => {
  docsExpanded.value = !docsExpanded.value
}

const truncateContent = (content) => {
  if (!content) return ''
  const maxLength = 200
  return content.length > maxLength ? content.substring(0, maxLength) + '...' : content
}

const getScoreClass = (score) => {
  if (score >= 0.8) return 'score-high'
  if (score >= 0.6) return 'score-medium'
  return 'score-low'
}

const handleAnswerWithDocs = () => {
  emit('answer-with-docs')
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
}

.retrieved-docs-message .docs-bubble {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  max-width: 100%;
}

.docs-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: rgba(0, 240, 255, 0.1);
  cursor: pointer;
  user-select: none;
  transition: background 0.3s;
  border-radius: 18px 18px 0 0;
}

.docs-header:hover {
  background: rgba(0, 240, 255, 0.15);
}

.docs-icon {
  font-size: 18px;
  margin-right: 8px;
}

.docs-title {
  flex: 1;
  color: #00f0ff;
  font-weight: 500;
  font-size: 14px;
}

.docs-toggle {
  color: #00f0ff;
  font-size: 12px;
  transition: transform 0.3s;
}

.docs-list {
  padding: 12px 16px;
  max-height: 250px;
  overflow-y: auto;
}

.doc-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.3s;
}

.doc-card:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(0, 240, 255, 0.4);
}

.doc-card:last-child {
  margin-bottom: 0;
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.doc-title {
  color: #fff;
  font-weight: 500;
  font-size: 14px;
}

.doc-score {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.score-high {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.score-medium {
  background: rgba(255, 204, 0, 0.2);
  color: #ffcc00;
}

.score-low {
  background: rgba(255, 107, 107, 0.2);
  color: #ff6b6b;
}

.doc-content {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.doc-source {
  color: rgba(255, 255, 255, 0.4);
  font-size: 11px;
}

.docs-action-bar {
  padding: 12px 16px;
  display: flex;
  justify-content: center;
  background: rgba(0, 240, 255, 0.05);
  border-top: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 0 0 18px 18px;
}

.answer-with-docs-btn {
  background: linear-gradient(135deg, #00f0ff 0%, #00b4db 100%);
  color: #1a1a2e;
  border: none;
  border-radius: 20px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 240, 255, 0.3);
}

.answer-with-docs-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 240, 255, 0.5);
}

.answer-with-docs-btn:active {
  transform: translateY(0);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-wrapper {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.message {
  display: flex;
  align-items: flex-start;
  max-width: 85%;
  margin-bottom: 8px;
}

.user-message {
  margin-left: auto;
  flex-direction: row;
}

.ai-message {
  margin-right: auto;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  margin-left: 8px;
}

.ai-avatar {
  margin-right: 8px;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #007bff;
  color: white;
  font-weight: bold;
}

.message-bubble {
  padding: 12px;
  border-radius: 18px;
  position: relative;
  word-wrap: break-word;
  min-width: 100px;
}

.user-message .message-bubble {
  background-color: #007bff;
  color: white;
  border-bottom-right-radius: 4px;
  text-align: left;
}

.ai-message .message-bubble {
  background-color: #2a2a4a;
  color: #e0e0e0;
  border-bottom-left-radius: 4px;
  text-align: left;
}

.message-content {
  font-size: 16px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.message-time {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 4px;
  text-align: right;
}

.expand-btn-container {
  margin-top: 8px;
  text-align: center;
}

.expand-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
  opacity: 0.9;
}

.expand-btn:hover {
  opacity: 1;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.expand-btn:active {
  transform: translateY(0);
}

.chat-input-container {
  background-color: white;
  border-top: 1px solid #e0e0e0;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.chat-input {
  display: flex;
  padding: 16px;
  height: 100%;
  box-sizing: border-box;
  align-items: center;
}

.input-box {
  flex-grow: 1;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 10px 16px;
  font-size: 16px;
  resize: none;
  min-height: 20px;
  max-height: 40px;
  outline: none;
  transition: border-color 0.3s;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.input-box::-webkit-scrollbar {
  display: none;
}

.input-box:focus {
  border-color: #007bff;
}

.send-button {
  margin-left: 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 0 20px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
  height: 40px;
  align-self: center;
}

.send-button:hover:not(:disabled) {
  background-color: #0069d9;
}

.typing-indicator {
  display: inline-block;
  animation: blink 0.7s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

.input-box:disabled, .send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .message {
    max-width: 95%;
  }

  .message-content {
    font-size: 15px;
  }

  .chat-input {
    padding: 12px;
  }

  .input-box {
    padding: 8px 12px;
  }

  .send-button {
    padding: 0 15px;
    font-size: 14px;
  }

  .doc-content {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .avatar {
    width: 32px;
    height: 32px;
  }

  .message-bubble {
    padding: 10px;
  }

  .message-content {
    font-size: 14px;
  }

  .chat-input-container {
    height: 64px;
  }

  .chat-messages {
    bottom: 64px;
  }

  .docs-header {
    padding: 10px 12px;
  }

  .docs-title {
    font-size: 13px;
  }

  .doc-card {
    padding: 10px;
  }
}

.ai-answer {
  animation: fadeIn 0.3s ease-in-out;
}

.ai-final {
}

.ai-error {
  opacity: 0.7;
}

.user-question {
}

.ai-message + .ai-message {
  margin-top: 4px;
}

.ai-message + .ai-message .avatar {
  visibility: hidden;
}

.ai-message + .ai-message .message-bubble {
  border-top-left-radius: 10px;
}
</style>