-- PetLife end-to-end test data seed.
-- Scope:
-- 1. This script is repeatable and only cleans rows created by this seed.
-- 2. Isolation is based on the [TEST] name prefix, 19900008xxx mobile range,
--    test object keys, test order numbers, test device serial numbers and test
--    provider/template codes.
-- 3. Do not replace this with broad TRUNCATE/DELETE statements in a shared DB.

USE `pet_life`;

START TRANSACTION;

SET @seed_prefix = '[TEST]';
SET @seed_mobile_prefix = '19900008';
SET @seed_object_prefix = 'test-data/petlife/';
SET @seed_now = CURRENT_TIMESTAMP;

DROP TEMPORARY TABLE IF EXISTS seed_users;
DROP TEMPORARY TABLE IF EXISTS seed_admins;
DROP TEMPORARY TABLE IF EXISTS seed_families;
DROP TEMPORARY TABLE IF EXISTS seed_pets;
DROP TEMPORARY TABLE IF EXISTS seed_media;
DROP TEMPORARY TABLE IF EXISTS seed_health;
DROP TEMPORARY TABLE IF EXISTS seed_reminders;
DROP TEMPORARY TABLE IF EXISTS seed_daily_logs;
DROP TEMPORARY TABLE IF EXISTS seed_timeline_events;
DROP TEMPORARY TABLE IF EXISTS seed_topics;
DROP TEMPORARY TABLE IF EXISTS seed_posts;
DROP TEMPORARY TABLE IF EXISTS seed_comments;
DROP TEMPORARY TABLE IF EXISTS seed_providers;
DROP TEMPORARY TABLE IF EXISTS seed_products;
DROP TEMPORARY TABLE IF EXISTS seed_skus;
DROP TEMPORARY TABLE IF EXISTS seed_orders;
DROP TEMPORARY TABLE IF EXISTS seed_devices;
DROP TEMPORARY TABLE IF EXISTS seed_appointments;

CREATE TEMPORARY TABLE seed_users AS
SELECT id FROM users WHERE mobile LIKE CONCAT(@seed_mobile_prefix, '%');

CREATE TEMPORARY TABLE seed_admins AS
SELECT id FROM admin_accounts WHERE username LIKE 'plt_%';

CREATE TEMPORARY TABLE seed_families AS
SELECT id FROM families
WHERE family_name LIKE CONCAT(@seed_prefix, '%')
   OR owner_user_id IN (SELECT id FROM seed_users);

CREATE TEMPORARY TABLE seed_pets AS
SELECT id FROM pets
WHERE pet_name LIKE CONCAT(@seed_prefix, '%')
   OR owner_user_id IN (SELECT id FROM seed_users)
   OR family_id IN (SELECT id FROM seed_families);

CREATE TEMPORARY TABLE seed_media AS
SELECT id FROM media_assets
WHERE object_key LIKE CONCAT(@seed_object_prefix, '%')
   OR uploader_user_id IN (SELECT id FROM seed_users)
   OR file_name LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_health AS
SELECT id FROM pet_health_records
WHERE pet_id IN (SELECT id FROM seed_pets)
   OR title LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_reminders AS
SELECT id FROM pet_reminders
WHERE pet_id IN (SELECT id FROM seed_pets)
   OR source_record_id IN (SELECT id FROM seed_health)
   OR title LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_daily_logs AS
SELECT id FROM pet_daily_logs
WHERE pet_id IN (SELECT id FROM seed_pets)
   OR author_user_id IN (SELECT id FROM seed_users)
   OR title LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_timeline_events AS
SELECT id FROM pet_timeline_events
WHERE pet_id IN (SELECT id FROM seed_pets)
   OR title LIKE CONCAT(@seed_prefix, '%')
   OR (source_type IN ('health_record', 'daily_log', 'service_appointment', 'device_event')
       AND source_id IN (
         SELECT id FROM seed_health
         UNION SELECT id FROM seed_daily_logs
       ));

CREATE TEMPORARY TABLE seed_topics AS
SELECT id FROM community_topics
WHERE topic_name LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_posts AS
SELECT id FROM community_posts
WHERE user_id IN (SELECT id FROM seed_users)
   OR pet_id IN (SELECT id FROM seed_pets)
   OR topic_id IN (SELECT id FROM seed_topics)
   OR source_daily_log_id IN (SELECT id FROM seed_daily_logs)
   OR title LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_comments AS
SELECT id FROM community_comments
WHERE post_id IN (SELECT id FROM seed_posts)
   OR user_id IN (SELECT id FROM seed_users)
   OR content LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_providers AS
SELECT id FROM service_providers
WHERE provider_name LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_appointments AS
SELECT id FROM service_appointments
WHERE user_id IN (SELECT id FROM seed_users)
   OR pet_id IN (SELECT id FROM seed_pets)
   OR provider_id IN (SELECT id FROM seed_providers)
   OR contact_mobile LIKE CONCAT(@seed_mobile_prefix, '%');

CREATE TEMPORARY TABLE seed_products AS
SELECT id FROM products
WHERE product_name LIKE CONCAT(@seed_prefix, '%');

CREATE TEMPORARY TABLE seed_skus AS
SELECT id FROM product_skus
WHERE product_id IN (SELECT id FROM seed_products)
   OR sku_code LIKE 'PLT-SEED-%';

CREATE TEMPORARY TABLE seed_orders AS
SELECT id FROM orders
WHERE order_no LIKE 'PLT-SEED-%'
   OR user_id IN (SELECT id FROM seed_users)
   OR receiver_mobile LIKE CONCAT(@seed_mobile_prefix, '%');

CREATE TEMPORARY TABLE seed_devices AS
SELECT id FROM devices
WHERE device_sn LIKE 'PLT-SEED-%';

DELETE FROM outbox_events
WHERE payload_json LIKE '%PLT-SEED%'
   OR payload_json LIKE CONCAT('%', @seed_prefix, '%')
   OR aggregate_id IN (
     SELECT id FROM seed_pets
     UNION SELECT id FROM seed_orders
     UNION SELECT id FROM seed_devices
   );

DELETE FROM audit_logs
WHERE operator_id LIKE 'plt_%'
   OR detail_json LIKE '%PLT-SEED%'
   OR target_id IN (
     SELECT CAST(id AS CHAR) FROM seed_pets
     UNION SELECT CAST(id AS CHAR) FROM seed_posts
     UNION SELECT CAST(id AS CHAR) FROM seed_orders
   );

DELETE FROM push_delivery_records
WHERE user_id IN (SELECT id FROM seed_users)
   OR failure_reason LIKE '%PLT-SEED%';

DELETE FROM push_tasks
WHERE user_id IN (SELECT id FROM seed_users)
   OR title LIKE CONCAT(@seed_prefix, '%')
   OR failure_reason LIKE '%PLT-SEED%';

DELETE FROM user_push_device_tokens
WHERE user_id IN (SELECT id FROM seed_users)
   OR device_token LIKE 'PLT-SEED-PUSH-%';

DELETE FROM moderation_tasks
WHERE (target_type IN ('community_post', 'community_question', 'post') AND target_id IN (SELECT id FROM seed_posts))
   OR JSON_UNQUOTE(JSON_EXTRACT(content_snapshot, '$.seed_code')) LIKE 'PLT-SEED-%'
   OR JSON_UNQUOTE(JSON_EXTRACT(review_result, '$.seed_code')) LIKE 'PLT-SEED-%'
   OR failure_reason LIKE '%PLT-SEED%';

DELETE FROM notifications
WHERE user_id IN (SELECT id FROM seed_users)
   OR title LIKE CONCAT(@seed_prefix, '%');

DELETE FROM admin_sessions WHERE admin_account_id IN (SELECT id FROM seed_admins);
DELETE FROM sms_send_records WHERE mobile LIKE CONCAT(@seed_mobile_prefix, '%');
DELETE FROM sms_verification_codes WHERE mobile LIKE CONCAT(@seed_mobile_prefix, '%');

DELETE FROM device_events WHERE device_id IN (SELECT id FROM seed_devices) OR pet_id IN (SELECT id FROM seed_pets);
DELETE FROM device_snapshots WHERE device_id IN (SELECT id FROM seed_devices) OR pet_id IN (SELECT id FROM seed_pets);
DELETE FROM device_bindings WHERE device_id IN (SELECT id FROM seed_devices) OR user_id IN (SELECT id FROM seed_users) OR pet_id IN (SELECT id FROM seed_pets);
DELETE FROM devices WHERE id IN (SELECT id FROM seed_devices);

DELETE FROM order_status_logs WHERE order_id IN (SELECT id FROM seed_orders);
DELETE FROM order_items WHERE order_id IN (SELECT id FROM seed_orders) OR product_id IN (SELECT id FROM seed_products) OR sku_id IN (SELECT id FROM seed_skus);
DELETE FROM cart_items WHERE user_id IN (SELECT id FROM seed_users) OR pet_id IN (SELECT id FROM seed_pets) OR sku_id IN (SELECT id FROM seed_skus);
DELETE FROM orders WHERE id IN (SELECT id FROM seed_orders);
DELETE FROM user_addresses WHERE user_id IN (SELECT id FROM seed_users) OR receiver_mobile LIKE CONCAT(@seed_mobile_prefix, '%');
DELETE FROM product_skus WHERE id IN (SELECT id FROM seed_skus);
DELETE FROM products WHERE id IN (SELECT id FROM seed_products);

DELETE FROM provider_reviews
WHERE provider_id IN (SELECT id FROM seed_providers)
   OR appointment_id IN (SELECT id FROM seed_appointments)
   OR user_id IN (SELECT id FROM seed_users)
   OR pet_id IN (SELECT id FROM seed_pets);

DELETE FROM service_appointments WHERE id IN (SELECT id FROM seed_appointments);
DELETE FROM provider_schedule_slots WHERE provider_id IN (SELECT id FROM seed_providers);
DELETE FROM provider_service_items WHERE provider_id IN (SELECT id FROM seed_providers);
DELETE FROM service_providers WHERE id IN (SELECT id FROM seed_providers);
DELETE FROM service_city_configs WHERE city_code IN ('PLT_SH', 'PLT_HZ', 'PLT_CD');

DELETE FROM community_reports
WHERE reporter_user_id IN (SELECT id FROM seed_users)
   OR target_id IN (SELECT id FROM seed_posts)
   OR reason_detail LIKE CONCAT(@seed_prefix, '%');

DELETE FROM community_post_favorites WHERE post_id IN (SELECT id FROM seed_posts) OR user_id IN (SELECT id FROM seed_users);
DELETE FROM community_post_reactions WHERE post_id IN (SELECT id FROM seed_posts) OR user_id IN (SELECT id FROM seed_users);
UPDATE community_comments c
JOIN seed_comments sc ON c.id = sc.id
SET c.parent_comment_id = NULL;

UPDATE community_comments c
JOIN seed_comments sc ON c.parent_comment_id = sc.id
SET c.parent_comment_id = NULL;
DELETE FROM community_comments WHERE id IN (SELECT id FROM seed_comments);
DELETE FROM community_posts WHERE id IN (SELECT id FROM seed_posts);
DELETE FROM community_topics WHERE id IN (SELECT id FROM seed_topics);

DELETE FROM pet_timeline_events
WHERE id IN (SELECT id FROM seed_timeline_events)
   OR pet_id IN (SELECT id FROM seed_pets)
   OR title LIKE CONCAT(@seed_prefix, '%');

DELETE FROM pet_daily_logs WHERE id IN (SELECT id FROM seed_daily_logs);
DELETE FROM pet_reminders WHERE id IN (SELECT id FROM seed_reminders);
DELETE FROM pet_health_records WHERE id IN (SELECT id FROM seed_health);
DELETE FROM reminder_templates WHERE template_name LIKE CONCAT(@seed_prefix, '%');
DELETE FROM media_assets WHERE id IN (SELECT id FROM seed_media);

DELETE FROM family_invitations WHERE invite_code LIKE 'PLT-SEED-%';

DELETE fi FROM family_invitations fi
JOIN seed_families sf ON fi.family_id = sf.id;

DELETE fi FROM family_invitations fi
JOIN seed_users su ON fi.inviter_user_id = su.id;

DELETE fi FROM family_invitations fi
JOIN seed_users su ON fi.invitee_user_id = su.id;

DELETE FROM family_members WHERE family_id IN (SELECT id FROM seed_families) OR user_id IN (SELECT id FROM seed_users);
DELETE FROM user_settings WHERE user_id IN (SELECT id FROM seed_users);
DELETE FROM user_sessions WHERE user_id IN (SELECT id FROM seed_users);
DELETE FROM pets WHERE id IN (SELECT id FROM seed_pets);
DELETE FROM families WHERE id IN (SELECT id FROM seed_families);
DELETE FROM message_templates WHERE template_code LIKE 'PLT_SEED_%';
DELETE FROM notification_channel_configs WHERE provider_code LIKE 'plt_%';
DELETE FROM admin_accounts WHERE id IN (SELECT id FROM seed_admins);
DELETE FROM users WHERE id IN (SELECT id FROM seed_users);

-- ====================================================================
-- User, admin, family and pet master data.
-- Admin seed password: petlife123
-- ====================================================================

INSERT INTO users (
  mobile, nickname, avatar_url, city_code, city_name, status,
  last_login_at, created_at, updated_at, deleted_at
) VALUES (
  '19900008001', '[TEST] 上海铲屎官林夏',
  'https://static.petlife.test/avatar/lin-xia.png',
  'PLT_SH', '上海市', 1,
  DATE_SUB(@seed_now, INTERVAL 1 HOUR), DATE_SUB(@seed_now, INTERVAL 120 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar_url = VALUES(avatar_url),
  city_code = VALUES(city_code),
  city_name = VALUES(city_name),
  status = VALUES(status),
  last_login_at = VALUES(last_login_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @user_owner_id = LAST_INSERT_ID();

INSERT INTO users (
  mobile, nickname, avatar_url, city_code, city_name, status,
  last_login_at, created_at, updated_at, deleted_at
) VALUES (
  '19900008002', '[TEST] 杭州家庭成员周宁',
  'https://static.petlife.test/avatar/zhou-ning.png',
  'PLT_HZ', '杭州市', 1,
  DATE_SUB(@seed_now, INTERVAL 2 HOUR), DATE_SUB(@seed_now, INTERVAL 80 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar_url = VALUES(avatar_url),
  city_code = VALUES(city_code),
  city_name = VALUES(city_name),
  status = VALUES(status),
  last_login_at = VALUES(last_login_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @user_member_id = LAST_INSERT_ID();

INSERT INTO users (
  mobile, nickname, avatar_url, city_code, city_name, status,
  last_login_at, created_at, updated_at, deleted_at
) VALUES (
  '19900008003', '[TEST] 成都关注用户许然',
  'https://static.petlife.test/avatar/xu-ran.png',
  'PLT_CD', '成都市', 1,
  DATE_SUB(@seed_now, INTERVAL 1 DAY), DATE_SUB(@seed_now, INTERVAL 45 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar_url = VALUES(avatar_url),
  city_code = VALUES(city_code),
  city_name = VALUES(city_name),
  status = VALUES(status),
  last_login_at = VALUES(last_login_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @user_follower_id = LAST_INSERT_ID();

INSERT INTO users (
  mobile, nickname, avatar_url, city_code, city_name, status,
  last_login_at, created_at, updated_at, deleted_at
) VALUES (
  '19900008004', '[TEST] 已禁用用户沈一',
  'https://static.petlife.test/avatar/disabled-user.png',
  'PLT_SH', '上海市', 2,
  DATE_SUB(@seed_now, INTERVAL 40 DAY), DATE_SUB(@seed_now, INTERVAL 200 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar_url = VALUES(avatar_url),
  city_code = VALUES(city_code),
  city_name = VALUES(city_name),
  status = VALUES(status),
  last_login_at = VALUES(last_login_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @user_disabled_id = LAST_INSERT_ID();

INSERT INTO admin_accounts (
  username, password_hash, display_name, role_code, status,
  last_login_at, created_at, updated_at, deleted_at
) VALUES (
  'plt_ops_admin',
  '$2a$10$lm1T.s53iApO9WHO.Sv8m.0bl4L1SWTBG8v50cwaYkC6A4wDA1YOe',
  '[TEST] 运营管理员', 'super_admin', 1,
  DATE_SUB(@seed_now, INTERVAL 30 MINUTE), DATE_SUB(@seed_now, INTERVAL 90 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  display_name = VALUES(display_name),
  role_code = VALUES(role_code),
  status = VALUES(status),
  last_login_at = VALUES(last_login_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @admin_ops_id = LAST_INSERT_ID();

INSERT INTO admin_accounts (
  username, password_hash, display_name, role_code, status,
  last_login_at, created_at, updated_at, deleted_at
) VALUES (
  'plt_disabled_admin',
  '$2a$10$lm1T.s53iApO9WHO.Sv8m.0bl4L1SWTBG8v50cwaYkC6A4wDA1YOe',
  '[TEST] 停用管理员', 'content_admin', 2,
  DATE_SUB(@seed_now, INTERVAL 20 DAY), DATE_SUB(@seed_now, INTERVAL 100 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  display_name = VALUES(display_name),
  role_code = VALUES(role_code),
  status = VALUES(status),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @admin_disabled_id = LAST_INSERT_ID();

INSERT INTO admin_sessions (
  admin_account_id, refresh_token_hash, expires_at, revoked_at,
  last_active_at, created_at, updated_at
) VALUES (
  @admin_ops_id,
  SHA2('PLT-SEED-admin-session-active', 256),
  DATE_ADD(@seed_now, INTERVAL 14 DAY),
  NULL,
  DATE_SUB(@seed_now, INTERVAL 20 MINUTE),
  DATE_SUB(@seed_now, INTERVAL 1 DAY),
  @seed_now
) ON DUPLICATE KEY UPDATE
  admin_account_id = VALUES(admin_account_id),
  expires_at = VALUES(expires_at),
  revoked_at = VALUES(revoked_at),
  last_active_at = VALUES(last_active_at),
  updated_at = @seed_now;

INSERT INTO families (
  family_name, owner_user_id, status, created_at, updated_at, deleted_at
) VALUES (
  '[TEST] 林夏的多宠家庭', @user_owner_id, 1,
  DATE_SUB(@seed_now, INTERVAL 118 DAY), @seed_now, NULL
);
SET @family_main_id = LAST_INSERT_ID();

INSERT INTO families (
  family_name, owner_user_id, status, created_at, updated_at, deleted_at
) VALUES (
  '[TEST] 停用数据观察家庭', @user_disabled_id, 2,
  DATE_SUB(@seed_now, INTERVAL 180 DAY), @seed_now, NULL
);
SET @family_disabled_id = LAST_INSERT_ID();

INSERT INTO family_members (family_id, user_id, role, invite_status, joined_at, created_at, updated_at)
VALUES
  (@family_main_id, @user_owner_id, 'owner', 'joined', DATE_SUB(@seed_now, INTERVAL 118 DAY), DATE_SUB(@seed_now, INTERVAL 118 DAY), @seed_now),
  (@family_main_id, @user_member_id, 'admin', 'joined', DATE_SUB(@seed_now, INTERVAL 70 DAY), DATE_SUB(@seed_now, INTERVAL 70 DAY), @seed_now),
  (@family_main_id, @user_follower_id, 'member', 'pending', NULL, DATE_SUB(@seed_now, INTERVAL 2 DAY), @seed_now),
  (@family_disabled_id, @user_disabled_id, 'owner', 'joined', DATE_SUB(@seed_now, INTERVAL 180 DAY), DATE_SUB(@seed_now, INTERVAL 180 DAY), @seed_now);

INSERT INTO family_invitations (
  family_id, inviter_user_id, invitee_mobile, invitee_user_id, role,
  shared_pet_ids, invite_code, status, expired_at, accepted_at, created_at, updated_at
) VALUES (
  @family_main_id, @user_owner_id, '19900008003', @user_follower_id, 'member',
  JSON_ARRAY(), 'PLT-SEED-FAMILY-INVITE-001', 'pending',
  DATE_ADD(@seed_now, INTERVAL 3 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 2 DAY), @seed_now
);

INSERT INTO pets (
  family_id, owner_user_id, pet_name, pet_type, breed, gender, birthday,
  adopt_date, neuter_status, avatar_url, weight_kg, allergy_notes,
  medical_history, status, created_at, updated_at, deleted_at
) VALUES (
  @family_main_id, @user_owner_id, '[TEST] 奶盖', 'cat', '英短银渐层', 'female',
  '2021-04-12', '2021-07-01', 1,
  'https://static.petlife.test/pets/naigai.png', 4.35,
  '对部分海鲜零食敏感',
  '2024 年完成绝育；偶发泪痕，需要定期观察眼部状态。',
  'active', DATE_SUB(@seed_now, INTERVAL 110 DAY), @seed_now, NULL
);
SET @pet_cat_id = LAST_INSERT_ID();

INSERT INTO pets (
  family_id, owner_user_id, pet_name, pet_type, breed, gender, birthday,
  adopt_date, neuter_status, avatar_url, weight_kg, allergy_notes,
  medical_history, status, created_at, updated_at, deleted_at
) VALUES (
  @family_main_id, @user_owner_id, '[TEST] 小风', 'dog', '边境牧羊犬', 'male',
  '2020-09-08', '2020-12-20', 0,
  'https://static.petlife.test/pets/xiaofeng.png', 18.60,
  '青霉素过敏',
  '髋关节需持续关注，运动后需要拉伸和休息。',
  'active', DATE_SUB(@seed_now, INTERVAL 105 DAY), @seed_now, NULL
);
SET @pet_dog_id = LAST_INSERT_ID();

INSERT INTO pets (
  family_id, owner_user_id, pet_name, pet_type, breed, gender, birthday,
  adopt_date, neuter_status, avatar_url, weight_kg, allergy_notes,
  medical_history, status, created_at, updated_at, deleted_at
) VALUES (
  @family_disabled_id, @user_disabled_id, '[TEST] 豆包', 'other', '侏儒兔', 'unknown',
  '2022-03-01', '2022-05-12', 0,
  'https://static.petlife.test/pets/doubao.png', 1.80,
  '无明确过敏',
  '测试停用用户名下宠物，用于后台筛选与异常状态展示。',
  'memorial', DATE_SUB(@seed_now, INTERVAL 170 DAY), @seed_now, NULL
);
SET @pet_other_id = LAST_INSERT_ID();

INSERT INTO user_settings (
  user_id, current_pet_id, notification_switch, privacy_level,
  extra_json, created_at, updated_at
) VALUES
  (@user_owner_id, @pet_cat_id, 1, 'normal', JSON_OBJECT('preferred_home_tab', 'dashboard', 'seed_tag', 'PLT-SEED'), @seed_now, @seed_now),
  (@user_member_id, @pet_dog_id, 1, 'family_only', JSON_OBJECT('preferred_home_tab', 'reminder', 'seed_tag', 'PLT-SEED'), @seed_now, @seed_now),
  (@user_follower_id, NULL, 1, 'normal', JSON_OBJECT('preferred_home_tab', 'community', 'seed_tag', 'PLT-SEED'), @seed_now, @seed_now),
  (@user_disabled_id, @pet_other_id, 0, 'private', JSON_OBJECT('disabled_reason', 'seed disabled user'), @seed_now, @seed_now)
ON DUPLICATE KEY UPDATE
  current_pet_id = VALUES(current_pet_id),
  notification_switch = VALUES(notification_switch),
  privacy_level = VALUES(privacy_level),
  extra_json = VALUES(extra_json),
  updated_at = @seed_now;

INSERT INTO user_sessions (
  user_id, refresh_token_hash, device_id, device_type, ip_address,
  user_agent, expires_at, revoked_at, last_active_at, created_at, updated_at
) VALUES
  (@user_owner_id, SHA2('PLT-SEED-user-session-owner', 256), 'ios-plt-seed-001', 'ios', '10.88.0.11', 'PetLife/iOS Seed', DATE_ADD(@seed_now, INTERVAL 30 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 5 MINUTE), DATE_SUB(@seed_now, INTERVAL 5 DAY), @seed_now),
  (@user_member_id, SHA2('PLT-SEED-user-session-member', 256), 'android-plt-seed-002', 'android', '10.88.0.12', 'PetLife/Android Seed', DATE_ADD(@seed_now, INTERVAL 14 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 50 MINUTE), DATE_SUB(@seed_now, INTERVAL 3 DAY), @seed_now)
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  device_id = VALUES(device_id),
  device_type = VALUES(device_type),
  expires_at = VALUES(expires_at),
  revoked_at = VALUES(revoked_at),
  last_active_at = VALUES(last_active_at),
  updated_at = @seed_now;

-- ====================================================================
-- SMS verification records. Real SMS vendor SDK integration: 该功能待完善.
-- ====================================================================

INSERT INTO sms_verification_codes (
  mobile, scene, code_hash, salt, expires_at, verified_at,
  attempt_count, max_attempt_count, status, request_ip, user_agent,
  created_at, updated_at
) VALUES
  ('19900008001', 'login', SHA2('PLT-SEED-246810:seed-salt-owner', 256), 'seed-salt-owner', DATE_ADD(@seed_now, INTERVAL 5 MINUTE), NULL, 0, 5, 'active', '10.88.0.11', 'PetLife/iOS Seed', DATE_SUB(@seed_now, INTERVAL 1 MINUTE), @seed_now),
  ('19900008002', 'login', SHA2('PLT-SEED-135790:seed-salt-member', 256), 'seed-salt-member', DATE_SUB(@seed_now, INTERVAL 5 MINUTE), DATE_SUB(@seed_now, INTERVAL 10 MINUTE), 1, 5, 'verified', '10.88.0.12', 'PetLife/Android Seed', DATE_SUB(@seed_now, INTERVAL 15 MINUTE), @seed_now),
  ('19900008004', 'login', SHA2('PLT-SEED-000000:seed-salt-disabled', 256), 'seed-salt-disabled', DATE_ADD(@seed_now, INTERVAL 5 MINUTE), NULL, 5, 5, 'locked', '10.88.0.14', 'PetLife/iOS Seed', DATE_SUB(@seed_now, INTERVAL 20 MINUTE), @seed_now);
SET @sms_active_id = LAST_INSERT_ID();

SELECT id INTO @sms_verified_id
FROM sms_verification_codes
WHERE mobile = '19900008002' AND scene = 'login' AND status = 'verified'
ORDER BY id DESC
LIMIT 1;

SELECT id INTO @sms_locked_id
FROM sms_verification_codes
WHERE mobile = '19900008004' AND scene = 'login' AND status = 'locked'
ORDER BY id DESC
LIMIT 1;

INSERT INTO sms_send_records (
  verification_id, mobile, scene, provider_code, send_status, failure_reason,
  request_ip, user_agent, created_at
) VALUES
  (@sms_active_id, '19900008001', 'login', 'dev_noop', 'accepted', '真实短信供应商 SDK 接入：该功能待完善', '10.88.0.11', 'PetLife/iOS Seed', DATE_SUB(@seed_now, INTERVAL 1 MINUTE)),
  (@sms_verified_id, '19900008002', 'login', 'dev_noop', 'accepted', '真实短信供应商 SDK 接入：该功能待完善', '10.88.0.12', 'PetLife/Android Seed', DATE_SUB(@seed_now, INTERVAL 15 MINUTE)),
  (@sms_locked_id, '19900008004', 'login', 'dev_noop', 'blocked', '测试手机号达到验证码尝试上限', '10.88.0.14', 'PetLife/iOS Seed', DATE_SUB(@seed_now, INTERVAL 20 MINUTE));

-- ====================================================================
-- Media, health records, reminders, daily logs and timeline.
-- Object storage cloud adapter and CDN: 该功能待完善.
-- ====================================================================

INSERT INTO media_assets (
  uploader_user_id, biz_type, media_type, file_name, object_key, bucket_name,
  cdn_url, content_type, file_size, width, height, duration_ms, file_hash,
  upload_status, review_status, completed_at, created_at, updated_at, deleted_at
) VALUES (
  @user_owner_id, 'health_report', 'image', '[TEST] 奶盖疫苗本.jpg',
  CONCAT(@seed_object_prefix, 'health/vaccine-book.jpg'), 'petlife-local-dev',
  'https://static.petlife.test/media/health/vaccine-book.jpg',
  'image/jpeg', 428120, 1440, 1080, NULL, SHA2('PLT-SEED-media-health', 256),
  'uploaded', 'approved', DATE_SUB(@seed_now, INTERVAL 30 DAY),
  DATE_SUB(@seed_now, INTERVAL 30 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  uploader_user_id = VALUES(uploader_user_id),
  biz_type = VALUES(biz_type),
  media_type = VALUES(media_type),
  file_name = VALUES(file_name),
  bucket_name = VALUES(bucket_name),
  cdn_url = VALUES(cdn_url),
  content_type = VALUES(content_type),
  file_size = VALUES(file_size),
  width = VALUES(width),
  height = VALUES(height),
  duration_ms = VALUES(duration_ms),
  file_hash = VALUES(file_hash),
  upload_status = VALUES(upload_status),
  review_status = VALUES(review_status),
  completed_at = VALUES(completed_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @media_health_id = LAST_INSERT_ID();

INSERT INTO media_assets (
  uploader_user_id, biz_type, media_type, file_name, object_key, bucket_name,
  cdn_url, content_type, file_size, width, height, duration_ms, file_hash,
  upload_status, review_status, completed_at, created_at, updated_at, deleted_at
) VALUES (
  @user_owner_id, 'daily_log', 'image', '[TEST] 奶盖窗边晒太阳.jpg',
  CONCAT(@seed_object_prefix, 'daily/cat-sunlight.jpg'), 'petlife-local-dev',
  'https://static.petlife.test/media/daily/cat-sunlight.jpg',
  'image/jpeg', 682310, 1600, 1200, NULL, SHA2('PLT-SEED-media-daily-image', 256),
  'uploaded', 'approved', DATE_SUB(@seed_now, INTERVAL 3 DAY),
  DATE_SUB(@seed_now, INTERVAL 3 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  uploader_user_id = VALUES(uploader_user_id),
  biz_type = VALUES(biz_type),
  media_type = VALUES(media_type),
  file_name = VALUES(file_name),
  cdn_url = VALUES(cdn_url),
  upload_status = VALUES(upload_status),
  review_status = VALUES(review_status),
  completed_at = VALUES(completed_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @media_daily_photo_id = LAST_INSERT_ID();

INSERT INTO media_assets (
  uploader_user_id, biz_type, media_type, file_name, object_key, bucket_name,
  cdn_url, content_type, file_size, width, height, duration_ms, file_hash,
  upload_status, review_status, completed_at, created_at, updated_at, deleted_at
) VALUES (
  @user_member_id, 'community', 'video', '[TEST] 小风飞盘训练.mp4',
  CONCAT(@seed_object_prefix, 'community/dog-frisbee.mp4'), 'petlife-local-dev',
  'https://static.petlife.test/media/community/dog-frisbee.mp4',
  'video/mp4', 5242880, 1920, 1080, 18000, SHA2('PLT-SEED-media-community-video', 256),
  'uploaded', 'pending_review', DATE_SUB(@seed_now, INTERVAL 2 DAY),
  DATE_SUB(@seed_now, INTERVAL 2 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  uploader_user_id = VALUES(uploader_user_id),
  biz_type = VALUES(biz_type),
  media_type = VALUES(media_type),
  file_name = VALUES(file_name),
  cdn_url = VALUES(cdn_url),
  upload_status = VALUES(upload_status),
  review_status = VALUES(review_status),
  completed_at = VALUES(completed_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @media_community_video_id = LAST_INSERT_ID();

INSERT INTO pet_health_records (
  pet_id, record_type, title, occurred_at, operator_user_id, hospital_name,
  doctor_name, severity_level, result_summary, attachments, notes,
  created_at, updated_at, deleted_at
) VALUES
  (@pet_cat_id, 'vaccine', '[TEST] 奶盖年度三联疫苗', DATE_SUB(@seed_now, INTERVAL 30 DAY), @user_owner_id, '上海宠爱动物医院', '陈医生', 'normal', '疫苗接种顺利，无明显不良反应。', JSON_ARRAY(@media_health_id), '接种后观察 30 分钟正常。', DATE_SUB(@seed_now, INTERVAL 30 DAY), @seed_now, NULL),
  (@pet_dog_id, 'deworming', '[TEST] 小风体内外驱虫', DATE_SUB(@seed_now, INTERVAL 20 DAY), @user_member_id, '杭州安心宠物诊所', '刘医生', 'normal', '完成体内外驱虫，建议 30 天后复查。', JSON_ARRAY(), '驱虫后当天减少剧烈运动。', DATE_SUB(@seed_now, INTERVAL 20 DAY), @seed_now, NULL),
  (@pet_cat_id, 'checkup', '[TEST] 奶盖年度体检', DATE_SUB(@seed_now, INTERVAL 10 DAY), @user_owner_id, '上海宠爱动物医院', '王医生', 'attention', '体重略有上升，建议控制零食。', JSON_ARRAY(@media_health_id), '该记录用于健康档案和后台健康查询。', DATE_SUB(@seed_now, INTERVAL 10 DAY), @seed_now, NULL),
  (@pet_dog_id, 'medication', '[TEST] 小风关节营养补充', DATE_SUB(@seed_now, INTERVAL 7 DAY), @user_member_id, '杭州安心宠物诊所', '刘医生', 'attention', '补充关节营养品两周。', JSON_ARRAY(), '观察运动后恢复情况。', DATE_SUB(@seed_now, INTERVAL 7 DAY), @seed_now, NULL),
  (@pet_cat_id, 'observation', '[TEST] 奶盖眼部分泌物观察', DATE_SUB(@seed_now, INTERVAL 1 DAY), @user_owner_id, NULL, NULL, 'mild', '早晨分泌物略多，已清洁并持续观察。', JSON_ARRAY(), '如持续 3 天则预约复诊。', DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now, NULL);

SELECT id INTO @health_vaccine_id FROM pet_health_records WHERE title = '[TEST] 奶盖年度三联疫苗' ORDER BY id DESC LIMIT 1;
SELECT id INTO @health_deworming_id FROM pet_health_records WHERE title = '[TEST] 小风体内外驱虫' ORDER BY id DESC LIMIT 1;
SELECT id INTO @health_checkup_id FROM pet_health_records WHERE title = '[TEST] 奶盖年度体检' ORDER BY id DESC LIMIT 1;
SELECT id INTO @health_medication_id FROM pet_health_records WHERE title = '[TEST] 小风关节营养补充' ORDER BY id DESC LIMIT 1;
SELECT id INTO @health_observation_id FROM pet_health_records WHERE title = '[TEST] 奶盖眼部分泌物观察' ORDER BY id DESC LIMIT 1;

INSERT INTO reminder_templates (
  template_name, reminder_type, default_reminder_mode, default_advance_value,
  default_advance_unit, default_cycle_value, default_cycle_unit,
  applicable_pet_type, enabled, sort_order, created_at, updated_at, deleted_at
) VALUES
  ('[TEST] 年度疫苗提醒模板', 'vaccine', 'cycle', 7, 'day', 12, 'month', 'all', 1, 10, @seed_now, @seed_now, NULL),
  ('[TEST] 犬体内外驱虫模板', 'deworming', 'cycle', 3, 'day', 1, 'month', 'dog', 1, 20, @seed_now, @seed_now, NULL),
  ('[TEST] 猫年度体检模板', 'examination', 'single', 1, 'week', NULL, NULL, 'cat', 1, 30, @seed_now, @seed_now, NULL),
  ('[TEST] 停用模板-旧版护理', 'custom', 'single', 0, 'day', NULL, NULL, 'all', 0, 99, @seed_now, @seed_now, NULL);

INSERT INTO pet_reminders (
  pet_id, reminder_type, title, reminder_mode, cycle_value, cycle_unit,
  remind_at, status, source_record_id, handler_user_id, handled_at,
  created_at, updated_at, deleted_at
) VALUES
  (@pet_cat_id, 'vaccine', '[TEST] 奶盖下一针疫苗', 'cycle', 12, 'month', DATE_ADD(@seed_now, INTERVAL 11 MONTH), 'pending', @health_vaccine_id, NULL, NULL, DATE_SUB(@seed_now, INTERVAL 30 DAY), @seed_now, NULL),
  (@pet_dog_id, 'deworming', '[TEST] 小风下次驱虫', 'cycle', 1, 'month', DATE_ADD(@seed_now, INTERVAL 10 DAY), 'pending', @health_deworming_id, NULL, NULL, DATE_SUB(@seed_now, INTERVAL 20 DAY), @seed_now, NULL),
  (@pet_cat_id, 'checkup', '[TEST] 奶盖体重复查', 'single', NULL, NULL, DATE_ADD(@seed_now, INTERVAL 20 DAY), 'skipped', @health_checkup_id, @user_owner_id, DATE_SUB(@seed_now, INTERVAL 1 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), @seed_now, NULL),
  (@pet_dog_id, 'custom', '[TEST] 小风关节补充完成提醒', 'single', NULL, NULL, DATE_SUB(@seed_now, INTERVAL 1 DAY), 'done', @health_medication_id, @user_member_id, DATE_SUB(@seed_now, INTERVAL 1 DAY), DATE_SUB(@seed_now, INTERVAL 7 DAY), @seed_now, NULL);

INSERT INTO pet_daily_logs (
  pet_id, author_user_id, media_list, title, content, scene_tags, mood_tags,
  visibility, sync_to_community, sync_to_timeline, community_post_id,
  happened_at, created_at, updated_at, deleted_at
) VALUES (
  @pet_cat_id, @user_owner_id, JSON_ARRAY(@media_daily_photo_id),
  '[TEST] 奶盖窗边晒太阳',
  '上午精神状态很好，主动吃完冻干，午后在窗边晒太阳。',
  JSON_ARRAY('居家', '晒太阳', '饮食'),
  JSON_ARRAY('放松', '亲人'),
  'public', 1, 1, NULL,
  DATE_SUB(@seed_now, INTERVAL 3 DAY), DATE_SUB(@seed_now, INTERVAL 3 DAY), @seed_now, NULL
);
SET @daily_log_cat_id = LAST_INSERT_ID();

INSERT INTO pet_daily_logs (
  pet_id, author_user_id, media_list, title, content, scene_tags, mood_tags,
  visibility, sync_to_community, sync_to_timeline, community_post_id,
  happened_at, created_at, updated_at, deleted_at
) VALUES (
  @pet_dog_id, @user_member_id, JSON_ARRAY(@media_community_video_id),
  '[TEST] 小风飞盘训练',
  '傍晚做了 20 分钟飞盘训练，回家后喝水和拉伸。',
  JSON_ARRAY('训练', '户外', '运动'),
  JSON_ARRAY('兴奋', '专注'),
  'family', 0, 1, NULL,
  DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY), @seed_now, NULL
);
SET @daily_log_dog_id = LAST_INSERT_ID();

INSERT INTO pet_timeline_events (
  pet_id, event_type, source_type, source_id, event_time, title,
  summary, cover_url, visibility, created_at, updated_at
) VALUES
  (@pet_cat_id, 'health', 'health_record', @health_vaccine_id, DATE_SUB(@seed_now, INTERVAL 30 DAY), '[TEST] 奶盖完成年度三联疫苗', '健康记录派生成长时间轴事件。', 'https://static.petlife.test/media/health/vaccine-book.jpg', 'family', @seed_now, @seed_now),
  (@pet_cat_id, 'daily_log', 'daily_log', @daily_log_cat_id, DATE_SUB(@seed_now, INTERVAL 3 DAY), '[TEST] 奶盖窗边晒太阳', '日常记录派生成长时间轴事件。', 'https://static.petlife.test/media/daily/cat-sunlight.jpg', 'public', @seed_now, @seed_now),
  (@pet_dog_id, 'daily_log', 'daily_log', @daily_log_dog_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), '[TEST] 小风飞盘训练', '家庭可见训练记录。', 'https://static.petlife.test/media/community/dog-frisbee.mp4', 'family', @seed_now, @seed_now);

-- ====================================================================
-- Community and moderation.
-- Third-party content moderation integration: 该功能待完善.
-- ====================================================================

INSERT INTO community_topics (topic_name, topic_desc, city_code, status, created_at, updated_at)
VALUES
  ('[TEST] 上海同城养猫互助', '测试上海同城内容流与话题筛选。', 'PLT_SH', 1, @seed_now, @seed_now),
  ('[TEST] 训练与行为问答', '测试问答、评论、举报和治理链路。', NULL, 1, @seed_now, @seed_now);
SELECT id INTO @topic_city_cat_id FROM community_topics WHERE topic_name = '[TEST] 上海同城养猫互助' ORDER BY id DESC LIMIT 1;
SELECT id INTO @topic_training_id FROM community_topics WHERE topic_name = '[TEST] 训练与行为问答' ORDER BY id DESC LIMIT 1;

INSERT INTO community_posts (
  user_id, pet_id, topic_id, post_type, title, content, media_list,
  source_daily_log_id, source_service_id, source_product_id, city_code,
  visibility, review_status, published_at, like_count, comment_count,
  favorite_count, created_at, updated_at, deleted_at
) VALUES (
  @user_owner_id, @pet_cat_id, @topic_city_cat_id, 'image_text',
  '[TEST] 奶盖窗边晒太阳记录',
  '同步自萌宠日常，用于测试推荐、同城和后台内容查询。',
  JSON_ARRAY(@media_daily_photo_id),
  @daily_log_cat_id, NULL, NULL, 'PLT_SH',
  'public', 'approved', DATE_SUB(@seed_now, INTERVAL 2 DAY),
  1, 2, 1, DATE_SUB(@seed_now, INTERVAL 2 DAY), @seed_now, NULL
);
SET @post_daily_sync_id = LAST_INSERT_ID();

INSERT INTO community_posts (
  user_id, pet_id, topic_id, post_type, title, content, media_list,
  source_daily_log_id, source_service_id, source_product_id, city_code,
  visibility, review_status, published_at, like_count, comment_count,
  favorite_count, created_at, updated_at, deleted_at
) VALUES (
  @user_member_id, @pet_dog_id, @topic_training_id, 'qa',
  '[TEST] 飞盘训练后如何安排休息',
  '小风飞盘训练后会比较兴奋，想请教如何安排拉伸和休息。',
  JSON_ARRAY(@media_community_video_id),
  NULL, NULL, NULL, 'PLT_HZ',
  'public', 'pending_review', DATE_SUB(@seed_now, INTERVAL 1 DAY),
  0, 1, 0, DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now, NULL
);
SET @post_qa_id = LAST_INSERT_ID();

INSERT INTO community_posts (
  user_id, pet_id, topic_id, post_type, title, content, media_list,
  source_daily_log_id, source_service_id, source_product_id, city_code,
  visibility, review_status, published_at, like_count, comment_count,
  favorite_count, created_at, updated_at, deleted_at
) VALUES (
  @user_follower_id, @pet_cat_id, @topic_city_cat_id, 'image_text',
  '[TEST] 已拒绝的社区内容样本',
  '该帖子用于验证人工拒绝后不进入公开社区流。',
  JSON_ARRAY(@media_daily_photo_id),
  NULL, NULL, NULL, 'PLT_SH',
  'public', 'rejected', DATE_SUB(@seed_now, INTERVAL 10 HOUR),
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 10 HOUR), @seed_now, NULL
);
SET @post_rejected_id = LAST_INSERT_ID();

UPDATE pet_daily_logs
SET community_post_id = @post_daily_sync_id, updated_at = @seed_now
WHERE id = @daily_log_cat_id;

INSERT INTO community_comments (
  post_id, user_id, parent_comment_id, content, status, created_at, updated_at, deleted_at
) VALUES (
  @post_daily_sync_id, @user_member_id, NULL,
  '[TEST] 看起来状态很好，疫苗后精神也稳定。',
  'normal', DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now, NULL
);
SET @comment_root_id = LAST_INSERT_ID();

INSERT INTO community_comments (
  post_id, user_id, parent_comment_id, content, status, created_at, updated_at, deleted_at
) VALUES (
  @post_daily_sync_id, @user_owner_id, @comment_root_id,
  '[TEST] 谢谢，准备按模板继续做年度提醒。',
  'normal', DATE_SUB(@seed_now, INTERVAL 20 HOUR), @seed_now, NULL
);
SET @comment_reply_id = LAST_INSERT_ID();

INSERT INTO community_comments (
  post_id, user_id, parent_comment_id, content, status, created_at, updated_at, deleted_at
) VALUES (
  @post_qa_id, @user_follower_id, NULL,
  '[TEST] 训练结束后可以先慢走十分钟，再补水。',
  'normal', DATE_SUB(@seed_now, INTERVAL 12 HOUR), @seed_now, NULL
);

INSERT INTO community_post_reactions (post_id, user_id, reaction_type, created_at)
VALUES
  (@post_daily_sync_id, @user_member_id, 'like', DATE_SUB(@seed_now, INTERVAL 1 DAY)),
  (@post_qa_id, @user_owner_id, 'like', DATE_SUB(@seed_now, INTERVAL 8 HOUR));

INSERT INTO community_post_favorites (post_id, user_id, created_at)
VALUES
  (@post_daily_sync_id, @user_follower_id, DATE_SUB(@seed_now, INTERVAL 12 HOUR)),
  (@post_qa_id, @user_owner_id, DATE_SUB(@seed_now, INTERVAL 7 HOUR));

INSERT INTO user_follows (follower_user_id, followed_user_id, created_at)
VALUES
  (@user_follower_id, @user_owner_id, DATE_SUB(@seed_now, INTERVAL 6 DAY)),
  (@user_owner_id, @user_member_id, DATE_SUB(@seed_now, INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO community_reports (
  reporter_user_id, target_type, target_id, reason_code, reason_detail,
  status, processed_by, admin_notes, processed_at, created_at, updated_at
) VALUES (
  @user_follower_id, 'post', @post_qa_id, 'suspected_risk',
  '[TEST] 视频内容需要人工复核，第三方内容审核接入：该功能待完善',
  'pending', NULL, NULL, NULL, DATE_SUB(@seed_now, INTERVAL 6 HOUR), @seed_now
), (
  @user_owner_id, 'comment', @comment_root_id, 'spam',
  '[TEST] 已处理的评论举报样本',
  'processed', 'plt_ops_admin', '测试治理记录，确认无违规。', DATE_SUB(@seed_now, INTERVAL 3 HOUR),
  DATE_SUB(@seed_now, INTERVAL 5 HOUR), @seed_now
);

INSERT INTO moderation_tasks (
  target_type, target_id, content_type, content_snapshot, provider_code,
  review_status, review_result, risk_labels, failure_reason, callback_payload, reviewed_at,
  created_at, updated_at
) VALUES (
  'community_question', @post_qa_id, 'qa',
  JSON_OBJECT(
    'seed_code', 'PLT-SEED-MODERATION-VIDEO',
    'title', '[TEST] 飞盘训练后如何安排休息',
    'content', '小风飞盘训练后会比较兴奋，想请教如何安排拉伸和休息。',
    'media_asset_ids', JSON_ARRAY(@media_community_video_id),
    'notice', '第三方内容审核接入：该功能待完善'
  ),
  'dev_noop', 'pending',
  JSON_OBJECT('seed_code', 'PLT-SEED-MODERATION-VIDEO', 'source', 'dev_noop', 'notice', '等待人工审核'),
  JSON_ARRAY('manual_review_required'), NULL, JSON_OBJECT(), NULL,
  DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now
), (
  'community_post', @post_daily_sync_id, 'image_text',
  JSON_OBJECT(
    'seed_code', 'PLT-SEED-MODERATION-IMAGE',
    'title', '[TEST] 奶盖窗边晒太阳记录',
    'content', '同步自萌宠日常，用于测试推荐、同城和后台内容查询。',
    'media_asset_ids', JSON_ARRAY(@media_daily_photo_id),
    'notice', '人工通过样本，不代表第三方内容审核'
  ),
  'dev_noop', 'approved',
  JSON_OBJECT('seed_code', 'PLT-SEED-MODERATION-IMAGE', 'source', 'manual', 'action', 'approve', 'admin_notes', '测试数据人工通过'),
  JSON_ARRAY('normal_daily_log'), NULL, JSON_OBJECT('source', 'seed_manual'), DATE_SUB(@seed_now, INTERVAL 1 DAY),
  DATE_SUB(@seed_now, INTERVAL 2 DAY), @seed_now
), (
  'community_post', @post_rejected_id, 'image_text',
  JSON_OBJECT(
    'seed_code', 'PLT-SEED-MODERATION-REJECTED',
    'title', '[TEST] 已拒绝的社区内容样本',
    'content', '该帖子用于验证人工拒绝后不进入公开社区流。',
    'media_asset_ids', JSON_ARRAY(@media_daily_photo_id),
    'notice', '人工拒绝样本，不代表第三方内容审核'
  ),
  'dev_noop', 'rejected',
  JSON_OBJECT('seed_code', 'PLT-SEED-MODERATION-REJECTED', 'source', 'manual', 'action', 'reject', 'admin_notes', '测试数据人工拒绝'),
  JSON_ARRAY('manual_rejected'), NULL, JSON_OBJECT('source', 'seed_manual'), DATE_SUB(@seed_now, INTERVAL 9 HOUR),
  DATE_SUB(@seed_now, INTERVAL 10 HOUR), @seed_now
);

-- ====================================================================
-- Service center, slots, appointments, cancellation and reviews.
-- Map positioning, navigation and distance sorting: 该功能待完善.
-- ====================================================================

INSERT INTO service_city_configs (
  city_code, city_name, opened, unavailable_reason, sort_order,
  created_at, updated_at, deleted_at
) VALUES
  ('PLT_SH', '上海市', 1, NULL, 10, @seed_now, @seed_now, NULL),
  ('PLT_HZ', '杭州市', 1, NULL, 20, @seed_now, @seed_now, NULL),
  ('PLT_CD', '成都市', 0, '测试未开通城市，地图定位、导航、距离排序：该功能待完善', 30, @seed_now, @seed_now, NULL)
ON DUPLICATE KEY UPDATE
  city_name = VALUES(city_name),
  opened = VALUES(opened),
  unavailable_reason = VALUES(unavailable_reason),
  sort_order = VALUES(sort_order),
  deleted_at = NULL,
  updated_at = @seed_now;

INSERT INTO service_providers (
  provider_type, provider_name, city_code, address, latitude, longitude,
  contact_phone, business_hours, rating_avg, review_count, status,
  ext_json, created_at, updated_at, deleted_at
) VALUES (
  'hospital', '[TEST] 上海安心宠物医院', 'PLT_SH',
  '上海市浦东新区测试路 88 号', 31.230410, 121.473701,
  '021-19900088', '09:00-21:00', 4.80, 12, 'online',
  JSON_OBJECT('departments', JSON_ARRAY('疫苗', '体检', '影像'), 'map_notice', '地图定位、导航、距离排序：该功能待完善'),
  DATE_SUB(@seed_now, INTERVAL 60 DAY), @seed_now, NULL
);
SET @provider_hospital_id = LAST_INSERT_ID();

INSERT INTO service_providers (
  provider_type, provider_name, city_code, address, latitude, longitude,
  contact_phone, business_hours, rating_avg, review_count, status,
  ext_json, created_at, updated_at, deleted_at
) VALUES (
  'grooming', '[TEST] 杭州毛孩子洗护中心', 'PLT_HZ',
  '杭州市西湖区测试街 19 号', 30.274085, 120.155070,
  '0571-19900088', '10:00-20:00', 4.60, 8, 'online',
  JSON_OBJECT('service_level', 'premium', 'map_notice', '地图定位、导航、距离排序：该功能待完善'),
  DATE_SUB(@seed_now, INTERVAL 50 DAY), @seed_now, NULL
);
SET @provider_grooming_id = LAST_INSERT_ID();

INSERT INTO provider_service_items (
  provider_id, service_code, service_name, service_desc, price_min,
  price_max, status, created_at, updated_at
) VALUES
  (@provider_hospital_id, 'vaccine', '[TEST] 疫苗接种', '含基础问诊、疫苗接种和接种后观察。', 128.00, 298.00, 'active', @seed_now, @seed_now),
  (@provider_hospital_id, 'checkup', '[TEST] 年度体检', '基础血常规、生化和口腔检查。', 399.00, 899.00, 'active', @seed_now, @seed_now),
  (@provider_grooming_id, 'grooming', '[TEST] 精细洗护', '适合长毛犬猫的洗护、梳毛和基础护理。', 168.00, 368.00, 'active', @seed_now, @seed_now)
ON DUPLICATE KEY UPDATE
  service_name = VALUES(service_name),
  service_desc = VALUES(service_desc),
  price_min = VALUES(price_min),
  price_max = VALUES(price_max),
  status = VALUES(status),
  updated_at = @seed_now;

INSERT INTO provider_schedule_slots (
  provider_id, appointment_type, slot_date, start_time, end_time,
  quota, booked_count, status, created_at, updated_at
) VALUES
  (@provider_hospital_id, 'hospital', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '09:00:00', '10:00:00', 4, 1, 'open', @seed_now, @seed_now),
  (@provider_hospital_id, 'hospital', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '10:00:00', '11:00:00', 4, 4, 'full', @seed_now, @seed_now),
  (@provider_grooming_id, 'grooming', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), '14:00:00', '15:30:00', 3, 1, 'open', @seed_now, @seed_now)
ON DUPLICATE KEY UPDATE
  quota = VALUES(quota),
  booked_count = VALUES(booked_count),
  status = VALUES(status),
  updated_at = @seed_now;

INSERT INTO service_appointments (
  user_id, pet_id, provider_id, appointment_type, appointment_date,
  appointment_slot, demand_desc, contact_name, contact_mobile, status,
  remark, created_at, updated_at, deleted_at
) VALUES (
  @user_owner_id, @pet_cat_id, @provider_hospital_id, 'hospital',
  DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '09:00-10:00',
  '奶盖年度疫苗复核，携带疫苗本。',
  '林夏', '19900008001', 'pending_confirm',
  '用户端预约提交样本。', DATE_SUB(@seed_now, INTERVAL 6 HOUR), @seed_now, NULL
);
SET @appointment_pending_id = LAST_INSERT_ID();

INSERT INTO service_appointments (
  user_id, pet_id, provider_id, appointment_type, appointment_date,
  appointment_slot, demand_desc, contact_name, contact_mobile, status,
  remark, created_at, updated_at, deleted_at
) VALUES (
  @user_member_id, @pet_dog_id, @provider_grooming_id, 'grooming',
  DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), '14:00-15:30',
  '小风训练后精细洗护，注意青霉素过敏。',
  '周宁', '19900008002', 'completed',
  '已完成并评价。', DATE_SUB(@seed_now, INTERVAL 7 DAY), DATE_SUB(@seed_now, INTERVAL 5 DAY), NULL
);
SET @appointment_completed_id = LAST_INSERT_ID();

INSERT INTO service_appointments (
  user_id, pet_id, provider_id, appointment_type, appointment_date,
  appointment_slot, demand_desc, contact_name, contact_mobile, status,
  remark, created_at, updated_at, deleted_at
) VALUES (
  @user_owner_id, @pet_cat_id, @provider_hospital_id, 'hospital',
  DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), '10:00-11:00',
  '临时调整时间，测试取消预约链路。',
  '林夏', '19900008001', 'cancelled',
  '用户主动取消。', DATE_SUB(@seed_now, INTERVAL 4 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY), NULL
);
SET @appointment_cancelled_id = LAST_INSERT_ID();

INSERT INTO provider_reviews (
  provider_id, appointment_id, user_id, pet_id, rating, content, images,
  status, created_at, updated_at, deleted_at
) VALUES (
  @provider_grooming_id, @appointment_completed_id, @user_member_id, @pet_dog_id,
  5, '[TEST] 洗护过程很细致，小风回家后状态稳定。',
  JSON_ARRAY('https://static.petlife.test/review/xiaofeng-after-grooming.jpg'),
  'visible', DATE_SUB(@seed_now, INTERVAL 4 DAY), @seed_now, NULL
), (
  @provider_hospital_id, @appointment_cancelled_id, @user_owner_id, @pet_cat_id,
  3, '[TEST] 取消预约样本，不应作为已完成预约评价入口。',
  JSON_ARRAY(),
  'hidden', DATE_SUB(@seed_now, INTERVAL 2 DAY), @seed_now, NULL
);

INSERT INTO pet_timeline_events (
  pet_id, event_type, source_type, source_id, event_time, title,
  summary, cover_url, visibility, created_at, updated_at
) VALUES (
  @pet_dog_id, 'service', 'service_appointment', @appointment_completed_id,
  DATE_SUB(@seed_now, INTERVAL 5 DAY),
  '[TEST] 小风完成精细洗护',
  '服务预约完成后生成的成长时间轴样本。',
  NULL, 'family', @seed_now, @seed_now
);

-- ====================================================================
-- Commerce reserved data. Commerce module: 当前预留，该功能待完善.
-- ====================================================================

INSERT INTO products (
  product_name, category_code, pet_type, age_stage, brand_name, main_image,
  detail_images, status, description, created_at, updated_at, deleted_at
) VALUES (
  '[TEST][该功能待完善] 低敏猫粮 1.5kg', 'cat_food', 'cat', 'adult',
  'PetLife Seed', 'https://static.petlife.test/product/cat-food-main.jpg',
  JSON_ARRAY('https://static.petlife.test/product/cat-food-1.jpg'),
  'online',
  '商城模块：当前预留，该功能待完善。该数据仅用于表结构和后台展示联调。',
  @seed_now, @seed_now, NULL
);
SET @product_cat_food_id = LAST_INSERT_ID();

INSERT INTO product_skus (
  product_id, sku_code, sku_name, sale_price, market_price, stock_qty,
  weight_g, status, created_at, updated_at, deleted_at
) VALUES (
  @product_cat_food_id, 'PLT-SEED-CAT-FOOD-1500G', '[TEST] 1.5kg 标准装',
  129.00, 159.00, 88, 1500, 'active', @seed_now, @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  product_id = VALUES(product_id),
  sku_name = VALUES(sku_name),
  sale_price = VALUES(sale_price),
  market_price = VALUES(market_price),
  stock_qty = VALUES(stock_qty),
  weight_g = VALUES(weight_g),
  status = VALUES(status),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @sku_cat_food_id = LAST_INSERT_ID();

INSERT INTO user_addresses (
  user_id, receiver_name, receiver_mobile, province_code, city_code,
  district_code, detail_address, is_default, created_at, updated_at, deleted_at
) VALUES (
  @user_owner_id, '林夏', '19900008001', '310000', 'PLT_SH', '310115',
  '[TEST] 上海市浦东新区测试路 88 号 1201 室', 1, @seed_now, @seed_now, NULL
);
SET @address_owner_id = LAST_INSERT_ID();

INSERT INTO cart_items (
  user_id, pet_id, sku_id, quantity, checked, created_at, updated_at
) VALUES (
  @user_owner_id, @pet_cat_id, @sku_cat_food_id, 2, 1, @seed_now, @seed_now
);

INSERT INTO orders (
  user_id, pet_id, address_id, order_no, order_status, total_amount,
  pay_amount, receiver_name, receiver_mobile, receiver_address,
  buyer_remark, pay_at, created_at, updated_at, deleted_at
) VALUES (
  @user_owner_id, @pet_cat_id, @address_owner_id, 'PLT-SEED-ORDER-0001',
  'paid', 258.00, 248.00, '林夏', '19900008001',
  '[TEST] 上海市浦东新区测试路 88 号 1201 室',
  '商城模块：当前预留，该功能待完善。', DATE_SUB(@seed_now, INTERVAL 1 DAY),
  DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now, NULL
) ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  pet_id = VALUES(pet_id),
  address_id = VALUES(address_id),
  order_status = VALUES(order_status),
  total_amount = VALUES(total_amount),
  pay_amount = VALUES(pay_amount),
  receiver_name = VALUES(receiver_name),
  receiver_mobile = VALUES(receiver_mobile),
  receiver_address = VALUES(receiver_address),
  buyer_remark = VALUES(buyer_remark),
  pay_at = VALUES(pay_at),
  deleted_at = NULL,
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @order_id = LAST_INSERT_ID();

INSERT INTO order_items (
  order_id, product_id, sku_id, product_name, sku_name, sale_price,
  quantity, main_image, created_at, updated_at
) VALUES (
  @order_id, @product_cat_food_id, @sku_cat_food_id,
  '[TEST][该功能待完善] 低敏猫粮 1.5kg', '[TEST] 1.5kg 标准装',
  129.00, 2, 'https://static.petlife.test/product/cat-food-main.jpg',
  @seed_now, @seed_now
);

INSERT INTO order_status_logs (
  order_id, from_status, to_status, operator_user_id, operator_type,
  remark, created_at
) VALUES
  (@order_id, NULL, 'pending_pay', @user_owner_id, 'user', '商城模块：当前预留，该功能待完善。', DATE_SUB(@seed_now, INTERVAL 1 DAY)),
  (@order_id, 'pending_pay', 'paid', @user_owner_id, 'user', '测试支付状态流水。', DATE_SUB(@seed_now, INTERVAL 23 HOUR));

-- ====================================================================
-- Device reserved data. Device linkage module: 当前预留，该功能待完善.
-- ====================================================================

INSERT INTO devices (
  device_sn, device_type, brand_name, model_name, firmware_version,
  online_status, last_online_at, created_at, updated_at
) VALUES (
  'PLT-SEED-FEEDER-0001', 'feeder', '[TEST][该功能待完善] SeedFeeder',
  'F1', '0.9.0-seed', 'online', DATE_SUB(@seed_now, INTERVAL 10 MINUTE),
  DATE_SUB(@seed_now, INTERVAL 30 DAY), @seed_now
) ON DUPLICATE KEY UPDATE
  device_type = VALUES(device_type),
  brand_name = VALUES(brand_name),
  model_name = VALUES(model_name),
  firmware_version = VALUES(firmware_version),
  online_status = VALUES(online_status),
  last_online_at = VALUES(last_online_at),
  updated_at = @seed_now,
  id = LAST_INSERT_ID(id);
SET @device_feeder_id = LAST_INSERT_ID();

INSERT INTO device_bindings (
  device_id, user_id, pet_id, bind_name, room_name, bind_status,
  bound_at, unbound_at, created_at, updated_at
) VALUES (
  @device_feeder_id, @user_owner_id, @pet_cat_id,
  '[TEST] 奶盖自动喂食器', '客厅', 'active',
  DATE_SUB(@seed_now, INTERVAL 20 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 20 DAY), @seed_now
) ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  pet_id = VALUES(pet_id),
  bind_name = VALUES(bind_name),
  room_name = VALUES(room_name),
  bind_status = VALUES(bind_status),
  bound_at = VALUES(bound_at),
  unbound_at = VALUES(unbound_at),
  updated_at = @seed_now;

INSERT INTO device_snapshots (
  device_id, pet_id, snapshot_time, online_status, health_status,
  metrics_json, last_event_id, created_at, updated_at
) VALUES (
  @device_feeder_id, @pet_cat_id, DATE_SUB(@seed_now, INTERVAL 10 MINUTE),
  'online', 'normal',
  JSON_OBJECT('remaining_food_g', 420, 'last_feed_g', 35, 'notice', '设备联动模块：当前预留，该功能待完善'),
  NULL, @seed_now, @seed_now
) ON DUPLICATE KEY UPDATE
  pet_id = VALUES(pet_id),
  snapshot_time = VALUES(snapshot_time),
  online_status = VALUES(online_status),
  health_status = VALUES(health_status),
  metrics_json = VALUES(metrics_json),
  updated_at = @seed_now;

INSERT INTO device_events (
  device_id, pet_id, dedupe_key, event_code, event_type, event_time,
  event_value, severity_level, raw_payload, created_at
) VALUES (
  @device_feeder_id, @pet_cat_id, 'PLT-SEED-FEEDER-0001-20260519-01',
  'feed_completed', 'feed', DATE_SUB(@seed_now, INTERVAL 2 HOUR),
  '35g', 'normal',
  JSON_OBJECT('source', 'seed', 'notice', '设备联动模块：当前预留，该功能待完善'),
  @seed_now
) ON DUPLICATE KEY UPDATE
  device_id = VALUES(device_id),
  pet_id = VALUES(pet_id),
  event_code = VALUES(event_code),
  event_type = VALUES(event_type),
  event_time = VALUES(event_time),
  event_value = VALUES(event_value),
  severity_level = VALUES(severity_level),
  raw_payload = VALUES(raw_payload);
SET @device_event_id = LAST_INSERT_ID();

INSERT INTO pet_timeline_events (
  pet_id, event_type, source_type, source_id, event_time, title,
  summary, cover_url, visibility, created_at, updated_at
) VALUES (
  @pet_cat_id, 'device', 'device_event', @device_event_id,
  DATE_SUB(@seed_now, INTERVAL 2 HOUR),
  '[TEST][该功能待完善] 奶盖自动喂食器完成投喂',
  '设备联动模块：当前预留，该功能待完善。',
  NULL, 'private', @seed_now, @seed_now
);

-- ====================================================================
-- Notification, templates, channels, outbox and audit.
-- Push channel, alerting, outbox compensation and release audit hardening:
-- 该功能待完善.
-- ====================================================================

INSERT INTO message_templates (
  template_code, channel_type, title_template, content_template,
  status, created_at, updated_at
) VALUES
  ('PLT_SEED_REMINDER_DUE', 'inbox', '[TEST] {{petName}} 有一条照护提醒', '今天需要处理 {{reminderTitle}}。', 'active', @seed_now, @seed_now),
  ('PLT_SEED_SMS_LOGIN', 'sms', NULL, '验证码 {{code}}，真实短信供应商 SDK 接入：该功能待完善。', 'active', @seed_now, @seed_now),
  ('PLT_SEED_PUSH_APPOINTMENT', 'push', '[TEST] 预约状态更新', 'Push 推送通知通道：该功能待完善。', 'inactive', @seed_now, @seed_now)
ON DUPLICATE KEY UPDATE
  title_template = VALUES(title_template),
  content_template = VALUES(content_template),
  status = VALUES(status),
  updated_at = @seed_now;

INSERT INTO notification_channel_configs (
  channel_type, provider_code, provider_name, enabled, config_status,
  remark, created_at, updated_at, deleted_at
) VALUES
  ('inbox', 'plt_inbox', '[TEST] 站内信通道', 1, 'ready', '站内信已完成可测。', @seed_now, @seed_now, NULL),
  ('sms', 'plt_sms_noop', '[TEST] dev_noop 短信通道', 0, 'draft', '真实短信供应商 SDK 接入：该功能待完善。', @seed_now, @seed_now, NULL),
  ('push', 'plt_push_placeholder', '[TEST] Push 占位通道', 0, 'draft', 'Push 推送通知通道：该功能待完善。', @seed_now, @seed_now, NULL)
ON DUPLICATE KEY UPDATE
  provider_name = VALUES(provider_name),
  enabled = VALUES(enabled),
  config_status = VALUES(config_status),
  remark = VALUES(remark),
  deleted_at = NULL,
  updated_at = @seed_now;

INSERT INTO notifications (
  user_id, notify_type, biz_type, biz_id, title, content, read_status,
  sent_at, read_at, created_at, updated_at
) VALUES
  (@user_owner_id, 'reminder', 'pet_reminder', @health_vaccine_id, '[TEST] 奶盖下一针疫苗快到了', '建议提前 7 天确认医院和疫苗本。', 0, DATE_SUB(@seed_now, INTERVAL 30 MINUTE), NULL, DATE_SUB(@seed_now, INTERVAL 30 MINUTE), @seed_now),
  (@user_member_id, 'appointment', 'service_appointment', @appointment_completed_id, '[TEST] 小风洗护预约已完成', '可以为本次服务写一条评价。', 1, DATE_SUB(@seed_now, INTERVAL 4 DAY), DATE_SUB(@seed_now, INTERVAL 3 DAY), DATE_SUB(@seed_now, INTERVAL 4 DAY), @seed_now),
  (@user_owner_id, 'interaction', 'community_post', @post_daily_sync_id, '[TEST] 你的日常收到了新互动', '周宁点赞并评论了奶盖的日常。', 0, DATE_SUB(@seed_now, INTERVAL 12 HOUR), NULL, DATE_SUB(@seed_now, INTERVAL 12 HOUR), @seed_now);

SELECT id INTO @notification_reminder_id
FROM notifications
WHERE user_id = @user_owner_id AND title = '[TEST] 奶盖下一针疫苗快到了'
ORDER BY id DESC LIMIT 1;

INSERT INTO user_push_device_tokens (
  user_id, platform, provider_code, device_token, device_id, app_version,
  enabled, last_registered_at, unregistered_at, created_at, updated_at
) VALUES
  (@user_owner_id, 'ios', 'dev_noop', 'PLT-SEED-PUSH-IOS-OWNER-0001', 'ios-plt-seed-001', '0.0.1+1', 1, DATE_SUB(@seed_now, INTERVAL 1 HOUR), NULL, DATE_SUB(@seed_now, INTERVAL 1 HOUR), @seed_now),
  (@user_disabled_id, 'android', 'dev_noop', 'PLT-SEED-PUSH-ANDROID-SWITCH-OFF-0001', 'android-plt-seed-004', '0.0.1+1', 1, DATE_SUB(@seed_now, INTERVAL 2 HOUR), NULL, DATE_SUB(@seed_now, INTERVAL 2 HOUR), @seed_now)
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  platform = VALUES(platform),
  device_id = VALUES(device_id),
  app_version = VALUES(app_version),
  enabled = VALUES(enabled),
  last_registered_at = VALUES(last_registered_at),
  unregistered_at = NULL,
  updated_at = @seed_now;

SELECT id INTO @push_token_owner_id
FROM user_push_device_tokens
WHERE provider_code = 'dev_noop' AND device_token = 'PLT-SEED-PUSH-IOS-OWNER-0001'
ORDER BY id DESC LIMIT 1;

INSERT INTO push_tasks (
  user_id, notification_id, notify_type, biz_type, biz_id, title, content,
  provider_code, task_status, failure_reason, created_at, updated_at
) VALUES (
  @user_owner_id, @notification_reminder_id, 'reminder', 'pet_reminder', @health_vaccine_id,
  '[TEST] 奶盖下一针疫苗快到了',
  'dev_noop 仅记录 Push 底座任务，不代表真实系统 Push 已送达。',
  'dev_noop', 'pending', NULL, DATE_SUB(@seed_now, INTERVAL 29 MINUTE), @seed_now
);
SET @push_task_reminder_id = LAST_INSERT_ID();

INSERT INTO push_tasks (
  user_id, notification_id, notify_type, biz_type, biz_id, title, content,
  provider_code, task_status, failure_reason, created_at, updated_at
) VALUES (
  @user_disabled_id, NULL, 'system', 'moderation_report', @post_rejected_id,
  '[TEST] 通知开关关闭时跳过 Push',
  'notification_switch 关闭时只保留 skipped 排查记录，不生成可投递 Push。',
  'dev_noop', 'skipped', 'notification_switch_off', DATE_SUB(@seed_now, INTERVAL 9 HOUR), @seed_now
);

INSERT INTO push_delivery_records (
  push_task_id, device_token_id, user_id, provider_code, delivery_status,
  failure_reason, attempted_at, created_at
) VALUES (
  @push_task_reminder_id, @push_token_owner_id, @user_owner_id, 'dev_noop',
  'pending', NULL, NULL, DATE_SUB(@seed_now, INTERVAL 29 MINUTE)
);

INSERT INTO outbox_events (
  aggregate_type, aggregate_id, event_type, payload_json, status,
  retry_count, next_retry_at, published_at, last_error, created_at, updated_at
) VALUES
  ('notification', @user_owner_id, 'PLT_SEED_REMINDER_DUE', JSON_OBJECT('seed_code', 'PLT-SEED-OUTBOX-REMINDER', 'user_id', @user_owner_id), 'success', 0, NULL, DATE_SUB(@seed_now, INTERVAL 25 MINUTE), NULL, DATE_SUB(@seed_now, INTERVAL 30 MINUTE), @seed_now),
  ('device', @device_feeder_id, 'PLT_SEED_DEVICE_EVENT', JSON_OBJECT('seed_code', 'PLT-SEED-OUTBOX-DEVICE', 'device_id', @device_feeder_id), 'failed', 3, DATE_ADD(@seed_now, INTERVAL 10 MINUTE), NULL, '完整监控告警、outbox 补偿、发布级审计加固：该功能待完善', DATE_SUB(@seed_now, INTERVAL 2 HOUR), @seed_now);

INSERT INTO audit_logs (
  operator_type, operator_id, target_type, target_id, action,
  detail_json, ip_address, user_agent, created_at
) VALUES
  ('admin', 'plt_ops_admin', 'reminder_template', 'PLT-SEED-TEMPLATE', 'upsert', JSON_OBJECT('seed_code', 'PLT-SEED-AUDIT-TEMPLATE', 'summary', '后台维护提醒模板'), '10.88.10.1', 'PetLife Admin Seed', DATE_SUB(@seed_now, INTERVAL 2 DAY)),
  ('admin', 'plt_ops_admin', 'community_report', CAST(@post_qa_id AS CHAR), 'process', JSON_OBJECT('seed_code', 'PLT-SEED-AUDIT-MODERATION', 'summary', '治理记录样本'), '10.88.10.1', 'PetLife Admin Seed', DATE_SUB(@seed_now, INTERVAL 3 HOUR)),
  ('system', 'plt_seed_job', 'outbox_event', 'PLT-SEED-OUTBOX-DEVICE', 'retry_failed', JSON_OBJECT('seed_code', 'PLT-SEED-AUDIT-OUTBOX', 'notice', '完整监控告警、outbox 补偿、发布级审计加固：该功能待完善'), '127.0.0.1', 'PetLife Seed Job', DATE_SUB(@seed_now, INTERVAL 1 HOUR));

COMMIT;

SELECT
  'PetLife test seed completed' AS result,
  (SELECT COUNT(*) FROM users WHERE mobile LIKE CONCAT(@seed_mobile_prefix, '%')) AS seeded_users,
  (SELECT COUNT(*) FROM pets WHERE pet_name LIKE CONCAT(@seed_prefix, '%')) AS seeded_pets,
  (SELECT COUNT(*) FROM community_posts WHERE title LIKE CONCAT(@seed_prefix, '%')) AS seeded_posts,
  (SELECT COUNT(*) FROM service_appointments WHERE contact_mobile LIKE CONCAT(@seed_mobile_prefix, '%')) AS seeded_appointments,
  (SELECT COUNT(*) FROM notifications WHERE title LIKE CONCAT(@seed_prefix, '%')) AS seeded_notifications;
