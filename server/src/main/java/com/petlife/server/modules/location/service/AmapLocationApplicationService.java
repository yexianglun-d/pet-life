package com.petlife.server.modules.location.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.location.converter.AmapLocationConverter;
import com.petlife.server.modules.location.domain.entity.AmapConfigStatusEntity;
import com.petlife.server.modules.location.domain.entity.GeoPointEntity;
import com.petlife.server.modules.location.dto.request.AdminGeocodeRequest;
import com.petlife.server.modules.location.dto.response.AmapConfigStatusResponse;
import com.petlife.server.modules.location.dto.response.AmapGeocodeResponse;
import com.petlife.server.modules.location.dto.response.AmapReverseGeocodeResponse;
import com.petlife.server.modules.location.service.provider.AmapWebServiceClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 高德地图应用服务。
 */
@Service
public class AmapLocationApplicationService {

    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final double EARTH_RADIUS_METERS = 6371008.8D;

    private final AmapWebServiceProperties properties;
    private final AmapWebServiceClient amapWebServiceClient;
    private final AmapLocationConverter amapLocationConverter;

    public AmapLocationApplicationService(
        AmapWebServiceProperties properties,
        AmapWebServiceClient amapWebServiceClient,
        AmapLocationConverter amapLocationConverter
    ) {
        this.properties = properties;
        this.amapWebServiceClient = amapWebServiceClient;
        this.amapLocationConverter = amapLocationConverter;
    }

    public AmapConfigStatusResponse getConfigStatus() {
        return amapLocationConverter.toConfigStatusResponse(new AmapConfigStatusEntity(
            "amap",
            properties.isConfigured(),
            properties.getBaseUrl(),
            List.of("geocode", "reverse_geocode", "distance"),
            properties.isConfigured() ? "高德 Web 服务配置已就绪" : "高德 Web 服务 Key 未配置"
        ));
    }

    public AmapGeocodeResponse geocode(AdminGeocodeRequest request) {
        String address = normalizeRequiredText(request.address(), "地址不能为空");
        String city = normalizeNullableText(request.city());
        return amapLocationConverter.toGeocodeResponse(
            address,
            amapWebServiceClient.geocode(address, city).orElse(null)
        );
    }

    public AmapReverseGeocodeResponse reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        GeoPointEntity point = normalizePoint(latitude, longitude);
        return amapLocationConverter.toReverseGeocodeResponse(
            amapWebServiceClient.reverseGeocode(point).orElse(null)
        );
    }

    public GeoPointEntity normalizePoint(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "经纬度不能为空");
        }
        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "纬度必须在 -90 到 90 之间");
        }
        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "经度必须在 -180 到 180 之间");
        }
        return new GeoPointEntity(
            latitude.setScale(6, RoundingMode.HALF_UP),
            longitude.setScale(6, RoundingMode.HALF_UP)
        );
    }

    /**
     * 服务商列表需要稳定、低延迟地按距离排序，使用已维护的高德坐标做球面直线距离计算。
     * 外部路线距离能力保留在 Web 服务适配层，避免用户目录查询因供应商波动不可用。
     */
    public Integer calculateStraightLineDistanceMeters(GeoPointEntity origin, GeoPointEntity destination) {
        if (origin == null || destination == null) {
            return null;
        }
        double originLatitude = Math.toRadians(origin.latitude().doubleValue());
        double destinationLatitude = Math.toRadians(destination.latitude().doubleValue());
        double latitudeDelta = Math.toRadians(destination.latitude().subtract(origin.latitude()).doubleValue());
        double longitudeDelta = Math.toRadians(destination.longitude().subtract(origin.longitude()).doubleValue());
        double halfChordLength = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
            + Math.cos(originLatitude) * Math.cos(destinationLatitude)
            * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        double angularDistance = 2 * Math.atan2(Math.sqrt(halfChordLength), Math.sqrt(1 - halfChordLength));
        return (int) Math.round(EARTH_RADIUS_METERS * angularDistance);
    }

    private String normalizeRequiredText(String text, String message) {
        String normalizedText = normalizeNullableText(text);
        if (normalizedText == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, message);
        }
        return normalizedText;
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String normalizedText = text.trim();
        return normalizedText.isEmpty() ? null : normalizedText;
    }
}
