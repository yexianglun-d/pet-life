import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import AdminLayout from '@/layouts/AdminLayout.vue';
import { ADMIN_ACCESS_TOKEN_KEY } from '@/shared/constants/adminSession';
import LoginView from '@/views/auth/LoginView.vue';
import CommunityPostGovernanceView from '@/views/community/CommunityPostGovernanceView.vue';
import CommunityQuestionGovernanceView from '@/views/community/CommunityQuestionGovernanceView.vue';
import DailyLogContentView from '@/views/content/DailyLogContentView.vue';
import HealthRecordReviewView from '@/views/content/HealthRecordReviewView.vue';
import TimelineEventDebugView from '@/views/content/TimelineEventDebugView.vue';
import DashboardView from '@/views/dashboard/DashboardView.vue';
import ModerationView from '@/views/moderation/ModerationView.vue';
import MessageTemplateManagementView from '@/views/notification/MessageTemplateManagementView.vue';
import NotificationChannelConfigView from '@/views/notification/NotificationChannelConfigView.vue';
import FamilyManagementView from '@/views/operation/FamilyManagementView.vue';
import PetArchiveQueryView from '@/views/operation/PetArchiveQueryView.vue';
import UserManagementView from '@/views/operation/UserManagementView.vue';
import ReminderTemplateManagementView from '@/views/reminder/ReminderTemplateManagementView.vue';
import SystemReminderQueryView from '@/views/reminder/SystemReminderQueryView.vue';
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
        path: 'community-posts',
        name: 'communityPosts',
        component: CommunityPostGovernanceView
      },
      {
        path: 'community-questions',
        name: 'communityQuestions',
        component: CommunityQuestionGovernanceView
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
        path: 'system-reminders',
        name: 'systemReminders',
        component: SystemReminderQueryView
      },
      {
        path: 'reminder-templates',
        name: 'reminderTemplates',
        component: ReminderTemplateManagementView
      },
      {
        path: 'message-templates',
        name: 'messageTemplates',
        component: MessageTemplateManagementView
      },
      {
        path: 'notification-channels',
        name: 'notificationChannels',
        component: NotificationChannelConfigView
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
