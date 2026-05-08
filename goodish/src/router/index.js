import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue') },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetail.vue') },
      { path: 'publish', name: 'Publish', component: () => import('@/views/ProductPublish.vue'), meta: { auth: true } },
      { path: 'edit/:id', name: 'EditProduct', component: () => import('@/views/ProductPublish.vue'), meta: { auth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { auth: true } },
      { path: 'my-products', name: 'MyProducts', component: () => import('@/views/MyProducts.vue'), meta: { auth: true } },
      { path: 'my-orders', name: 'MyOrders', component: () => import('@/views/MyOrders.vue'), meta: { auth: true } },
      { path: 'wallet', name: 'Wallet', component: () => import('@/views/Wallet.vue'), meta: { auth: true } },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/Favorites.vue'), meta: { auth: true } },
      { path: 'follows', name: 'Follows', component: () => import('@/views/Follows.vue'), meta: { auth: true } },
      { path: 'messages', name: 'Messages', component: () => import('@/views/Messages.vue'), meta: { auth: true } },
      { path: 'seller/:id', name: 'Seller', component: () => import('@/views/Seller.vue') },
      { path: 'chat/:otherId', name: 'Chat', component: () => import('@/views/Chat.vue'), meta: { auth: true } },
      { path: 'vip', name: 'Vip', component: () => import('@/views/Vip.vue'), meta: { auth: true } },
      { path: 'admin', name: 'Admin', component: () => import('@/views/Admin.vue'), meta: { auth: true, admin: true } },
    ],
  },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  { path: '/forgot', name: 'Forgot', component: () => import('@/views/ForgotPassword.vue') },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.meta.auth && !token) {
    return next('/login')
  }
  if (to.meta.admin) {
    const roles = JSON.parse(localStorage.getItem('roles') || '[]')
    if (!roles.includes('ROLE_ADMIN')) {
      return next('/')
    }
  }
  next()
})

export default router
