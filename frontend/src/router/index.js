import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import AppShell from '../layouts/AppShell.vue'
import Chat from '../views/Chat.vue'
import DietRecords from '../views/DietRecords.vue'
import Analysis from '../views/Analysis.vue'
import Goals from '../views/Goals.vue'
import Profile from '../views/Profile.vue'
import Favorites from '../views/Favorites.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/',
    component: AppShell,
    children: [
      { path: '', name: 'Chat', component: Chat, meta: { flush: true } },
      { path: 'diet', name: 'DietRecords', component: DietRecords },
      { path: 'analysis', name: 'Analysis', component: Analysis },
      { path: 'goals', name: 'Goals', component: Goals },
      { path: 'favorites', name: 'Favorites', component: Favorites },
      { path: 'profile', name: 'Profile', component: Profile },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.name !== 'Login' && to.name !== 'Register') {
    next('/login')
  } else {
    next()
  }
})

export default router
