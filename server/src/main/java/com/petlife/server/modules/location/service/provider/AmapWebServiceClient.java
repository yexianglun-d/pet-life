package com.petlife.server.modules.location.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.location.domain.entity.AmapDistanceEntity;
import com.petlife.server.modules.location.domain.entity.AmapGeocodeEntity;
import com.petlife.server.modules.location.domain.entity.AmapReverseGeocodeEntity;
import com.petlife.server.modules.location.domain.entity.GeoPointEntity;
import com.petlife.server.modules.location.service.AmapWebServiceProperties;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 高德 Web 服务 HTTP 适配器。
 */
@Component
public class AmapWebServiceClient {

    private static final String SUCCESS_STATUS = "1";
    private static final String DEFAULT_DISTANCE_TYPE = "1";

    private final AmapWebServiceProperties properties;
    private final RestClient restClient;

    public AmapWebServiceClient(
        AmapWebServiceProperties properties,
        @Qualifier("amapRestClient") RestClient restClient
    ) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public Optional<AmapGeocodeEntity> geocode(String address, String city) {
        requireConfigured();
        JsonNode response = executeGet("/v3/geocode/geo", List.of(
            new QueryParam("address", address),
            new QueryParam("city", city)
        ));
        ensureAmapSuccess(response);
        JsonNode geocodes = response.path("geocodes");
        if (!geocodes.isArray() || geocodes.isEmpty()) {
            return Optional.empty();
        }
        JsonNode first = geocodes.get(0);
        GeoPointEntity point = parseLocation(first.path("location").asText(null));
        if (point == null) {
            return Optional.empty();
        }
        return Optional.of(new AmapGeocodeEntity(
            nullableText(first.path("formatted_address")),
            nullableText(first.path("country")),
            nullableText(first.path("province")),
            nullableText(first.path("city")),
            nullableText(first.path("district")),
            nullableText(first.path("citycode")),
            nullableText(first.path("adcode")),
            point,
            nullableText(first.path("level"))
        ));
    }

    public Optional<AmapReverseGeocodeEntity> reverseGeocode(GeoPointEntity point) {
        requireConfigured();
        JsonNode response = executeGet("/v3/geocode/regeo", List.of(
            new QueryParam("location", toAmapLocation(point)),
            new QueryParam("extensions", "base")
        ));
        ensureAmapSuccess(response);
        JsonNode regeocode = response.path("regeocode");
        if (regeocode.isMissingNode() || regeocode.isEmpty()) {
            return Optional.empty();
        }
        JsonNode addressComponent = regeocode.path("addressComponent");
        return Optional.of(new AmapReverseGeocodeEntity(
            nullableText(regeocode.path("formatted_address")),
            nullableText(addressComponent.path("country")),
            nullableText(addressComponent.path("province")),
            nullableText(addressComponent.path("city")),
            nullableText(addressComponent.path("district")),
            nullableText(addressComponent.path("township")),
            nullableText(addressComponent.path("citycode")),
            nullableText(addressComponent.path("adcode")),
            point
        ));
    }

    public List<AmapDistanceEntity> calculateWalkingDistances(
        List<GeoPointEntity> origins,
        GeoPointEntity destination
    ) {
        requireConfigured();
        if (origins == null || origins.isEmpty()) {
            return List.of();
        }
        JsonNode response = executeGet("/v3/distance", List.of(
            new QueryParam("origins", origins.stream().map(this::toAmapLocation).reduce((left, right) -> left + "|" + right).orElse("")),
            new QueryParam("destination", toAmapLocation(destination)),
            new QueryParam("type", DEFAULT_DISTANCE_TYPE)
        ));
        ensureAmapSuccess(response);
        JsonNode results = response.path("results");
        if (!results.isArray()) {
            return List.of();
        }
        List<AmapDistanceEntity> distances = new ArrayList<>();
        for (JsonNode item : results) {
            distances.add(new AmapDistanceEntity(
                parseInteger(item.path("origin_id").asText(null), 1) - 1,
                parseInteger(item.path("distance").asText(null), null),
                parseInteger(item.path("duration").asText(null), null),
                nullableText(item.path("info"))
            ));
        }
        return distances;
    }

    private JsonNode executeGet(String path, List<QueryParam> queryParams) {
        try {
            return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(path).queryParam("key", properties.getKey());
                    for (QueryParam queryParam : queryParams) {
                        if (queryParam.value() != null && !queryParam.value().isBlank()) {
                            builder.queryParam(queryParam.name(), queryParam.value());
                        }
                    }
                    return builder.build();
                })
                .retrieve()
                .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new BusinessException(ResponseCode.MAP_PROVIDER_REQUEST_FAILED, "高德地图服务请求失败");
        }
    }

    private void ensureAmapSuccess(JsonNode response) {
        if (response == null || !SUCCESS_STATUS.equals(response.path("status").asText())) {
            String info = response == null ? null : nullableText(response.path("info"));
            String infoCode = response == null ? null : nullableText(response.path("infocode"));
            String message = info == null ? "高德地图服务返回异常" : "高德地图服务返回异常：" + info;
            if (infoCode != null) {
                message = message + " (" + infoCode + ")";
            }
            throw new BusinessException(
                ResponseCode.MAP_PROVIDER_REQUEST_FAILED,
                message
            );
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new BusinessException(ResponseCode.MAP_CONFIGURATION_MISSING, "高德 Web 服务 Key 未配置");
        }
    }

    private GeoPointEntity parseLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new GeoPointEntity(new BigDecimal(parts[1]), new BigDecimal(parts[0]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String toAmapLocation(GeoPointEntity point) {
        return point.longitude().stripTrailingZeros().toPlainString()
            + ","
            + point.latitude().stripTrailingZeros().toPlainString();
    }

    private String nullableText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text;
        if (node.isArray()) {
            Iterator<JsonNode> elements = node.elements();
            text = elements.hasNext() ? elements.next().asText() : null;
        } else {
            text = node.asText();
        }
        return text == null || text.isBlank() || "[]".equals(text) ? null : text;
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private record QueryParam(String name, String value) {
    }
}
