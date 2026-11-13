import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    component: { template: '<div />' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

const router = createRouter({
  history: createWebHistory('/admin-ui'),
  routes
});

export default router;
