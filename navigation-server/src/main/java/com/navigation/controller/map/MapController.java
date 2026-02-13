package com.navigation.controller.map;

import com.alibaba.fastjson.JSONObject;
import com.navigation.utils.MapUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
/**
 * 高德地图
 */
@RestController
@RequestMapping("/map")
@Api(tags = "Map Controller", description = "提供路线规划服务的接口")
public class MapController {


    /**
     * 日志对象
     */
    private static final Logger logger = LoggerFactory.getLogger(MapController.class);


    @ApiOperation(value = "公交路线规划", notes = "根据起点和终点获取公交路线规划")
    @GetMapping("/bus")
        public ResponseEntity<Map<String, Object>> getBus(
                @RequestParam String origin,
                @RequestParam String destination,
                @RequestParam(required = false) String city1,
                @RequestParam(required = false) String city2) {
            return getRoute(MapUtils.BUS_ROUTE_PLANNING, origin, destination, city1, city2, null);
        }


        @ApiOperation(value = "步行路线规划", notes = "根据起点和终点获取步行路线规划")
        @GetMapping("/walk")
            public ResponseEntity<Map<String, Object>> getWalk(
                    @RequestParam String origin,
                    @RequestParam String destination,
                    @RequestParam(required = false) String isindoor,
                    @RequestParam(required = false) String city1,
                    @RequestParam(required = false) String city2) {
                return getRoute(MapUtils.WALKING_ROUTE_PLANNING, origin, destination, city1, city2, isindoor);
            }


            @ApiOperation(value = "驾车路线规划", notes = "根据起点和终点获取驾车路线规划")
            @GetMapping("/drive")
                public ResponseEntity<Map<String, Object>> getDrive(
                        @RequestParam String origin,
                        @RequestParam String destination,
                        @RequestParam(required = false) String city1,
                        @RequestParam(required = false) String city2) {
                    return getRoute(MapUtils.DRIVING_ROUTE_PLANNING, origin, destination, city1, city2, null);
                }


                @ApiOperation(value = "骑行路线规划", notes = "根据起点和终点获取骑行路线规划")
                @GetMapping("/riding")
                    public ResponseEntity<Map<String, Object>> getRiding(
                            @RequestParam String origin,
                            @RequestParam String destination,
                            @RequestParam(required = false) String city1,
                            @RequestParam(required = false) String city2) {
                        return getRoute(MapUtils.CYCLING_ROUTE_PLANNING, origin, destination, city1, city2, null);
                    }

                    private ResponseEntity<Map<String, Object>> getRoute(String url, String origin, String destination, String city1, String city2, String isindoor) {
                        Map<String, String> params = new HashMap<>();

                        params.put("origin", origin);
                        params.put("destination", destination);

                        // 针对步行路线，去掉 city1 和 city2 参数
                        if (!"walking".equals(url)) {
                            if (city1 != null) {
                                params.put("city1", city1);
                            }
                            if (city2 != null) {
                                params.put("city2", city2);
                            }
                        }

                        if (isindoor != null) {
                            params.put("isindoor", isindoor);
                        }

                        MapUtils.RouteResult result = MapUtils.sendGet(url, params);
                        logger.info("返回结果: {}", JSONObject.toJSONString(result.getParsedData()));

                        // 解析 JSON 字符串为 Map
                        Map<String, Object> responseData = JSONObject.parseObject(result.getRawContent(), Map.class);
                        return ResponseEntity.ok(responseData);
                    }
                }