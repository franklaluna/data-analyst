<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <h1 @click="router.push('/')">AI 数据分析助手</h1>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
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
</style>
