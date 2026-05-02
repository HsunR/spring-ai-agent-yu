<template>
  <div class="spring-ai-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">Spring AI 知识库</h1>
      <div class="chat-id">会话ID: {{ chatId }}</div>
    </div>

    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom
          :messages="messages"
          :connection-status="connectionStatus"
          ai-type="spring-ai"
          @send-message="handleUserAction"
          @answer-with-docs="answerWithDocs"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import { chatWithSpringAi, retrieveSpringAiDocs } from '../api'

useHead({
  title: 'Spring AI 知识库 - AI超级智能体应用平台',
  meta: [
    {
      name: 'description',
      content: '基于 Spring AI 官方文档的智能问答助手，提供专业的技术解答'
    },
    {
      name: 'keywords',
      content: 'Spring AI,知识库,RAG,智能问答,技术文档,AI助手'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
const waitingForUserAction = ref(false)
const lastQuery = ref('')
let eventSource = null

const generateChatId = () => {
  return 'spring_ai_' + Math.random().toString(36).substring(2, 10)
}

const addMessage = (content, isUser, type = '', docs = null) => {
  const msg = {
    content,
    isUser,
    type,
    time: new Date().getTime()
  }
  if (docs !== null) {
    msg.docs = docs
  }
  messages.value.push(msg)
}

const handleUserAction = (message) => {
  if (waitingForUserAction.value) {
    // 用户发送新消息时，取消当前等待状态，用新消息重新检索
    waitingForUserAction.value = false
    // 移除最后一条 waitingForUserAction 的 retrieved-docs 消息
    const lastIndex = messages.value.length - 1
    if (lastIndex >= 0 && messages.value[lastIndex].type === 'retrieved-docs') {
      messages.value.pop()
    }
  }
  // 如果正在连接中（AI正在回答），关闭旧连接
  if (eventSource) {
    eventSource.close()
    eventSource = null
    // 如果最后一条是空的 AI 回答消息，移除它
    const lastIndex = messages.value.length - 1
    if (lastIndex >= 0 &&
        !messages.value[lastIndex].isUser &&
        messages.value[lastIndex].type === 'ai-answer' &&
        !messages.value[lastIndex].content) {
      messages.value.pop()
    }
  }
  retrieveAndShowDocs(message)
}

const retrieveAndShowDocs = async (message) => {
  addMessage(message, true, 'user-question')

  if (eventSource) {
    eventSource.close()
  }

  waitingForUserAction.value = false
  connectionStatus.value = 'connecting'

  try {
    lastQuery.value = message
    console.log('正在检索相关文档...')
    const response = await retrieveSpringAiDocs(message, 5)
    console.log('检索响应:', response)

    let docs = response.data
    if (docs && Array.isArray(docs)) {
      console.log(`检索到 ${docs.length} 个相关文档`, docs)
    } else if (docs && Array.isArray(docs.data)) {
      docs = docs.data
      console.log(`检索到 ${docs.data.length} 个相关文档`, docs.data)
    } else {
      docs = []
      console.log('未检索到相关文档，响应数据:', docs)
    }

    addMessage('', false, 'retrieved-docs', docs)
    waitingForUserAction.value = true
    messages.value[messages.value.length - 1].waitingForUserAction = true
    connectionStatus.value = 'disconnected'

  } catch (error) {
    console.error('检索失败:', error)
    connectionStatus.value = 'error'
    addMessage('抱歉，检索失败，请稍后重试。', false, 'ai-error')
  }
}

const answerWithDocs = () => {
  if (!waitingForUserAction.value) {
    return
  }

  const docsMessage = messages.value.slice().reverse().find(m => m.type === 'retrieved-docs')
  if (!docsMessage || !docsMessage.docs || docsMessage.docs.length === 0) {
    return
  }

  waitingForUserAction.value = false
  connectionStatus.value = 'connecting'

  const aiMessageIndex = messages.value.length
  addMessage('', false, 'ai-answer')

  eventSource = chatWithSpringAi(
    lastQuery.value,
    chatId.value,
    (data) => {
      if (data && data !== '[DONE]') {
        if (aiMessageIndex < messages.value.length) {
          messages.value[aiMessageIndex].content += data
        }
      }

      if (data === '[DONE]') {
        connectionStatus.value = 'disconnected'
        if (aiMessageIndex < messages.value.length) {
          messages.value[aiMessageIndex].type = 'ai-final'
        }
      }
    },
    (error) => {
      console.error('SSE Error:', error)
      connectionStatus.value = 'error'

      if (aiMessageIndex < messages.value.length &&
          !messages.value[aiMessageIndex].content) {
        messages.value[aiMessageIndex].content = '抱歉，连接出现错误，请稍后重试。'
        messages.value[aiMessageIndex].type = 'ai-error'
      }
    },
    docsMessage.docs
  )
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  chatId.value = generateChatId()

  addMessage('你好！我是 Spring AI 专家助手。我可以帮你解答关于 Spring AI 框架的各种问题。请直接提问，我会先为你检索相关知识片段，你可以选择仅查看片段或让我基于这些片段回答问题。', false)
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.spring-ai-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #1a1a2e;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #00b4db 0%, #0083b0 100%);
  backdrop-filter: blur(10px);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: opacity 0.2s;
}

.back-button:hover {
  opacity: 0.8;
}

.back-button:before {
  content: '←';
  margin-right: 8px;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
}

.chat-id {
  font-size: 14px;
  opacity: 0.8;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.chat-area {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  position: relative;
  height: 0;
}

@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }

  .title {
    font-size: 18px;
  }

  .chat-id {
    font-size: 12px;
  }

  .chat-area {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 10px 12px;
  }

  .back-button {
    font-size: 14px;
  }

  .title {
    font-size: 16px;
  }

  .chat-id {
    display: none;
  }

  .chat-area {
    padding: 8px;
  }
}
</style>