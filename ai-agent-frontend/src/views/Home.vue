<template>
  <div class="home-container">
    <!-- 粒子背景画布 -->
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>
    
    <div class="header" :class="{ 'fade-in': pageLoaded }">
      <div class="glitch-wrapper">
        <h1 class="glitch-title">AI超级智能体</h1>
      </div>
      <p class="subtitle">
        <span ref="typewriterText"></span>
        <span class="cursor" :class="{ 'blink': typewriterComplete }">|</span>
      </p>
      <div class="cyber-line"></div>
    </div>
    
    <div class="apps-container">
      <div 
        v-for="(app, index) in apps" 
        :key="app.path"
        class="app-card" 
        :class="{ 'slide-up': pageLoaded }"
        :style="{ animationDelay: `${0.3 + index * 0.15}s` }"
        @click="navigateTo(app.path)"
        @mousemove="handleCardMouseMove($event, index)"
        @mouseleave="handleCardMouseLeave(index)"
        :ref="el => { if(el) cardRefs[index] = el }"
      >
        <div class="card-glow"></div>
        <div class="card-scan-line"></div>
        <div class="app-icon" :class="app.iconClass">
          <span class="icon-emoji">{{ app.icon }}</span>
        </div>
        <div class="app-info">
          <div class="app-title">{{ app.title }}</div>
          <div class="app-desc">{{ app.desc }}</div>
        </div>
        <div class="app-button">
          <span class="btn-text">立即体验</span>
          <span class="btn-icon">→</span>
        </div>
      </div>
    </div>
    
    <div class="cyber-circles">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    
    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import { ref, onMounted, onUnmounted } from 'vue'

useHead({
  title: 'AI超级智能体应用平台 - 首页',
  meta: [
    {
      name: 'description',
      content: 'AI超级智能体应用平台提供AI恋爱大师和AI超级智能体服务，满足您的各种AI对话需求'
    },
    {
      name: 'keywords',
      content: 'AI智能体,AI应用,AI恋爱大师,AI助手,智能对话,AI超级智能体,首页'
    }
  ]
})

const router = useRouter()
const particleCanvas = ref(null)
const typewriterText = ref(null)
const cardRefs = ref([])
const pageLoaded = ref(false)
const typewriterComplete = ref(false)

const apps = [
  {
    path: '/super-agent',
    icon: '🤖',
    iconClass: 'robot-icon',
    title: 'AI超级智能体',
    desc: '全能型AI助手，解决各类专业问题'
  },
  {
    path: '/spring-ai-knowledge',
    icon: '📘',
    iconClass: 'spring-ai-icon',
    title: 'Spring AI 知识库',
    desc: '基于 Spring AI 官方文档的智能问答助手'
  }
]

const navigateTo = (path) => {
  router.push(path)
}

// 打字机效果
const typewriterEffect = (text, element, speed = 100) => {
  let i = 0
  element.textContent = ''
  
  const type = () => {
    if (i < text.length) {
      element.textContent += text.charAt(i)
      i++
      setTimeout(type, speed)
    } else {
      typewriterComplete.value = true
    }
  }
  
  setTimeout(type, 800)
}

// 粒子背景
class ParticleSystem {
  constructor(canvas) {
    this.canvas = canvas
    this.ctx = canvas.getContext('2d')
    this.particles = []
    this.animationId = null
    this.resize()
    this.init()
    this.animate()
    
    window.addEventListener('resize', () => this.resize())
  }
  
  resize() {
    this.canvas.width = window.innerWidth
    this.canvas.height = window.innerHeight
  }
  
  init() {
    const particleCount = Math.floor((this.canvas.width * this.canvas.height) / 15000)
    this.particles = []
    
    for (let i = 0; i < particleCount; i++) {
      this.particles.push({
        x: Math.random() * this.canvas.width,
        y: Math.random() * this.canvas.height,
        vx: (Math.random() - 0.5) * 0.5,
        vy: (Math.random() - 0.5) * 0.5,
        radius: Math.random() * 2 + 1,
        opacity: Math.random() * 0.5 + 0.2
      })
    }
  }
  
  animate() {
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height)
    
    // 更新和绘制粒子
    this.particles.forEach((particle, i) => {
      particle.x += particle.vx
      particle.y += particle.vy
      
      // 边界检测
      if (particle.x < 0 || particle.x > this.canvas.width) particle.vx *= -1
      if (particle.y < 0 || particle.y > this.canvas.height) particle.vy *= -1
      
      // 绘制粒子
      this.ctx.beginPath()
      this.ctx.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2)
      this.ctx.fillStyle = `rgba(0, 240, 255, ${particle.opacity})`
      this.ctx.fill()
      
      // 绘制连线
      for (let j = i + 1; j < this.particles.length; j++) {
        const dx = this.particles[j].x - particle.x
        const dy = this.particles[j].y - particle.y
        const distance = Math.sqrt(dx * dx + dy * dy)
        
        if (distance < 150) {
          this.ctx.beginPath()
          this.ctx.moveTo(particle.x, particle.y)
          this.ctx.lineTo(this.particles[j].x, this.particles[j].y)
          this.ctx.strokeStyle = `rgba(0, 240, 255, ${0.15 * (1 - distance / 150)})`
          this.ctx.lineWidth = 0.5
          this.ctx.stroke()
        }
      }
    })
    
    this.animationId = requestAnimationFrame(() => this.animate())
  }
  
  destroy() {
    if (this.animationId) {
      cancelAnimationFrame(this.animationId)
    }
  }
}

let particleSystem = null

// 卡片3D倾斜效果
const handleCardMouseMove = (e, index) => {
  const card = cardRefs.value[index]
  if (!card) return
  
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  
  const centerX = rect.width / 2
  const centerY = rect.height / 2
  
  const rotateX = (y - centerY) / 10
  const rotateY = (centerX - x) / 10
  
  card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-15px) scale(1.03)`
}

const handleCardMouseLeave = (index) => {
  const card = cardRefs.value[index]
  if (!card) return
  
  card.style.transform = 'perspective(1000px) rotateX(0) rotateY(0) translateY(0) scale(1)'
}

onMounted(() => {
  // 页面加载动画
  setTimeout(() => {
    pageLoaded.value = true
  }, 100)
  
  // 初始化打字机效果
  if (typewriterText.value) {
    typewriterEffect('/ 探索AI的无限可能 /', typewriterText.value, 80)
  }
  
  // 初始化粒子背景
  if (particleCanvas.value) {
    particleSystem = new ParticleSystem(particleCanvas.value)
  }
})

onUnmounted(() => {
  if (particleSystem) {
    particleSystem.destroy()
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;700&display=swap');

/* 全局样式变量 */
:root {
  --neon-blue: #00f0ff;
  --neon-purple: #9000ff;
  --neon-pink: #ff00d4;
  --cyber-black: #0a0a12;
  --cyber-dark: #111122;
  --cyber-light: #edf7ff;
}

.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--cyber-dark);
  background-image: 
    linear-gradient(0deg, rgba(8, 17, 34, 0.9), rgba(5, 8, 20, 0.9)),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect x="0" y="0" width="100" height="1" fill="%23111133" opacity="0.3"/><rect x="0" y="0" width="1" height="100" fill="%23111133" opacity="0.3"/></svg>');
  background-size: auto, 40px 40px;
  position: relative;
  overflow: hidden;
}

/* 粒子背景画布 */
.particle-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

/* 赛博朋克风格标题 */
.header {
  padding: 70px 20px 50px;
  text-align: center;
  background-color: transparent;
  position: relative;
  z-index: 2;
  opacity: 0;
  transform: translateY(-30px);
  transition: all 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.header.fade-in {
  opacity: 1;
  transform: translateY(0);
}

.glitch-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.glitch-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 3.2rem;
  font-weight: 700;
  color: var(--cyber-light);
  text-shadow: 
    0 0 5px rgba(0, 240, 255, 0.7),
    0 0 10px rgba(0, 240, 255, 0.5),
    0 0 20px rgba(0, 240, 255, 0.3);
  letter-spacing: 2px;
  position: relative;
  animation: glitch 3s infinite;
}

.glitch-title::before,
.glitch-title::after {
  content: 'AI超级智能体';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.8;
}

.glitch-title::before {
  color: var(--neon-pink);
  z-index: -1;
  animation: glitch-anim 2s infinite;
}

.glitch-title::after {
  color: var(--neon-blue);
  z-index: -2;
  animation: glitch-anim-2 3s infinite;
}

.subtitle {
  font-family: 'Orbitron', sans-serif;
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.7);
  max-width: 600px;
  margin: 0 auto 20px;
  letter-spacing: 3px;
  text-transform: uppercase;
  min-height: 1.5em;
}

.cursor {
  color: var(--neon-blue);
  font-weight: 100;
}

.cursor.blink {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.cyber-line {
  height: 2px;
  width: 80%;
  max-width: 600px;
  margin: 0 auto;
  background: linear-gradient(90deg, transparent, var(--neon-blue), transparent);
  position: relative;
}

.cyber-line::before,
.cyber-line::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 10px;
  height: 10px;
  background-color: var(--neon-blue);
  border-radius: 50%;
  transform: translateY(-50%);
  box-shadow: 0 0 10px 2px var(--neon-blue);
}

.cyber-line::before {
  left: 20%;
}

.cyber-line::after {
  right: 20%;
}

.apps-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 50px;
  max-width: 1200px;
  margin: 60px auto;
  padding: 0 20px;
  flex: 1;
  position: relative;
  z-index: 2;
}

.app-card {
  width: 340px;
  background-color: rgba(17, 23, 41, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 
    0 8px 32px rgba(0, 240, 255, 0.2),
    inset 0 0 0 1px rgba(255, 255, 255, 0.1);
  padding: 30px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
  opacity: 0;
  transform: translateY(50px);
  transform-style: preserve-3d;
}

.app-card.slide-up {
  opacity: 1;
  transform: translateY(0);
  animation: slideUp 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle at center,
    rgba(0, 240, 255, 0.1) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.5s;
  pointer-events: none;
}

/* 卡片扫描线效果 */
.card-scan-line {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(0, 240, 255, 0.2),
    transparent
  );
  pointer-events: none;
  opacity: 0;
}

.app-card:hover {
  transform: perspective(1000px) rotateX(0) rotateY(0) translateY(-15px) scale(1.03);
  box-shadow: 
    0 15px 50px rgba(0, 240, 255, 0.3),
    inset 0 0 0 1px rgba(0, 240, 255, 0.5);
}

.app-card:hover .card-glow {
  opacity: 1;
}

.app-card:hover .card-scan-line {
  animation: scanLine 1s ease-in-out;
}

@keyframes scanLine {
  0% {
    left: -100%;
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    left: 100%;
    opacity: 0;
  }
}

.app-icon {
  font-size: 4rem;
  margin-bottom: 25px;
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  position: relative;
  z-index: 1;
  transition: transform 0.3s;
}

.app-card:hover .app-icon {
  transform: scale(1.1) rotate(5deg);
}

.icon-emoji {
  display: block;
  animation: float-icon 3s ease-in-out infinite;
}

@keyframes float-icon {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.love-icon {
  background: linear-gradient(135deg, #ff007a, #ff5722);
  box-shadow: 0 0 20px rgba(255, 0, 122, 0.5);
}

.robot-icon {
  background: linear-gradient(135deg, #00b2ff, #4f56ff);
  box-shadow: 0 0 20px rgba(0, 178, 255, 0.5);
}

.spring-ai-icon {
  background: linear-gradient(135deg, #00b4db, #0083b0);
  box-shadow: 0 0 20px rgba(0, 180, 219, 0.5);
}

.app-info {
  text-align: center;
  margin-bottom: 30px;
  width: 100%;
}

.app-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 1.6rem;
  font-weight: bold;
  color: white;
  margin-bottom: 12px;
  text-shadow: 0 0 10px rgba(0, 240, 255, 0.5);
}

.app-desc {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.app-button {
  background: linear-gradient(90deg, #0088ff, #00b2ff);
  color: white;
  padding: 12px 28px;
  border-radius: 30px;
  font-weight: 500;
  transition: all 0.3s;
  margin-top: auto;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(0, 240, 255, 0.3);
}

.app-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.7s;
}

.app-button:hover {
  box-shadow: 0 0 15px rgba(0, 178, 255, 0.7);
  transform: scale(1.05);
}

.app-button:hover::before {
  left: 100%;
}

.btn-text {
  margin-right: 8px;
  letter-spacing: 1px;
}

.btn-icon {
  font-size: 1.2rem;
  transition: transform 0.3s;
}

.app-button:hover .btn-icon {
  transform: translateX(4px);
}

/* 背景圆圈动画 */
.cyber-circles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
}

.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.15;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
  background: linear-gradient(135deg, var(--neon-blue), var(--neon-purple));
  animation: float 15s infinite alternate;
}

.circle-2 {
  width: 500px;
  height: 500px;
  bottom: -200px;
  left: -200px;
  background: linear-gradient(135deg, var(--neon-purple), var(--neon-pink));
  animation: float 20s infinite alternate-reverse;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 40%;
  right: 15%;
  background: linear-gradient(135deg, var(--neon-pink), var(--neon-blue));
  animation: float 12s infinite alternate;
}

/* 动画效果 */
@keyframes float {
  0% {
    transform: translate(0, 0) rotate(0deg);
  }
  100% {
    transform: translate(50px, 50px) rotate(10deg);
  }
}

@keyframes glitch {
  0% {
    text-shadow: 
      0 0 5px rgba(0, 240, 255, 0.7),
      0 0 10px rgba(0, 240, 255, 0.5);
  }
  50% {
    text-shadow: 
      0 0 5px rgba(0, 240, 255, 0.7),
      0 0 10px rgba(0, 240, 255, 0.5),
      0 0 20px rgba(0, 240, 255, 0.3);
  }
  100% {
    text-shadow: 
      0 0 5px rgba(0, 240, 255, 0.7),
      0 0 10px rgba(0, 240, 255, 0.5);
  }
}

@keyframes glitch-anim {
  0%, 100% {
    transform: translate(0);
  }
  20% {
    transform: translate(-5px, 5px);
  }
  40% {
    transform: translate(-5px, -5px);
  }
  60% {
    transform: translate(5px, 5px);
  }
  80% {
    transform: translate(5px, -5px);
  }
}

@keyframes glitch-anim-2 {
  0%, 100% {
    transform: translate(0);
  }
  20% {
    transform: translate(3px, -3px);
  }
  40% {
    transform: translate(3px, 3px);
  }
  60% {
    transform: translate(-3px, -3px);
  }
  80% {
    transform: translate(-3px, 3px);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .glitch-title {
    font-size: 2.5rem;
  }
  
  .subtitle {
    font-size: 1rem;
  }
  
  .apps-container {
    gap: 30px;
    margin: 40px auto;
  }
  
  .app-card {
    width: 100%;
    max-width: 420px;
    padding: 25px;
  }
  
  .app-icon {
    font-size: 3.5rem;
    width: 80px;
    height: 80px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 50px 15px 40px;
  }
  
  .glitch-title {
    font-size: 2rem;
  }
  
  .subtitle {
    font-size: 0.9rem;
    letter-spacing: 2px;
  }
  
  .apps-container {
    margin: 30px auto;
    padding: 0 15px;
  }
  
  .app-card {
    padding: 20px;
  }
  
  .app-icon {
    font-size: 3rem;
    margin-bottom: 20px;
    width: 70px;
    height: 70px;
  }
  
  .app-title {
    font-size: 1.4rem;
  }
  
  .app-desc {
    font-size: 0.9rem;
  }
  
  .circle-1, .circle-2, .circle-3 {
    opacity: 0.1;
  }
}
</style>
