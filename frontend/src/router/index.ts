import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'projects', component: () => import('../views/Projects.vue') },
    { path: '/projects/:id', name: 'project-detail', component: () => import('../views/ProjectDetail.vue') },
    { path: '/upload', name: 'upload', component: () => import('../views/Upload.vue') },
    { path: '/analysis/:id', name: 'analysis', component: () => import('../views/Analysis.vue') },
  ],
})

export default router
