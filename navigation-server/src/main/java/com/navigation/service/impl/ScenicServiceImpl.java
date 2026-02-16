package com.navigation.service.impl;



import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.navigation.context.BaseContext;
import com.navigation.entity.Comment;
import com.navigation.entity.Scenic;
import com.navigation.entity.ScenicReservation;
import com.navigation.entity.User;
import com.navigation.mapper.ScenicMapper;
import com.navigation.mapper.ScenicReservationMapper;
import com.navigation.mapper.UserMapper;
import com.navigation.mapper.WeatherConditionsMapper;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.ScenicReservationService;
import com.navigation.service.ScenicService;
import com.navigation.service.UserService;
import com.navigation.service.WeatherService;
import com.navigation.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;


import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScenicServiceImpl extends ServiceImpl<ScenicMapper, Scenic> implements ScenicService {


    @Autowired
    private ScenicMapper scenicMapper;

    @Autowired
    private ScenicReservationMapper scenicReservationMapper;

    @Autowired
    private UserMapper  userMapper;


    @Autowired
    private Validator validator;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private WeatherConditionsMapper weatherConditionsMapper;

    @Override
    public Result<Void> saveScenic(Scenic scenic) {
        // 参数校验
        if (scenic == null) {
            log.error("传入的 Scenic 对象为空，无法保存景点信息");
            return Result.error("传入的景点信息为空");
        }

        List<String> missingFields = new ArrayList<>();
        // 假设Scenic类中有name、description等必填字段，这里以name为例
        if (scenic.getScenicName() == null || scenic.getScenicName().trim().isEmpty()) {
            missingFields.add("scenic_name");
        }
        if (scenic.getScenicDescription() == null || scenic.getScenicDescription().trim().isEmpty()) {
            missingFields.add("scenic_description");
        }
        if (scenic.getScenicLocateDescription() == null || scenic.getScenicLocateDescription().trim().isEmpty()) {
            missingFields.add("scenic_locate_description");
        }
        if (scenic.getScenicStar()== null || scenic.getScenicStar().toString().isEmpty()) {
            missingFields.add("scenic_star");
        }
        if (scenic.getRegionId() == null || scenic.getRegionId().toString().isEmpty()) {
            missingFields.add("region_id");
        }
        if (scenic.getMaxCapacity() == null || scenic.getMaxCapacity().toString().isEmpty()) {
            missingFields.add("max_capacity");
        }

        if (scenic.getScenicCover() == null || scenic.getScenicCover().trim().isEmpty()) {
            missingFields.add("scenic_cover");
        }
        if (scenic.getOpenEndTime() == null) {
            missingFields.add("open_end_time");
        }
        if (scenic.getOpenStartTime() == null) {
            missingFields.add("open_start_time");
        }
        // 可根据实际情况继续添加对其他必填字段的检查，如description等
        if (!missingFields.isEmpty()) {
            String fieldsString = String.join(", ", missingFields);
            return Result.error("以下必填参数未传入: " + fieldsString);
        }

        String keyPattern2 = "region:" + scenic.getRegionId() + ":*";
        Set<String> keys2 = stringRedisTemplate.keys(keyPattern2);

        if (keys2 == null || keys2.isEmpty()) {
            log.error("未找到地区id为{}的记录", scenic.getId());
            return Result.error("未找到对应地区记录");
        }


        try {

            scenic.setScenicStatus(1);
            scenic.setCreateTime(LocalDateTime.now());
            scenic.setUpdateTime(LocalDateTime.now());

            // 获取自增后的scenicId并设置到Scenic对象中
            long newScenicId = getIncrementedScenicId();
            scenic.setId((int) newScenicId);
            // 构建Redis的key
            String key = "scenic:" + scenic.getId() + ":" + scenic.getScenicName();
            // 将Scenic对象转换为JSON字符串
            String scenicJson = JsonUtils.toJson(scenic);
            stringRedisTemplate.opsForValue().set(key, scenicJson);
            return Result.success();
        } catch (Exception e) {
            log.error("保存景点信息时出现异常", e);
            return Result.error("保存景点信息失败，请稍后重试");
        }
    }

    // 假设获取自增ScenicId的方法，需根据实际业务逻辑实现，这里仅为示例
    private long getIncrementedScenicId() {
        // 这里可以是从数据库序列、Redis自增键等方式获取自增ID
        // 示例从Redis获取自增键值
        String idKey = "scenic_id_increment";
        return stringRedisTemplate.opsForValue().increment(idKey);
    }

   /* public Result<Void> saveScenic(Scenic scenic) {
        // 参数校验
        if (scenic == null) {
            log.error("传入的 Scenic 对象为空，无法保存景点信息");
            return Result.error("传入的景点信息为空");
        }

        // 使用Validator校验Scenic对象的其他必填字段
        Set<ConstraintViolation<Scenic>> violations = validator.validate(scenic);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("以下必填参数未传入: ");
            for (ConstraintViolation<Scenic> violation : violations) {
                errorMessage.append(violation.getMessage()).append("; ");
            }
            log.error(errorMessage.toString());
            return Result.error(errorMessage.toString());
        }

        try {
            scenic.setScenicStatus(1);
            scenic.setCreateTime(LocalDateTime.now());
            scenic.setUpdateTime(LocalDateTime.now());
            scenicMapper.saveScenic(scenic);
            return Result.success();
        } catch (Exception e) {
            log.error("保存景点信息时出现异常", e);
            // 这里如果开启了事务，异常会触发事务回滚
            return Result.error("保存景点信息失败，请稍后重试");
        }
    }*/

    @Override
    public Result<Void> update(Scenic scenic) {
        if (scenic == null || scenic.getId() == null) {
            log.error("传入的Scenic对象为空或缺少ID，无法更新景点信息");
            return Result.error("传入的景点信息为空或缺少ID");
        }

        try {
            // 构建基础key模式
            String keyPattern = "scenic:" + scenic.getId() + ":*";
            Set<String> keys = stringRedisTemplate.keys(keyPattern);

            if (keys == null || keys.isEmpty()) {
                log.error("未找到景点id为{}的记录", scenic.getId());
                return Result.error("未找到对应景点记录");
            }

            String keyPattern2 = "region:" + scenic.getRegionId() + ":*";
            Set<String> keys2 = stringRedisTemplate.keys(keyPattern2);

            if (keys2 == null || keys2.isEmpty()) {
                log.error("未找到地区id为{}的记录", scenic.getId());
                return Result.error("未找到对应地区记录");
            }

            // 获取第一个匹配的key（假设每个景点只有一个主key）
            String oldKey = keys.iterator().next();
            String scenicJson = stringRedisTemplate.opsForValue().get(oldKey);

            if (scenicJson == null) {
                log.error("景点id为{}的记录数据为空", scenic.getId());
                return Result.error("景点记录数据为空");
            }

            Scenic originalScenic = JsonUtils.fromJson(scenicJson, Scenic.class);
            if (originalScenic == null) {
                log.error("反序列化景点id为{}的记录失败", scenic.getId());
                return Result.error("景点数据解析失败");
            }

            // 记录是否需要更新键名（当景点名称变更时）
            boolean needUpdateKey = false;
            String originalName = originalScenic.getScenicName();


            // 调用天气接口获取景点开放状态（0 不开放，1 开放）
            /*int parkStatus = weatherConditionsMapper.getStatusByWeatherCondition()
            originalScenic.setScenicStatus(parkStatus);*/


            // 更新各字段（仅更新非空字段）
            if (scenic.getScenicName() != null && !scenic.getScenicName().equals(originalName)) {
                originalScenic.setScenicName(scenic.getScenicName());
                needUpdateKey = true;
            }
            if (scenic.getScenicCover() != null) {
                originalScenic.setScenicCover(scenic.getScenicCover());
            }
            if(scenic.getRegionId() != null){
                originalScenic.setRegionId(scenic.getRegionId());
            }
            if (scenic.getScenicDescription() != null) {
                originalScenic.setScenicDescription(scenic.getScenicDescription());
            }
            if (scenic.getScenicLocateDescription() != null) {
                originalScenic.setScenicLocateDescription(scenic.getScenicLocateDescription());
            }
            if (scenic.getMaxCapacity() != null) {
                originalScenic.setMaxCapacity(scenic.getMaxCapacity());
            }
            if (scenic.getOpenStartTime() != null) {
                originalScenic.setOpenStartTime(scenic.getOpenStartTime());
            }
            if (scenic.getOpenEndTime() != null) {
                originalScenic.setOpenEndTime(scenic.getOpenEndTime());
            }
            if (scenic.getScenicStatus() != null) {
                originalScenic.setScenicStatus(scenic.getScenicStatus());
            }
            originalScenic.setUpdateTime(LocalDateTime.now());

            // 获取该景点所有预约记录的总人数
            Integer totalPeopleCount = scenicReservationMapper.getTotalPeopleCountByScenicId(scenic.getId());
            originalScenic.setTotalPeople(totalPeopleCount);
            // 将更新后的对象转换为JSON
            String updatedScenicJson = JsonUtils.toJson(originalScenic);

            if (needUpdateKey) {
                // 如果景点名称变更，需要创建新key并删除旧key
                String newKey = "scenic:" + originalScenic.getId() + ":" + originalScenic.getScenicName();
                stringRedisTemplate.opsForValue().set(newKey, updatedScenicJson);
                stringRedisTemplate.delete(oldKey);
            } else {
                // 如果景点名称未变更，直接更新原key的值
                stringRedisTemplate.opsForValue().set(oldKey, updatedScenicJson);
            }

            return Result.success();
        } catch (Exception e) {
            log.error("更新景点id为{}的信息时出现异常: {}", scenic.getId(), e.getMessage(), e);
            return Result.error("更新景点信息时出现异常: " + e.getMessage());
        }
    }

    /*public Result<Void> update(Scenic scenic) {
        if (scenic == null) {
            log.error("传入的 Scenic 对象为空，无法更新景点信息");
            return Result.error("传入的景点信息为空");
        }

        try {
            // 检查scenicId是否存在
            Integer scenicId = scenic.getId();
            int count = scenicMapper.countScenicById(scenicId);
            if (count == 0) {
                log.error("景区id为 {} 的景区记录不存在", scenicId);
                return Result.error("景区id为 " + scenicId + " 的景区记录不存在");
            }

            scenic.setUpdateTime(LocalDateTime.now());
            scenicMapper.update(scenic);
            return Result.success();
        } catch (Exception e) {
            log.error("更新景点信息时出现异常", e);
            throw new RuntimeException(e);
        }
    }*/

    @Override
    @Transactional  // 添加事务管理（确保批量删除原子性）
    public Result<Void> batchDelete(List<Integer> ids) {
        // 检查传入的ID列表是否为空
        if (ids == null || ids.isEmpty()) {
            return Result.error("删除的ID列表不能为空");
        }

        try {
            // 用于存储不存在的ID
            List<Integer> nonExistingIds = new ArrayList<>();

            // 检查传入的每个ID是否存在于Redis中
            for (Integer id : ids) {
                // 构建匹配键的模式
                String pattern = "scenic:" + id + ":*";
                Set<String> keys = stringRedisTemplate.keys(pattern);
                if (keys.isEmpty()) {
                    nonExistingIds.add(id);
                }
            }

            // 如果有不存在的ID，返回包含所有不存在ID的错误信息
            if (!nonExistingIds.isEmpty()) {
                String idsString = nonExistingIds.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                return Result.error("ID为 " + idsString + " 的景点记录不存在，删除失败");
            }

            // 执行批量删除操作
            for (Integer id : ids) {
                String pattern = "scenic:" + id + ":*";
                Set<String> keys = stringRedisTemplate.keys(pattern);
                for (String key : keys) {
                    stringRedisTemplate.delete(key);
                }
            }
            return Result.success();
        } catch (Exception e) {
            // 记录详细的异常日志，实际应用中建议使用日志框架，如Logback或Log4j
            log.error("批量删除景点过程中出现异常", e);
            // 返回包含具体异常信息的错误结果
            return Result.error("删除过程中出现异常: " + e.getMessage());
        }
    }

    @Override
    // 定义方法，接收页码(pageNum)和每页条数(pageSize)，返回分页结果PageResult
    public PageResult queryScenic(Integer pageNum, Integer pageSize) {
        // 定义Redis键的匹配模式：以"scenic:"为前缀的所有键
        String pattern = "scenic:*:*";

        // 构建扫描选项：设置匹配模式和每次扫描的最大计数
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)  // 匹配符合pattern的键
                .count(pageSize) // 每次扫描最多返回pageSize条结果（非精确值）
                .build();        // 构建ScanOptions对象

        // 用于存储当前页的景点数据
        List<Scenic> scenicList = new ArrayList<>();
        // 用于记录符合条件的总记录数
        long total = 0;

        // 使用try-with-resources语法获取Redis扫描的游标，自动关闭资源
        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            // 用于记录当前遍历到的位置索引
            int index = 0;
            // 计算当前页的起始索引（分页逻辑：第N页从(N-1)*pageSize开始）
            int startIndex = (pageNum - 1) * pageSize;
            // 计算当前页的结束索引（不包含）
            int endIndex = startIndex + pageSize;

            // 遍历游标中的所有键
            while (cursor.hasNext()) {
                // 获取下一个符合条件的Redis键
                String key = cursor.next();
                // 总记录数+1（每找到一个键，代表一条数据）
                total++;

                // 判断当前记录是否在当前页的范围内
                if (index >= startIndex && index < endIndex) {
                    // 根据键从Redis中获取对应的JSON字符串值
                    String scenicJson = stringRedisTemplate.opsForValue().get(key);
                    // 如果JSON字符串不为空
                    if (scenicJson != null) {
                        // 将JSON字符串转换为Scenic对象
                        Scenic scenic = JsonUtils.fromJson(scenicJson, Scenic.class);
                        // 添加到当前页的结果列表中
                        scenicList.add(scenic);
                    }
                }
                // 索引自增，记录下一个位置
                index++;
            }
        } catch (Exception e) {
            // 捕获异常并记录错误日志
            log.error("扫描Redis键时出错: {}", e.getMessage());
        }

        // 对当前页的景点列表按id升序排序
        scenicList.sort((s1, s2) -> s1.getId().compareTo(s2.getId()));

        // 封装总记录数和当前页数据，返回分页结果
        return new PageResult(total, scenicList);
    }

    @Override
    public PageResult queryScenic2(Integer page, Integer pageSize) {
        // 1. 参数校验和默认值设置
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 2. 固定ID和URL数据
        List<Map<String, Object>> fixedData = List.of(
                Map.of("id", 1, "scenicCover", "https://img95.699pic.com/photo/50117/0971.jpg_wh860.jpg"),
                Map.of("id", 6, "scenicCover", "https://imgs.699pic.com/images/504/925/084.jpg!list1x.v2"),
                Map.of("id", 2, "scenicCover", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSETUZh8X44QqUJ7vg1MHgrkegMLIEPIVYyzQ&s"),
                Map.of("id", 29, "scenicCover", "https://pic.vjshi.com/2023-09-30/34ba578c374a425c992c93eb66d5ef3a/online/puzzle.jpg?x-oss-process=style/w1440_h2880"),
                Map.of("id", 40, "scenicCover", "https://dimg04.c-ctrip.com/images/0EQ0312000bpy6orcA147_W_640_10000.jpg?proc=autoorient"),
                Map.of("id", 42, "scenicCover", "https://youimg1.c-ctrip.com/target/100v14000000xfx9o6F1D.jpg")
        );

        // 3. 内存分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, fixedData.size());
        List<Map<String, Object>> pageList = start < fixedData.size()
                ? fixedData.subList(start, end)
                : Collections.emptyList();

        return new PageResult((long) fixedData.size(), pageList);
    }




    public PageResult queryPageByRegionId(Integer pageNum, Integer pageSize, Integer regionId) {
        // 1. 参数校验
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 2. 构造 SCAN 模式（匹配所有景点键）
        String pattern = "scenic:*:*";
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000) // 每次扫描的键数量
                .build();

        List<Scenic> scenicList = new ArrayList<>();
        long total = 0;
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = startIndex + pageSize;

        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String scenicJson = stringRedisTemplate.opsForValue().get(key);
                if (scenicJson == null) {
                    continue;}

                Scenic scenic = JsonUtils.fromJson(scenicJson, Scenic.class);
                if (scenic == null) {
                    continue;
                }

                // 3. 筛选符合 regionId 的景点
                if (regionId == null || regionId.equals(scenic.getRegionId())) {
                    total++; // 总数+1（无论是否在当前分页）

                    // 4. 只收集当前分页的数据
                    if (scenicList.size() < pageSize && total > startIndex) {
                        scenicList.add(scenic);
                    }

                    // 5. 提前终止：已收集足够数据且扫描完总数
                    if (scenicList.size() >= pageSize && total >= endIndex) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Redis扫描失败: {}", e.getMessage());
            throw new RuntimeException("查询景点分页失败", e);
        }

        // 6. 按ID排序（如果业务需要）
        scenicList.sort(Comparator.comparing(Scenic::getId));

        return new PageResult(total, scenicList);
    }

    @Override
    public Result<List<Scenic>> queryPageByRegionName(Integer page, Integer pageSize, String regionName) {
        try {
            // 1. 从 Redis 查询 region，获取 regionId
            String regionPattern = "region:*:" + regionName;
            Set<String> regionKeys = stringRedisTemplate.keys(regionPattern);

            if (regionKeys == null || regionKeys.isEmpty()) {
                log.warn("[ScenicService] 未找到地区 | regionName={}", regionName);
                return Result.error("未找到地区: " + regionName);
            }

            // 解析 regionId (从 key "region:1:西安" 中提取 1)
            String regionKey = regionKeys.iterator().next();
            String[] parts = regionKey.split(":");
            Integer regionId = Integer.parseInt(parts[1]);

            log.info("[ScenicService] 找到地区 | regionName={} | regionId={}", regionName, regionId);

            // 2. 获取所有 scenic keys
            Set<String> scenicKeys = stringRedisTemplate.keys("scenic:*:*");
            if (scenicKeys == null || scenicKeys.isEmpty()) {
                log.warn("[ScenicService] Redis中暂无景点数据");
                return Result.error("暂无景点数据");
            }

            // 3. 过滤出该地区的景点
            List<Scenic> allScenics = new ArrayList<>();
            for (String key : scenicKeys) {
                String scenicJson = stringRedisTemplate.opsForValue().get(key);
                if (scenicJson != null) {
                    try {
                        // 使用ObjectMapper替代FastJSON,避免中文日期格式解析问题
                        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
                        Scenic scenic = objectMapper.readValue(scenicJson, Scenic.class);

                        if (scenic != null && regionId.equals(scenic.getRegionId())) {
                            allScenics.add(scenic);
                            log.debug("[ScenicService] 成功解析景点 | scenicName={} | regionId={}",
                                scenic.getScenicName(), scenic.getRegionId());
                        }
                    } catch (Exception e) {
                        log.warn("[ScenicService] 解析景点数据失败 | key={} | error={}", key, e.getMessage());
                        // 跳过这个景点，继续处理下一个
                    }
                }
            }

            log.info("[ScenicService] 找到景点 | regionName={} | regionId={} | count={}",
                regionName, regionId, allScenics.size());

            // 4. 分页
            int offset = (page - 1) * pageSize;
            int end = Math.min(offset + pageSize, allScenics.size());

            if (offset >= allScenics.size()) {
                return Result.success(new ArrayList<>());
            }

            List<Scenic> pagedScenics = allScenics.subList(offset, end);
            return Result.success(pagedScenics);

        } catch (Exception e) {
            log.error("[ScenicService] 查询地区景点失败 | regionName={} | error={}",
                regionName, e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }


    @Override
    public Result<Void> queryTotalPeople(Integer id) {
        if(id == null){
            return Result.error("景点id不能为空");
        }

        try {
            // 构建基础key模式
            String keyPattern = "scenic:" + id + ":*";
            Set<String> keys = stringRedisTemplate.keys(keyPattern);

            if (keys == null || keys.isEmpty()) {
                log.error("未找到景点id为{}的记录", id);
                return Result.error("未找到对应景点记录");
            }

            // 获取第一个匹配的key（假设每个景点只有一个主key）
            String oldKey = keys.iterator().next();
            String scenicJson = stringRedisTemplate.opsForValue().get(oldKey);

            if (scenicJson == null) {
                log.error("景点id为{}的记录数据为空", id);
                return Result.error("景点记录数据为空");
            }

            Scenic originalScenic = JsonUtils.fromJson(scenicJson, Scenic.class);
            if (originalScenic == null) {
                log.error("反序列化景点id为{}的记录失败", id);
                return Result.error("景点数据解析失败");
            }

            originalScenic.setUpdateTime(LocalDateTime.now());

            // 获取该景点所有预约记录的总人数
            Integer totalPeopleCount = scenicReservationMapper.getTotalPeopleCountByScenicId(id);
            originalScenic.setTotalPeople(totalPeopleCount);
            // 将更新后的对象转换为JSON
            String updatedScenicJson = JsonUtils.toJson(originalScenic);
            stringRedisTemplate.opsForValue().set(oldKey, updatedScenicJson);

            return Result.success();
        } catch (Exception e) {
            log.error("更新景点id为{}的信息时出现异常: {}", id, e.getMessage(), e);
            return Result.error("更新景点信息时出现异常: " + e.getMessage());
        }


    }

     @Override
     public Result<Void> scenicMark(Integer scenicId, Integer mark) {
         // 1. 获取当前用户ID（根据你的安全框架调整）
         //Integer userId = 29;
         Integer userId = BaseContext.getUserId();
         if (userId == null) {
             return Result.error("用户未登录");
         }

         // 2. 获取原始Redis键（scenic:{id}:名称）
         Set<String> keys = stringRedisTemplate.keys("scenic:" + scenicId + ":*");
         if (keys == null || keys.isEmpty()) {
             return Result.error("景点不存在");
         }
         String redisKey = keys.iterator().next();

         ObjectMapper objectMapper = JsonUtils.getObjectMapper();
         try {
             // 3. 获取并解析原始JSON
             String scenicJson = stringRedisTemplate.opsForValue().get(redisKey);
             ObjectNode scenicNode = (ObjectNode) objectMapper.readTree(scenicJson);

             // 4. 检查是否已评分（使用原JSON中的字段记录）
             ArrayNode ratedUsers = scenicNode.has("ratedUsers") ?
                     (ArrayNode) scenicNode.get("ratedUsers") :
                     objectMapper.createArrayNode();

             if (ratedUsers.toString().contains(userId.toString())) {
                 return Result.error("您已评过分");
             }

             // 5. 初始化/更新评分字段
             int rateCount = scenicNode.has("rateCount") ? scenicNode.get("rateCount").asInt() : 0;
             double currentAverageMark = scenicNode.has("averageMark") ? scenicNode.get("averageMark").asDouble() : 0.0;

             // 6. 计算新评分
             double newAverageMark = (currentAverageMark * rateCount + mark) / (rateCount + 1);

             // 使用String.format格式化，保留一位小数
             String formattedAverageMark = String.format("%.1f", newAverageMark);
             double finalAverageMark = Double.parseDouble(formattedAverageMark);

             scenicNode.put("rateCount", rateCount + 1);
             scenicNode.put("averageMark", finalAverageMark);

             // 7. 记录已评分用户（添加到原JSON中）
             ratedUsers.add(userId);
             scenicNode.set("ratedUsers", ratedUsers);

             // 8. 更新Redis（只修改原有键的值）
             stringRedisTemplate.opsForValue().set(redisKey, scenicNode.toString());

             return Result.success();
         } catch (Exception e) {
             log.error("评分更新失败: scenicId={}, error={}", scenicId, e.getMessage());
             return Result.error("评分失败");
         }
     }

    @Override
    public Result<Boolean> IsCongested(Integer scenicId) {
        try {
            // 从Redis中查询景点信息
            String pattern = "scenic:" + scenicId + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys.isEmpty()) {
                log.error("未在Redis中找到景点Id为 {} 的记录", scenicId);
                return Result.error("未在Redis中找到对应的景点记录");
            }

            String key = keys.iterator().next();
            String scenicJson = stringRedisTemplate.opsForValue().get(key);
            // 复用JsonUtils中配置好的ObjectMapper实例
            ObjectMapper objectMapper = JsonUtils.getObjectMapper();
            Scenic scenic = objectMapper.readValue(scenicJson, Scenic.class);
            if (scenic == null || scenic.getId() == null) {
                log.error("从Redis获取的景点数据不完整或格式错误");
                return Result.error("从Redis获取的景点数据不完整或格式错误");
            }

            // 获取景点最大承载量
            int maxCapacity = scenic.getMaxCapacity();
            if (maxCapacity <= 0) {
                log.error("景点ID: {} 对应的最大承载量数据异常", scenicId);
                return Result.error("景点最大承载量数据异常，请检查");
            }

            // 获取该景点所有预约记录的总人数
            Integer totalPeopleCount = scenicReservationMapper.getTotalPeopleCountByScenicId(scenicId);
            if (totalPeopleCount == null) {
                totalPeopleCount = 0; // 设置默认值为0，可根据实际情况调整
            }
            // 判断是否拥堵
            if (totalPeopleCount >= maxCapacity * 0.8) {
                return  Result.success(true); // 返回拥堵状态
            } else {
                return Result.success(false); // 返回非拥堵状态
            }
        }
        catch (IOException e) {
            log.error("反序列化景点数据时出现异常", e);
            return Result.error("反序列化景点数据时出现异常");
        }
    }

    @Override
    public Result<Integer> queryTotalPeopleByScenicId(Integer scenicId) {
        try {
            // 从Redis中查询景点信息
            String pattern = "scenic:" + scenicId + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys.isEmpty()) {
                log.error("未在Redis中找到景点Id为 {} 的记录", scenicId);
                return Result.error("未在Redis中找到对应的景点记录");
            }
            // 获取该景点所有预约记录的总人数
            Integer totalPeopleCount = scenicReservationMapper.getTotalPeopleCountByScenicId(scenicId);
            if (totalPeopleCount == null) {
                totalPeopleCount = 0; // 设置默认值为0，可根据实际情况调整
            }
            return Result.success(totalPeopleCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Result<List<Scenic>> queryScenicByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.error("传入的景点名称为空");
            return Result.error("传入的景点名称为空");
        }

        log.info("[ScenicServiceImpl] 开始查询景点 | name={}", name);

        // 使用SCAN遍历所有scenic keys
        String pattern = "scenic:*:*";
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

        List<Scenic> scenicList = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                log.debug("[ScenicServiceImpl] 扫描到key: {}", key);

                String scenicJson = stringRedisTemplate.opsForValue().get(key);
                if (scenicJson != null) {
                    Scenic scenic = JsonUtils.fromJson(scenicJson, Scenic.class);
                    if (scenic != null && scenic.getScenicName() != null) {
                        // 模糊匹配景点名称
                        if (scenic.getScenicName().contains(name)) {
                            log.info("[ScenicServiceImpl] 找到匹配景点 | scenicName={}", scenic.getScenicName());
                            scenicList.add(scenic);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[ScenicServiceImpl] 查询景点失败 | name={} | error={}", name, e.getMessage(), e);
            return Result.error("查询景点信息时出现异常: " + e.getMessage());
        }

        if (scenicList.isEmpty()) {
            log.warn("[ScenicServiceImpl] 未找到匹配景点 | name={}", name);
            return Result.error("未找到对应景点");
        }

        log.info("[ScenicServiceImpl] 查询成功 | name={} | 找到{}个景点", name, scenicList.size());
        return Result.success(scenicList);
    }

    @Override
    public PageResult queryScenicComments(Integer page, Integer pageSize, Integer scenicId) {
        // 1. 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (scenicId == null) {
            throw new IllegalArgumentException("景点ID不能为空");
        }

        // 2. 构建Redis Key（你的Scenic数据存储在 "scenic:{id}:info" 之类的Key）
        String scenicKey = "scenic:" + scenicId + ":*"; // 或者更精确的Key，如 "scenic:" + scenicId + ":info"

        // 3. 从Redis获取景点信息
        Set<String> keys = stringRedisTemplate.keys(scenicKey);
        if (keys == null || keys.isEmpty()) {
            return new PageResult(0L, Collections.emptyList());
        }

        // 4. 获取第一个匹配的Key（假设每个景点只有一个Key）
        String scenicJson = stringRedisTemplate.opsForValue().get(keys.iterator().next());
        if (scenicJson == null) {
            return new PageResult(0L, Collections.emptyList());
        }

        // 5. 反序列化Scenic对象，并获取评论
        Scenic scenic = JsonUtils.fromJson(scenicJson, Scenic.class);
        if (scenic == null || scenic.getComments() == null || scenic.getComments().isEmpty()) {
            return new PageResult(0L, Collections.emptyList());
        }

        // 6. 对评论进行分页处理
        List<Comment> allComments = new ArrayList<>(scenic.getComments().values());
        long totalComments = allComments.size();

        // 计算分页范围
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, allComments.size());
        if (start >= totalComments) {
            return new PageResult(totalComments, Collections.emptyList());
        }

        // 7. 返回分页后的评论
        List<Comment> pagedComments = allComments.subList(start, end);
        return new PageResult(totalComments, pagedComments);
    }




    @Override
    public Result<Scenic> queryScenicById(Integer id) {
        if (id == null) {
            log.error("传入的景点ID为空");
            return Result.error("传入的景点ID为空");
        }

        // 构建匹配键的模式
        String pattern = "scenic:" + id + ":*";
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys.isEmpty()) {
            return Result.error("景点Id不存在");
        }

        try {
            // 假设只有一个键匹配（实际可能需处理多个匹配情况）
            String key = keys.iterator().next();
            String scenicJson = stringRedisTemplate.opsForValue().get(key);
            Scenic scenic = JsonUtils.fromJson(scenicJson, Scenic.class);
            return Result.success(scenic);
        } catch (Exception e) {
            log.error("将Redis获取的JSON数据反序列化为Scenic对象时出错，错误信息: {}", e.getMessage());
            return Result.error("查询景点信息时出现异常: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> addComments(Integer scenicId, Comment comments) {
        try {
            // 设置用户ID
            //Integer userId = 113;
            Integer userId = BaseContext.getUserId();
            comments.setUserId(userId); // 实际项目中应该从安全上下文中获取

            User user = userMapper.selectNickNameAndHeadById(userId);
            comments.setNickName(user.getNickName());
            comments.setHead(user.getHead());

            // 1. 生成评论ID
            String commentId = UUID.randomUUID().toString();

            // 2. 获取景点key
            String keyPattern = "scenic:" + scenicId + ":*";
            Set<String> keys = stringRedisTemplate.keys(keyPattern);

            if (keys == null || keys.isEmpty()) {
                return Result.error("景点不存在");
            }

            String scenicKey = keys.iterator().next();

            // 3. 获取当前景点信息
            String scenicJson = stringRedisTemplate.opsForValue().get(scenicKey);
            if (scenicJson == null) {
                return Result.error("景点数据异常");
            }

            Scenic scenic = JsonUtils.fromJson(scenicJson, Scenic.class);
            if (scenic == null) {
                return Result.error("景点数据解析失败");
            }

            // 4. 初始化comments map(如果为空)
            if (scenic.getComments() == null) {
                scenic.setComments(new HashMap<>());
            }

            // 5. 添加新评论
            scenic.getComments().put(commentId, comments);
            scenic.setUpdateTime(LocalDateTime.now());

            // 6. 更新Redis中的景点信息
            stringRedisTemplate.opsForValue().set(
                    scenicKey,
                    JsonUtils.toJson(scenic)
            );

            return Result.success();
        } catch (Exception e) {
            log.error("添加评论失败", e);
            return Result.error("添加评论失败: " + e.getMessage());
        }
    }


}
