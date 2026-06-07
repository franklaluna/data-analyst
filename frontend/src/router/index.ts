import { createRouter, createWebHistory } from 'vue-router'
import Upload from '@/views/Upload.vue'
import Analysis from '@/views/Analysis.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'upload', component: Upload },
    { path: '/analysis/:id', name: 'analysis', component: Analysis },
  ],
})

export default router
