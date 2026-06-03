<template>
  <section class="page-section service-page">
    <div class="pet-admin-hero service-hero">
      <p class="page-section__eyebrow">服务网络</p>
      <h1 class="page-section__title">服务商管理</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">开通城市 {{ openedCityCount }} 个</span>
        <span class="pet-admin-chip">在线服务商 {{ onlineProviderCount }} 家</span>
        <span class="pet-admin-chip">可预约时段 {{ openSlotCount }} 个</span>
        <span class="pet-admin-chip">待确认预约 {{ pendingAppointmentCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid service-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel service-section">
      <div class="service-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">城市开通配置</h2>
        </div>
        <div class="service-toolbar__actions">
          <el-select v-model="cityConfigFilters.opened" size="small" class="service-filter" placeholder="开通状态">
            <el-option label="全部城市" value="all" />
            <el-option label="已开通" value="opened" />
            <el-option label="未开通" value="closed" />
          </el-select>
          <el-input v-model="cityConfigFilters.cityCode" size="small" class="service-filter" placeholder="城市编码" clearable />
          <el-button :loading="cityConfigLoading" @click="loadCityConfigs">刷新</el-button>
          <el-button type="primary" @click="openCreateCityConfigDialog">新增城市</el-button>
        </div>
      </div>

      <el-alert
        v-if="cityConfigErrorMessage"
        :title="cityConfigErrorMessage"
        type="error"
        show-icon
        class="service-error"
        :closable="false"
      />

      <el-table
        :data="cityConfigs"
        v-loading="cityConfigLoading"
        row-key="city_code"
        empty-text="暂无城市配置"
        class="service-table"
      >
        <el-table-column label="城市" min-width="220">
          <template #default="{ row }">
            <div class="service-provider-cell">
              <div class="service-provider-cell__title">{{ row.city_name }}</div>
              <div class="service-provider-cell__meta">城市编码：{{ row.city_code }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="开通状态" width="130">
          <template #default="{ row }">
            <el-tag :type="cityOpenedTagType(row.opened)">
              {{ cityOpenedLabel(row.opened) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="未开通原因" min-width="280">
          <template #default="{ row }">
            <span class="service-remark">{{ row.opened ? '已开通城市不展示未开通原因' : row.unavailable_reason || '暂无原因' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="排序" width="90" prop="sort_order" />

        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ row.updated_at ? formatDateTime(row.updated_at) : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="service-actions">
              <el-button size="small" @click="openEditCityConfigDialog(row)">编辑</el-button>
              <el-button
                size="small"
                :type="row.opened ? 'warning' : 'success'"
                :loading="processingCityCode === row.city_code"
                @click="toggleCityConfig(row)"
              >
                {{ row.opened ? '关闭城市' : '开通城市' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article class="pet-admin-panel service-section">
      <div class="service-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">服务商与资源维护</h2>
        </div>
        <div class="service-toolbar__actions">
          <el-select v-model="providerFilters.providerType" size="small" class="service-filter" placeholder="服务类型">
            <el-option label="全部服务" value="all" />
            <el-option
              v-for="option in providerTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="providerFilters.status" size="small" class="service-filter" placeholder="服务商状态">
            <el-option label="全部状态" value="all" />
            <el-option
              v-for="option in providerStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input v-model="providerFilters.cityCode" size="small" class="service-filter" placeholder="城市编码" clearable />
          <el-button :loading="providerLoading" @click="loadProviders">刷新</el-button>
          <el-button type="primary" @click="openCreateProviderDialog">新增服务商</el-button>
        </div>
      </div>

      <el-alert
        v-if="providerErrorMessage"
        :title="providerErrorMessage"
        type="error"
        show-icon
        class="service-error"
        :closable="false"
      />

      <el-table
        :data="providers"
        v-loading="providerLoading"
        row-key="provider_id"
        empty-text="暂无服务商"
        class="service-table"
      >
        <el-table-column type="expand" width="48">
          <template #default="{ row }">
            <div class="service-provider-detail">
              <section class="service-provider-detail__panel">
                <div class="service-provider-detail__header">
                  <div>
                    <h3>服务项目</h3>
                  </div>
                  <el-button size="small" type="primary" @click="openCreateServiceItemDialog(row)">
                    新增项目
                  </el-button>
                </div>
                <div v-if="row.service_items.length === 0" class="service-empty-inline">
                  暂未配置服务项目
                </div>
                <div v-else class="service-resource-list">
                  <div
                    v-for="item in row.service_items"
                    :key="item.service_item_id"
                    class="service-resource-card"
                  >
                    <div class="service-resource-card__main">
                      <div class="service-resource-card__title">
                        <span>{{ item.service_name }}</span>
                        <el-tag size="small" :type="serviceItemStatusTagType(item.status)">
                          {{ serviceItemStatusLabel(item.status) }}
                        </el-tag>
                      </div>
                      <div class="service-provider-cell__meta">
                        {{ item.service_code }} · {{ priceRangeLabel(item) }}
                      </div>
                      <div class="service-provider-cell__meta">
                        {{ item.service_desc || '暂未维护服务内容' }}
                      </div>
                    </div>
                    <el-button size="small" @click="openCreateServiceItemDialog(row, item)">
                      编辑项目
                    </el-button>
                  </div>
                </div>
              </section>

              <section class="service-provider-detail__panel">
                <div class="service-provider-detail__header">
                  <div>
                    <h3>预约时段</h3>
                  </div>
                  <el-button size="small" type="primary" @click="openCreateSlotDialog(row)">
                    新增时段
                  </el-button>
                </div>
                <div v-if="row.available_slots.length === 0" class="service-empty-inline">
                  暂未配置预约时段
                </div>
                <div v-else class="service-resource-list">
                  <div
                    v-for="slot in row.available_slots"
                    :key="slot.slot_id"
                    class="service-resource-card"
                  >
                    <div class="service-resource-card__main">
                      <div class="service-resource-card__title">
                        <span>{{ slot.slot_date }} {{ formatSlotTime(slot) }}</span>
                        <el-tag size="small" :type="slotStatusTagType(slot.status)">
                          {{ slotStatusLabel(slot.status) }}
                        </el-tag>
                      </div>
                      <div class="service-provider-cell__meta">
                        {{ providerTypeLabel(slot.appointment_type) }} ·
                        已约 {{ slot.booked_count }} / 总名额 {{ slot.quota }} ·
                        剩余 {{ slot.available_quota }}
                      </div>
                      <div class="service-provider-cell__meta">
                        {{ slot.bookable ? '用户端当前可约' : '用户端当前不可约' }}
                      </div>
                    </div>
                    <el-button size="small" @click="openCreateSlotDialog(row, slot)">
                      编辑时段
                    </el-button>
                  </div>
                </div>
              </section>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="服务商" min-width="280">
          <template #default="{ row }">
            <div class="service-provider-cell">
              <div class="service-provider-cell__title">
                <span>{{ row.provider_name }}</span>
                <el-tag size="small" :type="providerTypeTagType(row.provider_type)">
                  {{ providerTypeLabel(row.provider_type) }}
                </el-tag>
              </div>
              <div class="service-provider-cell__meta">{{ row.address || '暂未维护地址' }}</div>
              <div class="service-provider-cell__meta">
                {{ row.contact_phone || '暂无电话' }} · {{ row.business_hours || '暂无营业时间' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <div class="service-status-stack">
              <el-tag :type="providerStatusTagType(row.status)">
                {{ providerStatusLabel(row.status) }}
              </el-tag>
              <span>{{ row.bookable ? '用户端可约' : '暂停预约' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="服务项目" min-width="260">
          <template #default="{ row }">
            <div class="service-inline-list">
              <span v-if="row.service_items.length === 0">暂未配置项目</span>
              <el-tag
                v-for="item in row.service_items.slice(0, 3)"
                :key="item.service_item_id"
                size="small"
                :type="item.status === 'active' ? 'success' : 'info'"
              >
                {{ item.service_name }}
              </el-tag>
              <span v-if="row.service_items.length > 3">+{{ row.service_items.length - 3 }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="预约时段" min-width="220">
          <template #default="{ row }">
            <div class="service-slot-overview">
              <strong>{{ providerOpenSlotCount(row) }} 个可约</strong>
              <span>近 60 天合计 {{ row.available_slots.length }} 个时段</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="口碑" width="130">
          <template #default="{ row }">
            <div class="service-rating">
              <strong>{{ row.rating_avg ?? '-' }}</strong>
              <span>{{ row.review_count }} 条评价</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="service-actions">
              <el-button size="small" @click="openEditProviderDialog(row)">编辑资料</el-button>
              <el-button size="small" @click="openCreateServiceItemDialog(row)">新增项目</el-button>
              <el-button size="small" @click="openCreateSlotDialog(row)">新增时段</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article class="pet-admin-panel service-section">
      <div class="service-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">预约记录处理</h2>
        </div>
        <div class="service-toolbar__actions">
          <el-select v-model="appointmentFilters.status" size="small" class="service-filter" placeholder="预约状态">
            <el-option label="全部预约" value="all" />
            <el-option
              v-for="option in appointmentStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="appointmentFilters.providerType" size="small" class="service-filter" placeholder="服务类型">
            <el-option label="全部服务" value="all" />
            <el-option
              v-for="option in providerTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input
            v-model="appointmentFilters.cityCode"
            size="small"
            class="service-filter"
            placeholder="城市编码"
            clearable
          />
          <el-button :loading="appointmentLoading" @click="loadAppointments">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="appointmentErrorMessage"
        :title="appointmentErrorMessage"
        type="error"
        show-icon
        class="service-error"
        :closable="false"
      />

      <el-table
        :data="appointments"
        v-loading="appointmentLoading"
        row-key="appointment_id"
        empty-text="暂无预约记录"
        class="service-table"
      >
        <el-table-column label="预约信息" min-width="280">
          <template #default="{ row }">
            <div class="service-provider-cell">
              <div class="service-provider-cell__title">
                <span>{{ row.provider_name }}</span>
                <el-tag size="small" :type="providerTypeTagType(row.appointment_type)">
                  {{ providerTypeLabel(row.appointment_type) }}
                </el-tag>
              </div>
              <div class="service-provider-cell__meta">
                {{ row.appointment_date }} · {{ row.appointment_slot }}
              </div>
              <div v-if="row.demand_desc" class="service-provider-cell__meta">{{ row.demand_desc }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="宠物与联系人" min-width="220">
          <template #default="{ row }">
            <div class="service-provider-cell">
              <div class="service-provider-cell__title">{{ row.pet_name }}</div>
              <div class="service-provider-cell__meta">{{ row.contact_name }} · {{ row.contact_mobile }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="appointmentStatusTagType(row.status)">
              {{ appointmentStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="220">
          <template #default="{ row }">
            <span class="service-remark">{{ row.remark || '暂无后台备注' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="service-actions">
              <el-button
                v-if="row.status === 'pending_confirm'"
                size="small"
                type="primary"
                @click="openAppointmentStatusDialog(row, 'confirmed')"
              >
                确认
              </el-button>
              <el-button
                v-if="row.status === 'confirmed'"
                size="small"
                type="success"
                @click="openAppointmentStatusDialog(row, 'completed')"
              >
                完成
              </el-button>
              <el-button
                v-if="row.status === 'pending_confirm' || row.status === 'confirmed'"
                size="small"
                type="warning"
                @click="openAppointmentStatusDialog(row, 'canceled')"
              >
                取消
              </el-button>
              <span v-if="row.status === 'completed' || row.status === 'canceled'" class="service-action-text">
                已结束
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article class="pet-admin-panel service-section">
      <div class="service-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">服务评价治理</h2>
        </div>
        <div class="service-toolbar__actions">
          <el-select v-model="reviewFilters.status" size="small" class="service-filter" placeholder="评价状态">
            <el-option label="全部评价" value="all" />
            <el-option
              v-for="option in reviewStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="reviewFilters.providerType" size="small" class="service-filter" placeholder="服务类型">
            <el-option label="全部服务" value="all" />
            <el-option
              v-for="option in providerTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input v-model="reviewFilters.cityCode" size="small" class="service-filter" placeholder="城市编码" clearable />
          <el-button :loading="reviewLoading" @click="loadReviews">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="reviewErrorMessage"
        :title="reviewErrorMessage"
        type="error"
        show-icon
        class="service-error"
        :closable="false"
      />

      <el-table
        :data="reviews"
        v-loading="reviewLoading"
        row-key="review_id"
        empty-text="暂无服务评价"
        class="service-table"
      >
        <el-table-column label="评价内容" min-width="340">
          <template #default="{ row }">
            <div class="service-provider-cell">
              <div class="service-provider-cell__title">
                <span>{{ row.reviewer_nickname }}</span>
                <el-tag size="small" type="warning">{{ row.rating }} 分</el-tag>
              </div>
              <div class="service-provider-cell__meta">
                {{ row.pet_name || '未知宠物' }} · {{ formatDateTime(row.created_at) }}
              </div>
              <div class="service-review-content">{{ row.content || '用户未填写文字评价' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="服务商" min-width="220">
          <template #default="{ row }">
            <div class="service-provider-cell">
              <div class="service-provider-cell__title">{{ row.provider_name }}</div>
              <div class="service-provider-cell__meta">{{ providerTypeLabel(row.provider_type) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="reviewStatusTagType(row.status)">
              {{ reviewStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'visible'"
              size="small"
              type="warning"
              :loading="processingReviewId === row.review_id"
              @click="handleReviewStatus(row, 'hidden')"
            >
              隐藏
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              :loading="processingReviewId === row.review_id"
              @click="handleReviewStatus(row, 'visible')"
            >
              恢复展示
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article class="pet-admin-panel service-section">
      <div class="service-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">服务中心操作审计</h2>
        </div>
        <div class="service-toolbar__actions">
          <el-select v-model="auditLogFilters.targetType" size="small" class="service-filter" placeholder="目标类型">
            <el-option label="全部目标" value="all" />
            <el-option
              v-for="option in auditTargetTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input v-model="auditLogFilters.operatorId" size="small" class="service-filter" placeholder="操作者" clearable />
          <el-input v-model="auditLogFilters.action" size="small" class="service-filter" placeholder="动作编码" clearable />
          <el-button :loading="auditLogLoading" @click="loadAuditLogs">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="auditLogErrorMessage"
        :title="auditLogErrorMessage"
        type="error"
        show-icon
        class="service-error"
        :closable="false"
      />

      <el-table
        :data="auditLogs"
        v-loading="auditLogLoading"
        row-key="audit_log_id"
        empty-text="暂无服务中心审计日志"
        class="service-table"
      >
        <el-table-column label="操作" min-width="260">
          <template #default="{ row }">
            <div class="service-provider-cell">
              <div class="service-provider-cell__title">
                <span>{{ auditActionLabel(row.action) }}</span>
                <el-tag size="small" type="info">{{ auditTargetTypeLabel(row.target_type) }}</el-tag>
              </div>
              <div class="service-provider-cell__meta">目标 ID：{{ row.target_id }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作者" min-width="190">
          <template #default="{ row }">
            <div class="service-provider-cell">
              <div class="service-provider-cell__title">{{ row.operator_id }}</div>
              <div class="service-provider-cell__meta">{{ row.ip_address || '未知 IP' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="详情" min-width="320">
          <template #default="{ row }">
            <pre class="service-audit-detail">{{ formatAuditDetail(row.detail_json) }}</pre>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-dialog v-model="cityConfigDialogVisible" :title="cityConfigDialogTitle" width="560px">
      <el-form label-position="top" class="service-form">
        <div class="service-form-grid">
          <el-form-item label="城市编码">
            <el-input
              v-model="cityConfigForm.cityCode"
              :disabled="Boolean(editingCityCode)"
              placeholder="例如：310000"
            />
          </el-form-item>
          <el-form-item label="城市名称">
            <el-input v-model="cityConfigForm.cityName" placeholder="例如：上海" />
          </el-form-item>
          <el-form-item label="开通状态">
            <el-switch
              v-model="cityConfigForm.opened"
              active-text="已开通"
              inactive-text="未开通"
              inline-prompt
            />
          </el-form-item>
          <el-form-item label="展示排序">
            <el-input-number
              v-model="cityConfigForm.sortOrder"
              class="service-form-control"
              :min="0"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item label="未开通原因">
          <el-input
            v-model="cityConfigForm.unavailableReason"
            type="textarea"
            :rows="3"
            :disabled="cityConfigForm.opened"
            placeholder="未开通原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cityConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="cityConfigSubmitting" @click="submitCityConfigForm">保存城市配置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="providerDialogVisible" :title="providerDialogTitle" width="720px">
      <el-form label-position="top" class="service-form">
        <div class="service-form-grid">
          <el-form-item label="服务类型">
            <el-select v-model="providerForm.providerType" class="service-form-control">
              <el-option
                v-for="option in providerTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="服务商状态">
            <el-select v-model="providerForm.status" class="service-form-control">
              <el-option
                v-for="option in providerStatusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="服务商名称">
            <el-input v-model="providerForm.providerName" placeholder="例如：安心宠物医院" />
          </el-form-item>
          <el-form-item label="城市编码">
            <el-input v-model="providerForm.cityCode" placeholder="例如：310000" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="providerForm.contactPhone" placeholder="门店联系电话" />
          </el-form-item>
          <el-form-item label="营业时间">
            <el-input v-model="providerForm.businessHours" placeholder="例如：09:00-20:00" />
          </el-form-item>
          <el-form-item label="平均评分">
            <el-input-number
              v-model="providerForm.ratingAvg"
              class="service-form-control"
              :min="0"
              :max="5"
              :precision="1"
              :step="0.1"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="评价数量">
            <el-input-number
              v-model="providerForm.reviewCount"
              class="service-form-control"
              :min="0"
              :step="1"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item label="详细地址">
          <el-input v-model="providerForm.address" placeholder="服务商线下地址" />
        </el-form-item>
        <div class="service-form-grid">
          <el-form-item label="纬度">
            <el-input-number
              v-model="providerForm.latitude"
              class="service-form-control"
              :precision="6"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="经度">
            <el-input-number
              v-model="providerForm.longitude"
              class="service-form-control"
              :precision="6"
              controls-position="right"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="providerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="providerSubmitting" @click="submitProviderForm">保存服务商</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="serviceItemDialogVisible" :title="serviceItemDialogTitle" width="560px">
      <div v-if="activeProvider" class="service-dialog-context">
        当前服务商：{{ activeProvider.provider_name }}
      </div>
      <el-form label-position="top" class="service-form">
        <el-form-item label="服务编码">
          <el-input v-model="serviceItemForm.serviceCode" placeholder="例如：hospital_basic" />
        </el-form-item>
        <el-form-item label="服务名称">
          <el-input v-model="serviceItemForm.serviceName" placeholder="例如：基础问诊" />
        </el-form-item>
        <el-form-item label="服务内容">
          <el-input v-model="serviceItemForm.serviceDesc" type="textarea" :rows="3" placeholder="服务内容" />
        </el-form-item>
        <div class="service-form-grid">
          <el-form-item label="最低价格">
            <el-input-number
              v-model="serviceItemForm.priceMin"
              class="service-form-control"
              :min="0"
              :precision="2"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="最高价格">
            <el-input-number
              v-model="serviceItemForm.priceMax"
              class="service-form-control"
              :min="0"
              :precision="2"
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item label="项目状态">
          <el-select v-model="serviceItemForm.status" class="service-form-control">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="serviceItemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="serviceItemSubmitting" @click="submitServiceItemForm">保存项目</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scheduleSlotDialogVisible" :title="scheduleSlotDialogTitle" width="560px">
      <div v-if="activeProvider" class="service-dialog-context">
        当前服务商：{{ activeProvider.provider_name }}
      </div>
      <el-form label-position="top" class="service-form">
        <el-form-item label="预约类型">
          <el-select v-model="scheduleSlotForm.appointmentType" class="service-form-control">
            <el-option
              v-for="option in providerTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker
            v-model="scheduleSlotForm.slotDate"
            class="service-form-control"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
        <div class="service-form-grid">
          <el-form-item label="开始时间">
            <el-time-picker
              v-model="scheduleSlotForm.startTime"
              class="service-form-control"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="开始时间"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-time-picker
              v-model="scheduleSlotForm.endTime"
              class="service-form-control"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="结束时间"
            />
          </el-form-item>
        </div>
        <div class="service-form-grid">
          <el-form-item label="可预约名额">
            <el-input-number
              v-model="scheduleSlotForm.quota"
              class="service-form-control"
              :min="0"
              :step="1"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="时段状态">
            <el-select v-model="scheduleSlotForm.status" class="service-form-control">
              <el-option label="开放" value="open" />
              <el-option label="关闭" value="closed" />
              <el-option label="已满" value="full" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="scheduleSlotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scheduleSlotSubmitting" @click="submitScheduleSlotForm">保存时段</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appointmentStatusDialogVisible" :title="appointmentStatusDialogTitle" width="520px">
      <div v-if="activeAppointment" class="service-dialog-context">
        {{ activeAppointment.pet_name }} · {{ activeAppointment.provider_name }} ·
        {{ activeAppointment.appointment_date }} {{ activeAppointment.appointment_slot }}
      </div>
      <el-form label-position="top" class="service-form">
        <el-form-item label="目标状态">
          <el-radio-group v-model="appointmentStatusForm.status">
            <el-radio-button label="confirmed">确认预约</el-radio-button>
            <el-radio-button label="completed">完成服务</el-radio-button>
            <el-radio-button label="canceled">取消预约</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="后台备注">
          <el-input
            v-model="appointmentStatusForm.remark"
            type="textarea"
            :rows="4"
            placeholder="状态调整原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appointmentStatusDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="appointmentStatusSubmitting"
          @click="submitAppointmentStatusForm"
        >
          更新预约
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import {
  createProviderScheduleSlot,
  createProviderServiceItem,
  createServiceProvider,
  listProviderReviews,
  listServiceAuditLogs,
  listServiceCityConfigs,
  listServiceAppointments,
  listServiceProviders,
  updateProviderReviewStatus,
  updateProviderScheduleSlot,
  updateProviderServiceItem,
  updateServiceAppointmentStatus,
  updateServiceProvider,
  upsertServiceCityConfig,
  type ServiceAuditLogSnapshot,
  type ServiceAuditTargetType,
  type ServiceAuditTargetTypeFilter,
  type ServiceCityConfigSnapshot,
  type ServiceCityOpenedFilter,
  type ProviderScheduleSlotSnapshot,
  type ProviderScheduleSlotStatus,
  type ProviderReviewSnapshot,
  type ProviderReviewStatus,
  type ProviderServiceItemSnapshot,
  type ProviderServiceItemStatus,
  type ServiceAppointmentSnapshot,
  type ServiceAppointmentStatus,
  type ServiceListFilter,
  type ServiceProviderSnapshot,
  type ServiceProviderStatus,
  type ServiceProviderType
} from '@/shared/api/serviceApi';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

interface ProviderFormState {
  providerType: ServiceProviderType;
  providerName: string;
  cityCode: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  contactPhone: string;
  businessHours: string;
  ratingAvg: number | null;
  reviewCount: number;
  status: ServiceProviderStatus;
}

interface CityConfigFormState {
  cityCode: string;
  cityName: string;
  opened: boolean;
  unavailableReason: string;
  sortOrder: number;
}

interface ServiceItemFormState {
  serviceCode: string;
  serviceName: string;
  serviceDesc: string;
  priceMin: number | null;
  priceMax: number | null;
  status: ProviderServiceItemStatus;
}

interface ScheduleSlotFormState {
  appointmentType: ServiceProviderType;
  slotDate: string;
  startTime: string;
  endTime: string;
  quota: number;
  status: ProviderScheduleSlotStatus;
}

const providerTypeOptions: Array<{ label: string; value: ServiceProviderType }> = [
  { label: '宠物医院', value: 'hospital' },
  { label: '寄养照看', value: 'boarding' },
  { label: '洗护美容', value: 'grooming' },
  { label: '训练服务', value: 'training' }
];

const providerStatusOptions: Array<{ label: string; value: ServiceProviderStatus }> = [
  { label: '在线', value: 'online' },
  { label: '休息中', value: 'rest' },
  { label: '下线', value: 'offline' }
];

const appointmentStatusOptions: Array<{ label: string; value: ServiceAppointmentStatus }> = [
  { label: '待确认', value: 'pending_confirm' },
  { label: '已确认', value: 'confirmed' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'canceled' }
];

const reviewStatusOptions: Array<{ label: string; value: ProviderReviewStatus }> = [
  { label: '展示中', value: 'visible' },
  { label: '已隐藏', value: 'hidden' }
];

const auditTargetTypeOptions: Array<{ label: string; value: ServiceAuditTargetType }> = [
  { label: '城市配置', value: 'service_city' },
  { label: '服务商', value: 'service_provider' },
  { label: '服务项目', value: 'provider_service_item' },
  { label: '预约时段', value: 'provider_schedule_slot' },
  { label: '预约记录', value: 'service_appointment' },
  { label: '服务评价', value: 'provider_review' }
];

const cityConfigs = ref<ServiceCityConfigSnapshot[]>([]);
const providers = ref<ServiceProviderSnapshot[]>([]);
const appointments = ref<ServiceAppointmentSnapshot[]>([]);
const reviews = ref<ProviderReviewSnapshot[]>([]);
const auditLogs = ref<ServiceAuditLogSnapshot[]>([]);
const cityConfigLoading = ref(false);
const providerLoading = ref(false);
const appointmentLoading = ref(false);
const reviewLoading = ref(false);
const auditLogLoading = ref(false);
const cityConfigSubmitting = ref(false);
const providerSubmitting = ref(false);
const serviceItemSubmitting = ref(false);
const scheduleSlotSubmitting = ref(false);
const appointmentStatusSubmitting = ref(false);
const processingCityCode = ref<string | null>(null);
const processingReviewId = ref<string | null>(null);
const cityConfigErrorMessage = ref('');
const providerErrorMessage = ref('');
const appointmentErrorMessage = ref('');
const reviewErrorMessage = ref('');
const auditLogErrorMessage = ref('');

const cityConfigFilters = reactive<{
  opened: ServiceCityOpenedFilter;
  cityCode: string;
}>({
  opened: 'all',
  cityCode: ''
});

const providerFilters = reactive<{
  providerType: ServiceListFilter<ServiceProviderType>;
  cityCode: string;
  status: ServiceListFilter<ServiceProviderStatus>;
}>({
  providerType: 'all',
  cityCode: '',
  status: 'all'
});

const appointmentFilters = reactive<{
  status: ServiceListFilter<ServiceAppointmentStatus>;
  providerType: ServiceListFilter<ServiceProviderType>;
  cityCode: string;
}>({
  status: 'all',
  providerType: 'all',
  cityCode: ''
});

const reviewFilters = reactive<{
  status: ServiceListFilter<ProviderReviewStatus>;
  providerType: ServiceListFilter<ServiceProviderType>;
  cityCode: string;
}>({
  status: 'all',
  providerType: 'all',
  cityCode: ''
});

const auditLogFilters = reactive<{
  targetType: ServiceAuditTargetTypeFilter;
  operatorId: string;
  action: string;
}>({
  targetType: 'all',
  operatorId: '',
  action: ''
});

const cityConfigDialogVisible = ref(false);
const providerDialogVisible = ref(false);
const serviceItemDialogVisible = ref(false);
const scheduleSlotDialogVisible = ref(false);
const appointmentStatusDialogVisible = ref(false);
const editingCityCode = ref<string | null>(null);
const editingProviderId = ref<string | null>(null);
const editingServiceItemId = ref<string | null>(null);
const editingScheduleSlotId = ref<string | null>(null);
const activeProvider = ref<ServiceProviderSnapshot | null>(null);
const activeScheduleSlot = ref<ProviderScheduleSlotSnapshot | null>(null);
const activeAppointment = ref<ServiceAppointmentSnapshot | null>(null);

const cityConfigForm = reactive<CityConfigFormState>(createDefaultCityConfigForm());
const providerForm = reactive<ProviderFormState>(createDefaultProviderForm());
const serviceItemForm = reactive<ServiceItemFormState>(createDefaultServiceItemForm());
const scheduleSlotForm = reactive<ScheduleSlotFormState>(createDefaultScheduleSlotForm());
const appointmentStatusForm = reactive<{
  status: ServiceAppointmentStatus;
  remark: string;
}>({
  status: 'confirmed',
  remark: ''
});

const openedCityCount = computed(() => cityConfigs.value.filter((cityConfig) => cityConfig.opened).length);
const onlineProviderCount = computed(() => providers.value.filter((provider) => provider.status === 'online').length);
const openSlotCount = computed(() =>
  providers.value.reduce((total, provider) => total + providerOpenSlotCount(provider), 0)
);
const pendingAppointmentCount = computed(
  () => appointments.value.filter((appointment) => appointment.status === 'pending_confirm').length
);
const visibleReviewCount = computed(() => reviews.value.filter((review) => review.status === 'visible').length);
const activeServiceItemCount = computed(() =>
  providers.value.reduce(
    (total, provider) =>
      total + provider.service_items.filter((serviceItem) => serviceItem.status === 'active').length,
    0
  )
);

const summaryCards = computed(() => [
  {
    title: '开通城市',
    description: '当前对用户端开放服务中心的城市。',
    value: `${openedCityCount.value} 个`
  },
  {
    title: '服务商',
    description: '后台已纳入管理的服务商数量。',
    value: `${providers.value.length} 家`
  },
  {
    title: '服务项目',
    description: '当前启用的服务项目数量。',
    value: `${activeServiceItemCount.value} 项`
  },
  {
    title: '可约时段',
    description: '近 60 天仍可被用户预约的时段。',
    value: `${openSlotCount.value} 个`
  },
  {
    title: '待确认',
    description: '需要后台跟进确认的预约。',
    value: `${pendingAppointmentCount.value} 条`
  },
  {
    title: '评价',
    description: '当前展示中的服务评价数量。',
    value: `${visibleReviewCount.value} 条`
  },
  {
    title: '审计记录',
    description: '最近可追踪的服务后台操作。',
    value: `${auditLogs.value.length} 条`
  }
]);

const cityConfigDialogTitle = computed(() => (editingCityCode.value ? '编辑城市配置' : '新增城市配置'));
const providerDialogTitle = computed(() => (editingProviderId.value ? '编辑服务商' : '新增服务商'));
const serviceItemDialogTitle = computed(() => (editingServiceItemId.value ? '编辑服务项目' : '新增服务项目'));
const scheduleSlotDialogTitle = computed(() => (editingScheduleSlotId.value ? '编辑预约时段' : '新增预约时段'));
const appointmentStatusDialogTitle = computed(
  () => `更新为${appointmentStatusLabel(appointmentStatusForm.status)}`
);

onMounted(() => {
  void loadAll();
});

async function loadAll() {
  await Promise.all([loadCityConfigs(), loadProviders(), loadAppointments(), loadReviews(), loadAuditLogs()]);
}

async function loadCityConfigs() {
  cityConfigLoading.value = true;
  cityConfigErrorMessage.value = '';

  try {
    cityConfigs.value = await listServiceCityConfigs({
      opened: cityConfigFilters.opened,
      cityCode: cityConfigFilters.cityCode
    });
  } catch (error) {
    cityConfigErrorMessage.value = error instanceof Error ? error.message : '城市配置加载失败';
  } finally {
    cityConfigLoading.value = false;
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
    providerErrorMessage.value = error instanceof Error ? error.message : '服务商列表加载失败';
  } finally {
    providerLoading.value = false;
  }
}

async function loadAppointments() {
  appointmentLoading.value = true;
  appointmentErrorMessage.value = '';

  try {
    appointments.value = await listServiceAppointments({
      status: appointmentFilters.status,
      providerType: appointmentFilters.providerType,
      cityCode: appointmentFilters.cityCode
    });
  } catch (error) {
    appointmentErrorMessage.value = error instanceof Error ? error.message : '预约记录加载失败';
  } finally {
    appointmentLoading.value = false;
  }
}

async function loadReviews() {
  reviewLoading.value = true;
  reviewErrorMessage.value = '';

  try {
    reviews.value = await listProviderReviews({
      status: reviewFilters.status,
      providerType: reviewFilters.providerType,
      cityCode: reviewFilters.cityCode
    });
  } catch (error) {
    reviewErrorMessage.value = error instanceof Error ? error.message : '评价列表加载失败';
  } finally {
    reviewLoading.value = false;
  }
}

async function loadAuditLogs() {
  auditLogLoading.value = true;
  auditLogErrorMessage.value = '';

  try {
    auditLogs.value = await listServiceAuditLogs({
      targetType: auditLogFilters.targetType,
      operatorId: auditLogFilters.operatorId,
      action: auditLogFilters.action
    });
  } catch (error) {
    auditLogErrorMessage.value = error instanceof Error ? error.message : '审计日志加载失败';
  } finally {
    auditLogLoading.value = false;
  }
}

function openCreateCityConfigDialog() {
  editingCityCode.value = null;
  Object.assign(cityConfigForm, createDefaultCityConfigForm());
  cityConfigDialogVisible.value = true;
}

function openEditCityConfigDialog(cityConfig: ServiceCityConfigSnapshot) {
  editingCityCode.value = cityConfig.city_code;
  Object.assign(cityConfigForm, {
    cityCode: cityConfig.city_code,
    cityName: cityConfig.city_name,
    opened: cityConfig.opened,
    unavailableReason: cityConfig.unavailable_reason ?? '',
    sortOrder: cityConfig.sort_order
  });
  cityConfigDialogVisible.value = true;
}

async function submitCityConfigForm() {
  if (!validateCityConfigForm()) {
    return;
  }

  cityConfigSubmitting.value = true;
  try {
    const updatedCityConfig = await upsertServiceCityConfig({
      city_code: cityConfigForm.cityCode.trim(),
      city_name: cityConfigForm.cityName.trim(),
      opened: cityConfigForm.opened,
      unavailable_reason: cityConfigForm.opened ? null : normalizeNullableText(cityConfigForm.unavailableReason),
      sort_order: cityConfigForm.sortOrder
    });
    upsertCityConfig(updatedCityConfig);
    cityConfigDialogVisible.value = false;
    ElMessage.success('城市配置已保存');
    await Promise.all([loadProviders(), loadAppointments(), loadReviews(), loadAuditLogs()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '城市配置保存失败');
  } finally {
    cityConfigSubmitting.value = false;
  }
}

async function toggleCityConfig(cityConfig: ServiceCityConfigSnapshot) {
  processingCityCode.value = cityConfig.city_code;
  try {
    const nextOpened = !cityConfig.opened;
    const updatedCityConfig = await upsertServiceCityConfig({
      city_code: cityConfig.city_code,
      city_name: cityConfig.city_name,
      opened: nextOpened,
      unavailable_reason: nextOpened ? null : cityConfig.unavailable_reason || '当前城市服务正在准备中',
      sort_order: cityConfig.sort_order
    });
    upsertCityConfig(updatedCityConfig);
    ElMessage.success(nextOpened ? '城市已开通' : '城市已关闭');
    await Promise.all([loadProviders(), loadAppointments(), loadReviews(), loadAuditLogs()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '城市状态更新失败');
  } finally {
    processingCityCode.value = null;
  }
}

function openCreateProviderDialog() {
  editingProviderId.value = null;
  Object.assign(providerForm, createDefaultProviderForm());
  providerDialogVisible.value = true;
}

function openEditProviderDialog(provider: ServiceProviderSnapshot) {
  editingProviderId.value = provider.provider_id;
  Object.assign(providerForm, {
    providerType: provider.provider_type,
    providerName: provider.provider_name,
    cityCode: provider.city_code,
    address: provider.address ?? '',
    latitude: provider.latitude,
    longitude: provider.longitude,
    contactPhone: provider.contact_phone ?? '',
    businessHours: provider.business_hours ?? '',
    ratingAvg: provider.rating_avg,
    reviewCount: provider.review_count,
    status: provider.status
  });
  providerDialogVisible.value = true;
}

async function submitProviderForm() {
  if (!validateProviderForm()) {
    return;
  }

  providerSubmitting.value = true;
  try {
    const payload = {
      provider_type: providerForm.providerType,
      provider_name: providerForm.providerName.trim(),
      city_code: providerForm.cityCode.trim(),
      address: normalizeNullableText(providerForm.address),
      latitude: providerForm.latitude,
      longitude: providerForm.longitude,
      contact_phone: normalizeNullableText(providerForm.contactPhone),
      business_hours: normalizeNullableText(providerForm.businessHours),
      rating_avg: providerForm.ratingAvg,
      review_count: providerForm.reviewCount,
      status: providerForm.status
    };
    const updatedProvider = editingProviderId.value
      ? await updateServiceProvider(editingProviderId.value, payload)
      : await createServiceProvider(payload);
    upsertProvider(updatedProvider);
    providerDialogVisible.value = false;
    ElMessage.success('服务商已保存');
    await loadAuditLogs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '服务商保存失败');
  } finally {
    providerSubmitting.value = false;
  }
}

function openCreateServiceItemDialog(provider: ServiceProviderSnapshot, serviceItem?: ProviderServiceItemSnapshot) {
  activeProvider.value = provider;
  editingServiceItemId.value = serviceItem?.service_item_id ?? null;
  Object.assign(
    serviceItemForm,
    serviceItem
      ? {
          serviceCode: serviceItem.service_code,
          serviceName: serviceItem.service_name,
          serviceDesc: serviceItem.service_desc ?? '',
          priceMin: serviceItem.price_min,
          priceMax: serviceItem.price_max,
          status: serviceItem.status
        }
      : createDefaultServiceItemForm()
  );
  serviceItemDialogVisible.value = true;
}

async function submitServiceItemForm() {
  if (!activeProvider.value || !validateServiceItemForm()) {
    return;
  }

  serviceItemSubmitting.value = true;
  try {
    const payload = {
      service_code: serviceItemForm.serviceCode.trim(),
      service_name: serviceItemForm.serviceName.trim(),
      service_desc: normalizeNullableText(serviceItemForm.serviceDesc),
      price_min: serviceItemForm.priceMin,
      price_max: serviceItemForm.priceMax,
      status: serviceItemForm.status
    };
    const updatedProvider = editingServiceItemId.value
      ? await updateProviderServiceItem(activeProvider.value.provider_id, editingServiceItemId.value, payload)
      : await createProviderServiceItem(activeProvider.value.provider_id, payload);
    upsertProvider(updatedProvider);
    serviceItemDialogVisible.value = false;
    ElMessage.success('服务项目已保存');
    await loadAuditLogs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '服务项目保存失败');
  } finally {
    serviceItemSubmitting.value = false;
  }
}

function openCreateSlotDialog(provider: ServiceProviderSnapshot, slot?: ProviderScheduleSlotSnapshot) {
  activeProvider.value = provider;
  activeScheduleSlot.value = slot ?? null;
  editingScheduleSlotId.value = slot?.slot_id ?? null;
  Object.assign(
    scheduleSlotForm,
    slot
      ? {
          appointmentType: slot.appointment_type,
          slotDate: slot.slot_date,
          startTime: slot.start_time,
          endTime: slot.end_time,
          quota: slot.quota,
          status: slot.status
        }
      : createDefaultScheduleSlotForm(provider.provider_type)
  );
  scheduleSlotDialogVisible.value = true;
}

async function submitScheduleSlotForm() {
  if (!activeProvider.value || !validateScheduleSlotForm()) {
    return;
  }

  scheduleSlotSubmitting.value = true;
  try {
    const payload = {
      appointment_type: scheduleSlotForm.appointmentType,
      slot_date: scheduleSlotForm.slotDate,
      start_time: scheduleSlotForm.startTime,
      end_time: scheduleSlotForm.endTime,
      quota: scheduleSlotForm.quota,
      status: scheduleSlotForm.status
    };
    const updatedProvider = editingScheduleSlotId.value
      ? await updateProviderScheduleSlot(activeProvider.value.provider_id, editingScheduleSlotId.value, payload)
      : await createProviderScheduleSlot(activeProvider.value.provider_id, payload);
    upsertProvider(updatedProvider);
    scheduleSlotDialogVisible.value = false;
    ElMessage.success('预约时段已保存');
    await loadAuditLogs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约时段保存失败');
  } finally {
    scheduleSlotSubmitting.value = false;
  }
}

function openAppointmentStatusDialog(
  appointment: ServiceAppointmentSnapshot,
  nextStatus: ServiceAppointmentStatus
) {
  activeAppointment.value = appointment;
  appointmentStatusForm.status = nextStatus;
  appointmentStatusForm.remark = defaultAppointmentRemark(nextStatus);
  appointmentStatusDialogVisible.value = true;
}

async function submitAppointmentStatusForm() {
  if (!activeAppointment.value) {
    return;
  }

  appointmentStatusSubmitting.value = true;
  try {
    const updatedAppointment = await updateServiceAppointmentStatus(
      activeAppointment.value.appointment_id,
      appointmentStatusForm.status,
      normalizeNullableText(appointmentStatusForm.remark)
    );
    upsertAppointment(updatedAppointment);
    appointmentStatusDialogVisible.value = false;
    ElMessage.success('预约状态已更新');
    if (updatedAppointment.status === 'canceled') {
      await loadProviders();
    }
    await loadAuditLogs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约状态更新失败');
  } finally {
    appointmentStatusSubmitting.value = false;
  }
}

async function handleReviewStatus(review: ProviderReviewSnapshot, status: ProviderReviewStatus) {
  processingReviewId.value = review.review_id;
  try {
    const updatedReview = await updateProviderReviewStatus(review.review_id, status);
    const reviewIndex = reviews.value.findIndex((item) => item.review_id === updatedReview.review_id);
    if (reviewIndex >= 0) {
      reviews.value.splice(reviewIndex, 1, updatedReview);
    }
    ElMessage.success(status === 'visible' ? '评价已恢复展示' : '评价已隐藏');
    await Promise.all([loadProviders(), loadAuditLogs()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评价状态更新失败');
  } finally {
    processingReviewId.value = null;
  }
}

function reviewStatusLabel(status: ProviderReviewStatus) {
  const labelMap: Record<ProviderReviewStatus, string> = {
    visible: '展示中',
    hidden: '已隐藏'
  };
  return labelMap[status];
}

function reviewStatusTagType(status: ProviderReviewStatus) {
  return status === 'visible' ? 'success' : 'info';
}

function auditTargetTypeLabel(targetType: ServiceAuditTargetType) {
  const option = auditTargetTypeOptions.find((item) => item.value === targetType);
  return option?.label ?? targetType;
}

function auditActionLabel(action: string) {
  const labelMap: Record<string, string> = {
    service_city_upsert: '保存城市配置',
    service_provider_create: '新增服务商',
    service_provider_update: '编辑服务商',
    provider_service_item_create: '新增服务项目',
    provider_service_item_update: '编辑服务项目',
    provider_schedule_slot_create: '新增预约时段',
    provider_schedule_slot_update: '编辑预约时段',
    service_appointment_status_update: '更新预约状态',
    provider_review_status_update: '更新评价状态'
  };
  return labelMap[action] ?? action;
}

function formatAuditDetail(detailJson: string) {
  try {
    return JSON.stringify(JSON.parse(detailJson), null, 2);
  } catch {
    return detailJson || '{}';
  }
}

function validateCityConfigForm() {
  if (!cityConfigForm.cityCode.trim()) {
    ElMessage.warning('城市编码不能为空');
    return false;
  }
  if (!cityConfigForm.cityName.trim()) {
    ElMessage.warning('城市名称不能为空');
    return false;
  }
  if (!cityConfigForm.opened && !cityConfigForm.unavailableReason.trim()) {
    ElMessage.warning('未开通城市需要填写展示原因');
    return false;
  }
  return true;
}

function validateProviderForm() {
  if (!providerForm.providerName.trim()) {
    ElMessage.warning('服务商名称不能为空');
    return false;
  }
  if (!providerForm.cityCode.trim()) {
    ElMessage.warning('城市编码不能为空');
    return false;
  }
  return true;
}

function validateServiceItemForm() {
  if (!serviceItemForm.serviceCode.trim()) {
    ElMessage.warning('服务编码不能为空');
    return false;
  }
  if (!serviceItemForm.serviceName.trim()) {
    ElMessage.warning('服务名称不能为空');
    return false;
  }
  if (
    serviceItemForm.priceMin !== null &&
    serviceItemForm.priceMax !== null &&
    serviceItemForm.priceMax < serviceItemForm.priceMin
  ) {
    ElMessage.warning('最高价格不能低于最低价格');
    return false;
  }
  return true;
}

function validateScheduleSlotForm() {
  if (!scheduleSlotForm.slotDate) {
    ElMessage.warning('预约日期不能为空');
    return false;
  }
  if (!scheduleSlotForm.startTime || !scheduleSlotForm.endTime) {
    ElMessage.warning('预约时间不能为空');
    return false;
  }
  if (scheduleSlotForm.endTime <= scheduleSlotForm.startTime) {
    ElMessage.warning('结束时间必须晚于开始时间');
    return false;
  }
  if (activeScheduleSlot.value && scheduleSlotForm.quota < activeScheduleSlot.value.booked_count) {
    ElMessage.warning(`总名额不能少于已预约数量 ${activeScheduleSlot.value.booked_count}`);
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

function upsertAppointment(appointment: ServiceAppointmentSnapshot) {
  const appointmentIndex = appointments.value.findIndex(
    (item) => item.appointment_id === appointment.appointment_id
  );
  if (appointmentIndex >= 0) {
    appointments.value.splice(appointmentIndex, 1, appointment);
    return;
  }
  appointments.value.unshift(appointment);
}

function upsertCityConfig(cityConfig: ServiceCityConfigSnapshot) {
  const cityConfigIndex = cityConfigs.value.findIndex((item) => item.city_code === cityConfig.city_code);
  if (cityConfigIndex >= 0) {
    cityConfigs.value.splice(cityConfigIndex, 1, cityConfig);
    return;
  }
  cityConfigs.value.unshift(cityConfig);
}

function providerOpenSlotCount(provider: ServiceProviderSnapshot) {
  return provider.available_slots.filter((slot) => slot.status === 'open' && slot.bookable).length;
}

function priceRangeLabel(serviceItem: ProviderServiceItemSnapshot) {
  if (serviceItem.price_min === null && serviceItem.price_max === null) {
    return '暂未维护价格';
  }
  if (serviceItem.price_min !== null && serviceItem.price_max !== null) {
    return `¥${serviceItem.price_min} - ¥${serviceItem.price_max}`;
  }
  if (serviceItem.price_min !== null) {
    return `¥${serviceItem.price_min} 起`;
  }
  return `最高 ¥${serviceItem.price_max}`;
}

function serviceItemStatusLabel(status: ProviderServiceItemStatus) {
  const labelMap: Record<ProviderServiceItemStatus, string> = {
    active: '启用',
    inactive: '停用'
  };
  return labelMap[status];
}

function serviceItemStatusTagType(status: ProviderServiceItemStatus) {
  return status === 'active' ? 'success' : 'info';
}

function slotStatusLabel(status: ProviderScheduleSlotStatus) {
  const labelMap: Record<ProviderScheduleSlotStatus, string> = {
    open: '开放',
    closed: '关闭',
    full: '已满'
  };
  return labelMap[status];
}

function slotStatusTagType(status: ProviderScheduleSlotStatus) {
  if (status === 'open') {
    return 'success';
  }
  if (status === 'closed') {
    return 'info';
  }
  return 'warning';
}

function formatSlotTime(slot: ProviderScheduleSlotSnapshot) {
  return `${slot.start_time.slice(0, 5)}-${slot.end_time.slice(0, 5)}`;
}

function createDefaultCityConfigForm(): CityConfigFormState {
  return {
    cityCode: '310000',
    cityName: '上海',
    opened: true,
    unavailableReason: '',
    sortOrder: 0
  };
}

function createDefaultProviderForm(): ProviderFormState {
  return {
    providerType: 'hospital',
    providerName: '',
    cityCode: '310000',
    address: '',
    latitude: null,
    longitude: null,
    contactPhone: '',
    businessHours: '',
    ratingAvg: null,
    reviewCount: 0,
    status: 'online'
  };
}

function createDefaultServiceItemForm(): ServiceItemFormState {
  return {
    serviceCode: '',
    serviceName: '',
    serviceDesc: '',
    priceMin: null,
    priceMax: null,
    status: 'active'
  };
}

function createDefaultScheduleSlotForm(providerType: ServiceProviderType = 'hospital'): ScheduleSlotFormState {
  return {
    appointmentType: providerType,
    slotDate: defaultSlotDate(),
    startTime: '09:00:00',
    endTime: '10:00:00',
    quota: 2,
    status: 'open'
  };
}

function defaultSlotDate() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString().slice(0, 10);
}

function normalizeNullableText(value: string) {
  const normalizedValue = value.trim();
  return normalizedValue.length > 0 ? normalizedValue : null;
}

function defaultAppointmentRemark(status: ServiceAppointmentStatus) {
  const remarkMap: Record<ServiceAppointmentStatus, string> = {
    pending_confirm: '',
    confirmed: '后台已确认服务商和预约时段',
    completed: '服务已完成',
    canceled: '后台取消预约'
  };
  return remarkMap[status];
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

function cityOpenedLabel(opened: boolean) {
  return opened ? '已开通' : '未开通';
}

function cityOpenedTagType(opened: boolean) {
  return opened ? 'success' : 'info';
}

function providerStatusLabel(status: ServiceProviderStatus) {
  const labelMap: Record<ServiceProviderStatus, string> = {
    online: '在线',
    rest: '休息中',
    offline: '下线'
  };
  return labelMap[status];
}

function appointmentStatusLabel(status: ServiceAppointmentStatus) {
  const labelMap: Record<ServiceAppointmentStatus, string> = {
    pending_confirm: '待确认',
    confirmed: '已确认',
    completed: '已完成',
    canceled: '已取消'
  };
  return labelMap[status];
}

function providerTypeTagType(providerType: ServiceProviderType) {
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

function providerStatusTagType(status: ServiceProviderStatus) {
  if (status === 'online') {
    return 'success';
  }
  if (status === 'rest') {
    return 'warning';
  }
  return 'info';
}

function appointmentStatusTagType(status: ServiceAppointmentStatus) {
  if (status === 'pending_confirm') {
    return 'warning';
  }
  if (status === 'confirmed') {
    return 'primary';
  }
  if (status === 'completed') {
    return 'success';
  }
  return 'info';
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.service-summary,
.service-section {
  margin-top: 24px;
}

.service-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.service-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.service-filter {
  width: 138px;
}

.service-error {
  margin-bottom: 16px;
}

.service-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.service-provider-detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 14px 18px 18px;
  background: #fffaf5;
}

.service-provider-detail__panel {
  padding: 16px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 18px;
  background: var(--pet-admin-surface);
}

.service-provider-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.service-provider-detail__header h3 {
  margin: 0;
  color: var(--pet-admin-title);
  font-size: 16px;
}

.service-provider-detail__header p {
  margin: 6px 0 0;
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.service-empty-inline {
  padding: 16px;
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.service-resource-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.service-resource-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: #fffdfb;
}

.service-resource-card__main {
  min-width: 0;
}

.service-resource-card__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.service-provider-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.service-provider-cell__title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.service-provider-cell__meta,
.service-remark,
.service-action-text,
.service-review-content,
.service-audit-detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.service-review-content {
  color: var(--pet-admin-body);
}

.service-audit-detail {
  max-height: 132px;
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  border-radius: 14px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-body);
  white-space: pre-wrap;
}

.service-status-stack,
.service-slot-overview,
.service-rating {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.service-status-stack span,
.service-slot-overview span,
.service-rating span {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.service-slot-overview strong,
.service-rating strong {
  color: var(--pet-admin-title);
}

.service-inline-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-muted);
}

.service-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.service-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.service-form-control {
  width: 100%;
}

.service-dialog-context {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-title);
  font-size: 13px;
  font-weight: 700;
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

:deep(.el-dialog) {
  border-radius: 24px;
}

:deep(.el-dialog__title) {
  color: var(--pet-admin-title);
  font-weight: 700;
}

@media (max-width: 1080px) {
  .service-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .service-toolbar__actions {
    justify-content: flex-start;
  }

  .service-provider-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .service-filter {
    width: 100%;
  }

  .service-toolbar__actions,
  .service-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .service-form-grid {
    grid-template-columns: 1fr;
  }

  .service-provider-detail__header,
  .service-resource-card {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
