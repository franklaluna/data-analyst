<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <h1 @click="router.push('/')">AI 数据分析助手</h1>
      <nav class="nav-links">
        <span
          class="nav-link"
          :class="{ active: route.name === 'projects' }"
          @click="router.push('/')"
        >项目</span>
        <span
          class="nav-link"
          :class="{ active: route.name === 'upload' }"
          @click="router.push('/upload')"
        >上传</span>
      </nav>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
const router = useRouter()
const route = useRoute()
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f7fa; }
.app-container { min-height: 100vh; }
.app-header {
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 24px;
}
.app-header h1 { font-size: 20px; cursor: pointer; font-weight: 600; }
.nav-links { margin-left: 40px; display: flex; gap: 24px; }
.nav-link { font-size: 15px; cursor: pointer; padding: 4px 0; opacity: 0.8; transition: opacity 0.2s; }
.nav-link:hover { opacity: 1; }
.nav-link.active { opacity: 1; border-bottom: 2px solid white; font-weight: 600; }
</style>
