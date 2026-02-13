package com.navigation.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class MapUtils {
    public static class RouteResult {
        private String rawContent;
        private Map<String, String> parsedData;

        public RouteResult(String rawContent, Map<String, String> parsedData) {
            this.rawContent = rawContent;
            this.parsedData = parsedData;
        }


        public String getRawContent() { return rawContent; }
        public void setRawContent(String rawContent) { this.rawContent = rawContent; }
        public Map<String, String> getParsedData() { return new HashMap<>(parsedData); }
        public void setParsedData(Map<String, String> parsedData) { this.parsedData = parsedData; }
    }

    private static final Logger logger = LoggerFactory.getLogger(MapUtils.class);
    private static final String API_KEY = "2c5617149f45866b04506b7b61a8bda7";


        public static final String DRIVING_ROUTE_PLANNING = "https://restapi.amap.com/v5/direction/driving";
        public static final String WALKING_ROUTE_PLANNING = "https://restapi.amap.com/v5/direction/walking";
        public static final String CYCLING_ROUTE_PLANNING = "https://restapi.amap.com/v5/direction/bicycling";
        public static final String BUS_ROUTE_PLANNING = "https://restapi.amap.com/v5/direction/transit/integrated";


            public static RouteResult sendGet(String url, Map<String, String> params) {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    URI uri = getBuilder(url, params);
                    HttpGet httpGet = new HttpGet(uri);

                    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                        String rawResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
                        Map<String, String> parsedData = parseRoute(rawResponse);
                        return new RouteResult(rawResponse, parsedData);
                    }
                } catch (Exception e) {
                    logger.error("请求高德 API 失败: {}", e.getMessage(), e);
                    return new RouteResult("{\"error\": \"" + e.getMessage() + "\"}", new HashMap<>());
                }
            }

            private static URI getBuilder(String url, Map<String, String> params) throws Exception {
                URIBuilder uriBuilder = new URIBuilder(url);
                uriBuilder.setParameter("key", API_KEY);
                uriBuilder.setParameter("origin", params.get("origin"));
                uriBuilder.setParameter("destination", params.get("destination"));
                uriBuilder.setParameter("show_fields", "cost");
                // 打印出请求的 URL，检查参数是否正确
                System.out.println("请求的 URL: " + uriBuilder.toString());
                // 只在步行路径中，去掉 city1 和 city2 参数
                if (!"walking".equals(url)) {
                    // 对于非步行路线，如果提供了 city1 和 city2 参数，添加到 URL 中
                    if (params.containsKey("city1")) {
                        uriBuilder.setParameter("city1", params.get("city1"));
                    }
                    if (params.containsKey("city2")) {
                        uriBuilder.setParameter("city2", params.get("city2"));
                    }
                }

                // 如果是步行路线并且需要 indoor 参数，添加该参数
                if ("walking".equals(url) && params.containsKey("isindoor")) {

                    uriBuilder.setParameter("isindoor", params.get("isindoor"));
                }

                return uriBuilder.build();
            }



                private static Map<String, String> parseRoute(String jsonContent) {
                    Map<String, String> result = new HashMap<>();
                    JSONObject jsonObject = JSON.parseObject(jsonContent);

                        // 检查status字段
                        int status = jsonObject.getInteger("status");
                        if (status != 1) {
                            // 记录错误信息
                            String errorInfo = jsonObject.getString("info");
                            logger.error("API 请求失败, 状态码: {}, 错误信息: {}", status, errorInfo);
                            result.put("error", "API 请求失败: " + errorInfo);
                            return result;
                        }

                            // 解析路径数据
                            JSONObject route = jsonObject.getJSONObject("route");
                            if (route != null) {
                                JSONArray paths = route.getJSONArray("paths");
                                if (paths != null && !paths.isEmpty()) {
                                    JSONObject path = paths.getJSONObject(0);
                                    result.put("distance", path.getString("distance"));
                                    result.put("duration", path.getString("duration"));
                                } else {
                                    // 如果没有路径，可能是公交或者骑行路线的情况
                                    if (route.containsKey("transits")) {
                                        JSONArray transits = route.getJSONArray("transits");
                                        if (transits != null && !transits.isEmpty()) {
                                            JSONObject transit = transits.getJSONObject(0);
                                            result.put("distance", transit.getString("distance"));
                                            result.put("duration", transit.getString("duration"));
                                        }
                                    }
                                }
                            }


                            return result;
                        }
                    }
