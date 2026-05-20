<template>
  <section class="page-section service-map-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">地图能力排查</p>
      <h1 class="page-section__title">维护服务商坐标，排查距离能力底座</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">服务商 {{ providers.length }} 家</span>
        <span class="pet-admin-chip">已维护坐标 {{ locatedProviderCount }} 家</span>
        <span class="pet-admin-chip">缺坐标 {{ missingCoordinateCount }} 家</span>
        <span class="pet-admin-chip">地图配置 {{ mapConfigStatusLabel }}</span>
      </div>
    </div>

    <div class="summary-grid service-map-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <div class="pet-admin-grid pet-admin-grid--two service-map-panels">
      <article class="pet-admin-panel">
        <div class="service-map-panel-heading">
          <div>
            <h2 class="pet-admin-panel__title">地图服务配置</h2>
          </div>
          <el-button :loading="mapConfigLoading" @click="loadMapConfig">刷新配置</el-button>
        </div>

        <el-alert
          v-if="mapConfigErrorMessage"
          :title="mapConfigErrorMessage"
          type="error"
          show-icon
          class="service-map-error"
          :closable="false"
        />

        <dl v-if="mapConfig" class="service-map-config-list">
          <div>
            <dt>服务商</dt>
            <dd>{{ mapConfig.provider_code }}</dd>
          </div>
          <div>
            <dt>配置状态</dt>
            <dd>
              <el-tag :type="mapConfigStatusTagType">
                {{ mapConfig.configured ? 'Web 服务 Key 已配置' : 'Web 服务 Key 未配置' }}
              </el-tag>
            </dd>
          </div>
          <div>
            <dt>服务地址</dt>
            <dd>{{ mapConfig.base_url }}</dd>
          </div>
          <div>
            <dt>能力列表</dt>
            <dd class="service-map-tags">
              <el-tag v-for="capability in mapConfig.capabilities" :key="capability" type="info">
                {{ capabilityLabel(capability) }}
              </el-tag>
              <span v-if="mapConfig.capabilities.length === 0">暂无能力</span>
            </dd>
          </div>
        </dl>
        <div v-else class="service-map-empty-inline">
          {{ mapConfigLoading ? '正在读取地图配置状态' : '地图配置状态暂不可用，请刷新重试' }}
        </div>
      </article>

      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">距离能力排查入口</h2>
        <div class="service-map-readiness">
          <div>
            <strong>{{ locatedProviderCount }}</strong>
            <span>可参与距离计算</span>
          </div>
          <div>
            <strong>{{ missingCoordinateCount }}</strong>
            <span>缺少坐标</span>
          </div>
          <div>
            <strong>{{ distanceCapabilityReady ? '就绪' : '待确认' }}</strong>
            <span>distance 能力</span>
          </div>
        </div>
        <div class="service-map-distance-note">
          <p>若用户端附近排序异常，先检查服务商是否有经纬度，再检查服务端地图配置与距离能力。</p>
          <p>真实路线距离、导航和地图可视化不在本轮 admin-web 范围内。</p>
        </div>
      </article>
    </div>

    <article class="pet-admin-panel service-map-section">
      <div class="service-map-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">服务商坐标维护</h2>
        </div>
        <div class="service-map-toolbar__actions">
          <el-select v-model="providerFilters.providerType" size="small" class="service-map-filter" placeholder="服务类型">
            <el-option label="全部服务" value="all" />
            <el-option
              v-for="option in providerTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="providerFilters.status" size="small" class="service-map-filter" placeholder="服务商状态">
            <el-option label="全部状态" value="all" />
            <el-option label="在线" value="online" />
            <el-option label="休息中" value="rest" />
            <el-option label="下线" value="offline" />
          </el-select>
          <el-input v-model="providerFilters.cityCode" size="small" class="service-map-filter" placeholder="城市编码" clearable />
          <el-checkbox v-model="providerFilters.onlyMissingCoordinate">只看缺坐标</el-checkbox>
          <el-button :loading="providerLoading || mapConfigLoading" @click="loadPage">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="providerErrorMessage"
        :title="providerErrorMessage"
        type="error"
        show-icon
        class="service-map-error"
        :closable="false"
      />

      <el-table
        :data="visibleProviders"
        v-loading="providerLoading"
        row-key="provider_id"
        empty-text="暂无服务商坐标数据"
        class="service-map-table"
      >
        <el-table-column label="服务商" min-width="280">
          <template #default="{ row }">
            <div class="service-map-cell">
              <div class="service-map-cell__title">
                <span>{{ row.provider_name }}</span>
                <el-tag size="small" :type="providerTypeTagType(row.provider_type)">
                  {{ providerTypeLabel(row.provider_type) }}
                </el-tag>
                <el-tag size="small" :type="providerStatusTagType(row.status)">
                  {{ providerStatusLabel(row.status) }}
                </el-tag>
              </div>
              <div class="service-map-cell__meta">城市编码：{{ row.city_code }}</div>
              <div class="service-map-cell__detail">{{ row.address || '暂未维护地址' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="坐标" min-width="250">
          <template #default="{ row }">
            <div class="service-map-cell">
              <div class="service-map-cell__title">
                <el-tag :type="hasCoordinate(row) ? 'success' : 'warning'">
                  {{ hasCoordinate(row) ? '已维护坐标' : '缺少坐标' }}
                </el-tag>
                <el-tag v-if="row.coordinate_source" type="info">
                  {{ coordinateSourceLabel(row.coordinate_source) }}
                </el-tag>
              </div>
              <div class="service-map-cell__meta">纬度：{{ formatCoordinate(row.latitude) }}</div>
              <div class="service-map-cell__meta">经度：{{ formatCoordinate(row.longitude) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="距离能力" min-width="230">
          <template #default="{ row }">
            <div class="service-map-cell">
              <div class="service-map-cell__title">
                <el-tag :type="hasCoordinate(row) && distanceCapabilityReady ? 'success' : 'info'">
                  {{ distanceReadinessLabel(row) }}
                </el-tag>
              </div>
              <div class="service-map-cell__detail">
                {{ distanceReadinessDescription(row) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updated_at) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="service-map-actions">
              <el-button size="small" type="primary" @click="openLocationDrawer(row)">维护坐标</el-button>
              <el-button size="small" :disabled="!hasCoordinate(row)" @click="reverseProviderCoordinate(row)">
                反查地址
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="locationDrawerVisible" title="服务商坐标维护" size="620px">
      <div v-if="activeProvider" class="service-map-drawer">
        <section class="service-map-drawer__section">
          <div class="service-map-drawer__heading">
            <h3>{{ activeProvider.provider_name }}</h3>
            <el-tag :type="hasCoordinate(activeProvider) ? 'success' : 'warning'">
              {{ hasCoordinate(activeProvider) ? '已维护坐标' : '缺少坐标' }}
            </el-tag>
          </div>
          <dl class="service-map-detail-list">
            <div>
              <dt>服务类型</dt>
              <dd>{{ providerTypeLabel(activeProvider.provider_type) }}</dd>
            </div>
            <div>
              <dt>城市编码</dt>
              <dd>{{ activeProvider.city_code }}</dd>
            </div>
            <div>
              <dt>当前地址</dt>
              <dd>{{ activeProvider.address || '-' }}</dd>
            </div>
            <div>
              <dt>当前坐标</dt>
              <dd>{{ formatCoordinate(activeProvider.latitude) }} / {{ formatCoordinate(activeProvider.longitude) }}</dd>
            </div>
          </dl>
        </section>

        <section class="service-map-drawer__section">
          <h3>地址转坐标辅助</h3>
          <p class="service-map-section-note">
            高德 Web 服务
          </p>
          <div class="service-map-form-grid">
            <label class="service-map-form-item">
              <span>待解析地址</span>
              <el-input v-model="geocodeForm.address" maxlength="255" show-word-limit placeholder="输入完整门店地址" />
            </label>
            <label class="service-map-form-item">
              <span>城市</span>
              <el-input v-model="geocodeForm.city" placeholder="城市编码或城市名" />
            </label>
          </div>
          <div class="service-map-actions">
            <el-button
              type="primary"
              :loading="geocodeLoading"
              :disabled="!mapServiceReady"
              @click="handleGeocode"
            >
              地址转坐标
            </el-button>
            <el-button :disabled="!latestGeocodeResult?.matched" @click="applyGeocodeResult">
              使用解析结果
            </el-button>
          </div>
          <el-alert
            v-if="mapServiceUnavailableReason"
            :title="mapServiceUnavailableReason"
            type="warning"
            show-icon
            class="service-map-inline-alert"
            :closable="false"
          />
          <article v-if="latestGeocodeResult" class="service-map-result-card">
            <div class="service-map-cell__title">
              <span>{{ latestGeocodeResult.matched ? '已匹配地址' : '未匹配到坐标' }}</span>
              <el-tag v-if="latestGeocodeResult.level" type="info">{{ latestGeocodeResult.level }}</el-tag>
            </div>
            <div class="service-map-cell__detail">
              {{ latestGeocodeResult.formatted_address || latestGeocodeResult.address }}
            </div>
            <div class="service-map-cell__meta">
              {{ latestGeocodeResult.province || '-' }} · {{ latestGeocodeResult.city || '-' }} ·
              {{ latestGeocodeResult.district || '-' }}
            </div>
            <div class="service-map-cell__meta">
              坐标：{{ formatCoordinate(latestGeocodeResult.latitude) }} /
              {{ formatCoordinate(latestGeocodeResult.longitude) }}
            </div>
          </article>
        </section>

        <section class="service-map-drawer__section">
          <h3>手动编辑坐标</h3>
          <div class="service-map-form-grid">
            <label class="service-map-form-item service-map-form-item--wide">
              <span>地址</span>
              <el-input v-model="locationForm.address" maxlength="255" show-word-limit placeholder="服务商地址，可为空" />
            </label>
            <label class="service-map-form-item">
              <span>纬度</span>
              <el-input-number
                v-model="locationForm.latitude"
                class="service-map-form-control"
                :min="-90"
                :max="90"
                :precision="6"
                controls-position="right"
              />
            </label>
            <label class="service-map-form-item">
              <span>经度</span>
              <el-input-number
                v-model="locationForm.longitude"
                class="service-map-form-control"
                :min="-180"
                :max="180"
                :precision="6"
                controls-position="right"
              />
            </label>
            <label class="service-map-form-item">
              <span>坐标来源</span>
              <el-select v-model="locationForm.coordinateSource" class="service-map-form-control">
                <el-option label="手动维护" value="manual" />
                <el-option label="高德地理编码" value="amap" />
              </el-select>
            </label>
          </div>
          <div class="service-map-actions">
            <el-button
              :loading="reverseGeocodeLoading"
              :disabled="!canReverseLocationForm"
              @click="handleReverseGeocode"
            >
              坐标反查地址
            </el-button>
            <el-button type="primary" :loading="locationSubmitting" @click="submitLocationForm">
              保存坐标
            </el-button>
          </div>
          <article v-if="latestReverseResult" class="service-map-result-card">
            <div class="service-map-cell__title">
              <span>{{ latestReverseResult.matched ? '反查到地址' : '未反查到地址' }}</span>
            </div>
            <div class="service-map-cell__detail">
              {{ latestReverseResult.formatted_address || '-' }}
            </div>
            <div class="service-map-cell__meta">
              {{ latestReverseResult.province || '-' }} · {{ latestReverseResult.city || '-' }} ·
              {{ latestReverseResult.district || '-' }}
            </div>
            <div class="service-map-actions">
              <el-button
                size="small"
                :disabled="!latestReverseResult.formatted_address"
                @click="applyReverseGeocodeResult"
              >
                使用反查地址
              </el-button>
            </div>
          </article>
        </section>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import {
  geocodeAdminAddress,
  getAdminMapConfig,
  listServiceProviders,
  reverseGeocodeAdminCoordinate,
  updateServiceProviderLocation,
  type AmapConfigStatusSnapshot,
  type AmapGeocodeResultSnapshot,
  type AmapReverseGeocodeResultSnapshot,
  type ServiceListFilter,
  type ServiceProviderCoordinateSource,
  type ServiceProviderSnapshot,
  type ServiceProviderStatus,
  type ServiceProviderType
} from '@/shared/api/serviceApi';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

type ElementTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';
type MapConfigStatus = 'loading' | 'error' | 'unknown' | 'configured' | 'missing';

interface LocationFormState {
  address: string;
  latitude: number | null;
  longitude: number | null;
  coordinateSource: ServiceProviderCoordinateSource;
}

const providerTypeOptions: Array<{ label: string; value: ServiceProviderType }> = [
  { label: '宠物医院', value: 'hospital' },
  { label: '寄养照看', value: 'boarding' },
  { label: '洗护美容', value: 'grooming' },
  { label: '训练服务', value: 'training' }
];

const providers = ref<ServiceProviderSnapshot[]>([]);
const mapConfig = ref<AmapConfigStatusSnapshot | null>(null);
const activeProvider = ref<ServiceProviderSnapshot | null>(null);
const latestGeocodeResult = ref<AmapGeocodeResultSnapshot | null>(null);
const latestReverseResult = ref<AmapReverseGeocodeResultSnapshot | null>(null);
const providerLoading = ref(false);
const mapConfigLoading = ref(false);
const geocodeLoading = ref(false);
const reverseGeocodeLoading = ref(false);
const locationSubmitting = ref(false);
const providerErrorMessage = ref('');
const mapConfigErrorMessage = ref('');
const locationDrawerVisible = ref(false);

const providerFilters = reactive<{
  providerType: ServiceListFilter<ServiceProviderType>;
  cityCode: string;
  status: ServiceListFilter<ServiceProviderStatus>;
  onlyMissingCoordinate: boolean;
}>({
  providerType: 'all',
  cityCode: '',
  status: 'all',
  onlyMissingCoordinate: false
});

const geocodeForm = reactive<{
  address: string;
  city: string;
}>({
  address: '',
  city: ''
});

const locationForm = reactive<LocationFormState>({
  address: '',
  latitude: null,
  longitude: null,
  coordinateSource: 'manual'
});

const visibleProviders = computed(() =>
  providers.value.filter((provider) => !providerFilters.onlyMissingCoordinate || !hasCoordinate(provider))
);
const locatedProviderCount = computed(() => providers.value.filter(hasCoordinate).length);
const missingCoordinateCount = computed(() => providers.value.length - locatedProviderCount.value);
const manualCoordinateCount = computed(
  () => providers.value.filter((provider) => provider.coordinate_source === 'manual').length
);
const amapCoordinateCount = computed(
  () => providers.value.filter((provider) => provider.coordinate_source === 'amap').length
);
const distanceCapabilityReady = computed(
  () => Boolean(mapServiceReady.value && mapConfig.value?.capabilities.includes('distance'))
);
const canReverseLocationForm = computed(
  () => locationForm.latitude !== null && locationForm.longitude !== null && mapServiceReady.value
);
const mapConfigStatus = computed<MapConfigStatus>(() => {
  if (mapConfigLoading.value && !mapConfig.value) {
    return 'loading';
  }
  if (mapConfigErrorMessage.value && !mapConfig.value) {
    return 'error';
  }
  if (!mapConfig.value) {
    return 'unknown';
  }
  return mapConfig.value.configured ? 'configured' : 'missing';
});
const mapConfigStatusLabel = computed(() => {
  const labelMap: Record<MapConfigStatus, string> = {
    loading: '读取中',
    error: '读取失败',
    unknown: '待确认',
    configured: '已配置',
    missing: '未配置'
  };
  return labelMap[mapConfigStatus.value];
});
const mapConfigStatusTagType = computed<ElementTagType>(() => {
  const tagTypeMap: Record<MapConfigStatus, ElementTagType> = {
    loading: 'info',
    error: 'danger',
    unknown: 'info',
    configured: 'success',
    missing: 'warning'
  };
  return tagTypeMap[mapConfigStatus.value];
});
const mapServiceReady = computed(() => mapConfig.value?.configured === true);
const mapServiceUnavailableReason = computed(() => {
  if (mapServiceReady.value) {
    return '';
  }
  if (mapConfigLoading.value && !mapConfig.value) {
    return '正在读取地图配置状态，读取完成后才能使用地理编码或坐标反查';
  }
  if (mapConfigErrorMessage.value && !mapConfig.value) {
    return '地图配置状态加载失败，请刷新配置后再使用地理编码或坐标反查';
  }
  if (mapConfig.value && !mapConfig.value.configured) {
    return '地图 Web 服务 Key 未配置，无法使用地理编码或坐标反查';
  }
  return '地图配置状态未确认，无法使用地理编码或坐标反查';
});
const summaryCards = computed(() => [
  {
    title: '坐标覆盖',
    description: '服务商中已经维护经纬度的数量。',
    value: `${locatedProviderCount.value} / ${providers.value.length}`
  },
  {
    title: '缺坐标',
    description: '无法参与附近距离计算的服务商。',
    value: `${missingCoordinateCount.value} 家`
  },
  {
    title: '高德解析',
    description: '通过服务端高德地理编码回填的坐标。',
    value: `${amapCoordinateCount.value} 家`
  },
  {
    title: '手动维护',
    description: '由后台人工录入或校正的坐标。',
    value: `${manualCoordinateCount.value} 家`
  }
]);

onMounted(() => {
  void loadPage();
});

async function loadPage() {
  await Promise.all([loadMapConfig(), loadProviders()]);
}

async function loadMapConfig() {
  mapConfigLoading.value = true;
  mapConfigErrorMessage.value = '';
  try {
    mapConfig.value = await getAdminMapConfig();
  } catch (error) {
    mapConfigErrorMessage.value = error instanceof Error ? error.message : '地图配置状态加载失败';
  } finally {
    mapConfigLoading.value = false;
  }
}

async function loadProviders() {
  providerLoading.value = true;
  providerErrorMessage.value = '';
  try {
    providers.value = await listServiceProviders({
      providerType: providerFilters.providerType,
      cityCode: providerFilters.cityCode,
      status: providerFilters.status
    });
  } catch (error) {
    providerErrorMessage.value = error instanceof Error ? error.message : '服务商坐标数据加载失败';
  } finally {
    providerLoading.value = false;
  }
}

function openLocationDrawer(provider: ServiceProviderSnapshot) {
  activeProvider.value = provider;
  latestGeocodeResult.value = null;
  latestReverseResult.value = null;
  geocodeForm.address = provider.address ?? '';
  geocodeForm.city = provider.city_code;
  Object.assign(locationForm, {
    address: provider.address ?? '',
    latitude: provider.latitude,
    longitude: provider.longitude,
    coordinateSource: provider.coordinate_source ?? 'manual'
  });
  locationDrawerVisible.value = true;
}

async function handleGeocode() {
  if (!mapServiceReady.value) {
    ElMessage.warning(mapServiceUnavailableReason.value || '请先确认地图服务已配置');
    return;
  }
  if (!geocodeForm.address.trim()) {
    ElMessage.warning('请输入需要解析的地址');
    return;
  }
  geocodeLoading.value = true;
  try {
    latestGeocodeResult.value = await geocodeAdminAddress({
      address: geocodeForm.address.trim(),
      city: normalizeNullableText(geocodeForm.city) ?? undefined
    });
    if (latestGeocodeResult.value.matched) {
      ElMessage.success('地址解析完成，可检查后回填坐标');
    } else {
      ElMessage.warning('未匹配到可用坐标，请补充更完整地址');
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '地址转坐标失败');
  } finally {
    geocodeLoading.value = false;
  }
}

function applyGeocodeResult() {
  const result = latestGeocodeResult.value;
  if (!result?.matched || result.latitude === null || result.longitude === null) {
    ElMessage.warning('当前没有可回填的解析坐标');
    return;
  }
  locationForm.address = result.formatted_address || result.address || locationForm.address;
  locationForm.latitude = result.latitude;
  locationForm.longitude = result.longitude;
  locationForm.coordinateSource = 'amap';
}

async function handleReverseGeocode() {
  if (locationForm.latitude === null || locationForm.longitude === null) {
    ElMessage.warning('请先填写合法经纬度');
    return;
  }
  if (!mapServiceReady.value) {
    ElMessage.warning(mapServiceUnavailableReason.value || '请先确认地图服务已配置');
    return;
  }
  reverseGeocodeLoading.value = true;
  try {
    latestReverseResult.value = await reverseGeocodeAdminCoordinate({
      latitude: locationForm.latitude,
      longitude: locationForm.longitude
    });
    if (latestReverseResult.value.matched) {
      ElMessage.success('坐标反查完成');
    } else {
      ElMessage.warning('未反查到地址，请检查坐标');
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '坐标反查失败');
  } finally {
    reverseGeocodeLoading.value = false;
  }
}

async function reverseProviderCoordinate(provider: ServiceProviderSnapshot) {
  if (provider.latitude === null || provider.longitude === null) {
    return;
  }
  openLocationDrawer(provider);
  await handleReverseGeocode();
}

function applyReverseGeocodeResult() {
  if (!latestReverseResult.value?.formatted_address) {
    ElMessage.warning('当前没有可回填的反查地址');
    return;
  }
  locationForm.address = latestReverseResult.value.formatted_address;
}

async function submitLocationForm() {
  if (!activeProvider.value || !validateLocationForm()) {
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认保存「${activeProvider.value.provider_name}」的服务商坐标？保存后会影响用户端附近排序和距离展示。`,
      '保存服务商坐标',
      {
        confirmButtonText: '保存坐标',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    locationSubmitting.value = true;
    const updatedProvider = await updateServiceProviderLocation(activeProvider.value.provider_id, {
      address: normalizeNullableText(locationForm.address),
      latitude: Number(locationForm.latitude),
      longitude: Number(locationForm.longitude),
      coordinate_source: locationForm.coordinateSource
    });
    upsertProvider(updatedProvider);
    activeProvider.value = updatedProvider;
    locationDrawerVisible.value = false;
    ElMessage.success('服务商坐标已保存');
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '服务商坐标保存失败');
    }
  } finally {
    locationSubmitting.value = false;
  }
}

function validateLocationForm() {
  if (locationForm.latitude === null || locationForm.longitude === null) {
    ElMessage.warning('纬度和经度不能为空');
    return false;
  }
  if (locationForm.latitude < -90 || locationForm.latitude > 90) {
    ElMessage.warning('纬度必须在 -90 到 90 之间');
    return false;
  }
  if (locationForm.longitude < -180 || locationForm.longitude > 180) {
    ElMessage.warning('经度必须在 -180 到 180 之间');
    return false;
  }
  return true;
}

function upsertProvider(provider: ServiceProviderSnapshot) {
  const providerIndex = providers.value.findIndex((item) => item.provider_id === provider.provider_id);
  if (providerIndex >= 0) {
    providers.value.splice(providerIndex, 1, provider);
    return;
  }
  providers.value.unshift(provider);
}

function hasCoordinate(provider: ServiceProviderSnapshot) {
  return provider.latitude !== null && provider.longitude !== null;
}

function distanceReadinessLabel(provider: ServiceProviderSnapshot) {
  if (!hasCoordinate(provider)) {
    return '缺少坐标';
  }
  if (!distanceCapabilityReady.value) {
    return '等待配置';
  }
  return '可参与距离计算';
}

function distanceReadinessDescription(provider: ServiceProviderSnapshot) {
  if (!hasCoordinate(provider)) {
    return '需要先维护经纬度，用户端附近排序才有基础数据。';
  }
  if (!distanceCapabilityReady.value) {
    return '坐标已维护，但地图配置或 distance 能力仍需确认。';
  }
  return '坐标和 distance 能力均已具备；真实排序由服务端用户端接口计算。';
}

function capabilityLabel(capability: string) {
  const labelMap: Record<string, string> = {
    geocode: '地址转坐标',
    reverse_geocode: '坐标反查地址',
    distance: '距离计算'
  };
  return labelMap[capability] ?? capability;
}

function coordinateSourceLabel(source: ServiceProviderCoordinateSource) {
  const labelMap: Record<ServiceProviderCoordinateSource, string> = {
    manual: '手动维护',
    amap: '高德解析'
  };
  return labelMap[source];
}

function providerTypeLabel(providerType: ServiceProviderType) {
  const labelMap: Record<ServiceProviderType, string> = {
    hospital: '宠物医院',
    boarding: '寄养照看',
    grooming: '洗护美容',
    training: '训练服务'
  };
  return labelMap[providerType];
}

function providerStatusLabel(status: ServiceProviderStatus) {
  const labelMap: Record<ServiceProviderStatus, string> = {
    online: '在线',
    rest: '休息中',
    offline: '下线'
  };
  return labelMap[status];
}

function providerTypeTagType(providerType: ServiceProviderType): ElementTagType {
  if (providerType === 'hospital') {
    return 'danger';
  }
  if (providerType === 'grooming') {
    return 'success';
  }
  if (providerType === 'training') {
    return 'warning';
  }
  return 'info';
}

function providerStatusTagType(status: ServiceProviderStatus): ElementTagType {
  if (status === 'online') {
    return 'success';
  }
  if (status === 'rest') {
    return 'warning';
  }
  return 'info';
}

function normalizeNullableText(value: string) {
  const normalizedValue = value.trim();
  return normalizedValue.length > 0 ? normalizedValue : null;
}

function formatCoordinate(value: number | null) {
  return value === null ? '-' : value.toFixed(6);
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.service-map-summary,
.service-map-section,
.service-map-boundary,
.service-map-panels {
  margin-top: 24px;
}

.service-map-panel-heading,
.service-map-toolbar,
.service-map-drawer__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.service-map-toolbar__actions,
.service-map-actions,
.service-map-tags {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.service-map-filter {
  width: 138px;
}

.service-map-error {
  margin-bottom: 16px;
}

.service-map-empty-inline {
  padding: 16px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.7;
}

.service-map-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.service-map-config-list,
.service-map-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.service-map-config-list div,
.service-map-detail-list div {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 12px;
}

.service-map-config-list dt,
.service-map-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.service-map-config-list dd,
.service-map-detail-list dd {
  margin: 0;
  color: var(--pet-admin-title);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}

.service-map-readiness {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.service-map-readiness div {
  padding: 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 18px;
  background: var(--pet-admin-surface-soft);
}

.service-map-readiness strong {
  display: block;
  color: var(--pet-admin-title);
  font-size: 22px;
}

.service-map-readiness span,
.service-map-distance-note,
.service-map-section-note {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.7;
}

.service-map-distance-note {
  margin-top: 16px;
}

.service-map-distance-note p {
  margin: 0 0 6px;
}

.service-map-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.service-map-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.service-map-cell__meta,
.service-map-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.service-map-drawer {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.service-map-drawer__section {
  padding: 18px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 20px;
  background: var(--pet-admin-surface-soft);
}

.service-map-drawer__section h3,
.service-map-drawer__heading h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 16px;
}

.service-map-drawer__heading h3 {
  margin: 0;
}

.service-map-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.service-map-form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--pet-admin-title);
  font-size: 13px;
  font-weight: 700;
}

.service-map-form-item--wide {
  grid-column: 1 / -1;
}

.service-map-form-control {
  width: 100%;
}

.service-map-inline-alert,
.service-map-result-card {
  margin-top: 14px;
}

.service-map-result-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: #fffdfb;
}

:deep(.el-table) {
  --el-table-border-color: var(--pet-admin-line);
  --el-table-header-bg-color: #fff8f2;
  --el-table-row-hover-bg-color: #fff6ee;
  --el-table-text-color: var(--pet-admin-body);
  --el-table-header-text-color: var(--pet-admin-title);
  border-radius: 20px;
}

:deep(.el-button),
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-input-number),
:deep(.el-textarea__inner) {
  border-radius: 14px;
}

@media (max-width: 1080px) {
  .service-map-panel-heading,
  .service-map-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .service-map-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .service-map-filter,
  .service-map-form-control {
    width: 100%;
  }

  .service-map-toolbar__actions,
  .service-map-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .service-map-form-grid,
  .service-map-readiness {
    grid-template-columns: 1fr;
  }
}
</style>
