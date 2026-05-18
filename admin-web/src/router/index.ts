import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import AdminLayout from '@/layouts/AdminLayout.vue';
import { ADMIN_ACCESS_TOKEN_KEY } from '@/shared/constants/adminSession';
import LoginView from '@/views/auth/LoginView.vue';
import DailyLogContentView from '@/views/content/DailyLogContentView.vue';
import HealthRecordReviewView from '@/views/content/HealthRecordReviewView.vue';
import TimelineEventDebugView from '@/views/content/TimelineEventDebugView.vue';
import DashboardView from '@/views/dashboard/DashboardView.vue';
import ModerationView from '@/views/moderation/ModerationView.vue';
import FamilyManagementView from '@/views/operation/FamilyManagementView.vue';
import PetArchiveQueryView from '@/views/operation/PetArchiveQueryView.vue';
import UserManagementView from '@/views/operation/UserManagementView.vue';
import ServiceProviderView from '@/views/service/ServiceProviderView.vue';
import SystemConfigView from '@/views/system/SystemConfigView.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView
      },
      {
        path: 'moderation',
        name: 'moderation',
        component: ModerationView
      },
      {
        path: 'users',
        name: 'users',
        component: UserManagementView
      },
      {
        path: 'families',
        name: 'families',
        component: FamilyManagementView
      },
      {
        path: 'pets',
        name: 'pets',
        component: PetArchiveQueryView
      },
      {
        path: 'health-records',
        name: 'healthRecords',
        component: HealthRecordReviewView
      },
      {
        path: 'daily-logs',
        name: 'dailyLogs',
        component: DailyLogContentView
      },
      {
        path: 'timeline-events',
        name: 'timelineEvents',
        component: TimelineEventDebugView
      },
      {
        path: 'service-providers',
        name: 'serviceProviders',
        component: ServiceProviderView
      },
      {
        path: 'system-config',
        name: 'systemConfig',
        component: SystemConfigView
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const accessToken = window.localStorage.getItem(ADMIN_ACCESS_TOKEN_KEY);

  if (to.meta.requiresAuth && !accessToken) {
    return { name: 'login' };
  }

  if (to.name === 'login' && accessToken) {
    return { name: 'dashboard' };
  }

  return true;
});

export default router;
