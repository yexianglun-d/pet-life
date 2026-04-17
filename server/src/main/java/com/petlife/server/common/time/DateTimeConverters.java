package com.petlife.server.common.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 应用层时间转换工具。
 *
 * <p>当前 MySQL DDL 使用 DATETIME，不保存时区信息；接口层统一返回 OffsetDateTime。
 * 这里集中完成本地时区转换，避免各业务服务散落重复转换逻辑。</p>
 */
public final class DateTimeConverters {

    private static final ZoneId APPLICATION_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private DateTimeConverters() {
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(APPLICATION_ZONE_ID).toOffsetDateTime();
    }

    public static LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime, LocalDateTime fallback) {
        if (offsetDateTime == null) {
            return fallback;
        }
        return offsetDateTime.atZoneSameInstant(APPLICATION_ZONE_ID).toLocalDateTime();
    }
}
