package com.navigation.service.impl;



import cn.hutool.core.lang.TypeReference;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.navigation.entity.Region;


import com.navigation.entity.Scenic;
import com.navigation.mapper.RegionMapper;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.RegionService;

import com.navigation.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements RegionService {

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 获取自增的regionId
    private long getIncrementedRegionId() {
        // 使用Redis的INCR命令对指定键进行自增操作，这里假设使用"region_id_counter"作为键
        return stringRedisTemplate.opsForValue().increment("region_id_counter");
    }
    @Override
    public Result<Void> saveRegion(Region region) {
        // 参数校验
        if (region == null) {
            log.error("传入的 region 对象为空，无法保存地区信息");
            return Result.error("传入的地区信息为空");
        }

        List<String> missingFields = new ArrayList<>();
        // 假设Region类中有name、description等必填字段，这里以name为例
        if (region.getRegionName() == null || region.getRegionName().trim().isEmpty()) {
            missingFields.add("region_name");
        }
        if (region.getRegionDescription() == null || region.getRegionDescription().trim().isEmpty()) {
            missingFields.add("region_description");
        }

        // 可根据实际情况继续添加对其他必填字段的检查，如description等
        if (!missingFields.isEmpty()) {
            String fieldsString = String.join(", ", missingFields);
            return Result.error("以下必填参数未传入: " + fieldsString);
        }


        try {
           /* region.setCreateTime(LocalDateTime.now());
            region.setUpdateTime(LocalDateTime.now());
            regionMapper.saveRegion(region);
            return Result.success();*/
            region.setCreateTime(LocalDateTime.now());
            region.setUpdateTime(LocalDateTime.now());
            // 获取自增后的regionId并设置到Region对象中
            long newRegionId = getIncrementedRegionId();
            region.setRegionId((int) newRegionId);
            // 构建Redis的key
            String key = "region:" + region.getRegionId() + ":" + region.getRegionName();
            // 将Region对象转换为JSON字符串，这里假设使用Jackson库进行转换，需引入相关依赖
            String regionJson = JsonUtils.toJson(region);
            stringRedisTemplate.opsForValue().set(key, regionJson);
            return Result.success();
        } catch (Exception e) {
            log.error("保存地区信息时出现异常", e);
            return Result.error("保存地区信息失败，请稍后重试");
        }
    }

    @Override
    public Result<Void> update(Region region) {
        if (region == null) {
            log.error("传入的 region 对象为空，无法更新地区信息");
            return Result.error("传入的地区信息为空");
        }
        try {
            String regionName = region.getRegionName();
            // 查找包含该regionName的所有key
            Set<String> keys = stringRedisTemplate.keys("region:*:" + regionName);
            if (keys.isEmpty()) {
                log.error("未找到地区名称为 {} 的记录", regionName);
                return Result.error("未找到对应地区记录");
            }
            // 假设只有一条记录匹配（实际可能需更复杂逻辑处理多条匹配情况）
            String key = keys.iterator().next();
            // 从Redis获取原数据
            String regionJson = stringRedisTemplate.opsForValue().get(key);
            Region originalRegion = null;
            if (regionJson != null) {
                try {
                    originalRegion = JsonUtils.fromJson(regionJson, Region.class);
                    // 更新需要修改的字段，这里假设除了updateTime，其他字段如果传入有值就更新
                    if (region.getRegionName() != null) {
                        originalRegion.setRegionName(region.getRegionName());
                    }
                    if (region.getRegionDescription() != null) {
                        originalRegion.setRegionDescription(region.getRegionDescription());
                    }
                    originalRegion.setUpdateTime(LocalDateTime.now());
                } catch (Exception e) {
                    log.error("将Redis获取的JSON数据反序列化为Region对象时出错，错误信息: {}", e.getMessage());
                    return Result.error("地区数据反序列化失败");
                }
            }
            if (originalRegion != null) {
                // 将更新后的对象转换为JSON字符串并存回Redis
                String updatedRegionJson = JsonUtils.toJson(originalRegion);
                stringRedisTemplate.opsForValue().set(key, updatedRegionJson);
                return Result.success();
            } else {
                log.error("地区记录在Redis中存在问题");
                return Result.error("地区记录处理异常");
            }
        } catch (Exception e) {
            log.error("更新地区信息时出现异常", e);
            return Result.error("更新地区信息时出现异常: " + e.getMessage());
        }
    }
    /*public Result<Void> update(Region region) {
        if (region == null) {
            log.error("传入的 region 对象为空，无法更新地区信息");
            return Result.error("传入的地区信息为空");
        }

        *//*try {
            // 检查 regionId 是否存在
            Integer regionId = region.getRegionId();
            if (regionId != null) {
                int count = regionMapper.countRegionById(regionId);
                if (count == 0) {
                    log.error("地区记录不存在");
                    return Result.error("地区记录不存在");
                }
            } else {
                log.error("传入的 Region 对象中 regionId 为空");
                return Result.error("传入的 Region 对象中 regionId 为空");
            }

            region.setUpdateTime(LocalDateTime.now());
            regionMapper.update(region);
            return Result.success();
        } catch (Exception e) {
            log.error("更新地区信息时出现异常", e);
            return Result.error("更新地区信息时出现异常: " + e.getMessage());
        }*//*
        try {
            String regionName = region.getRegionName();
            // 查找包含该regionName的所有key
            Set<String> keys = stringRedisTemplate.keys("region:*:" + regionName);
            if (keys.isEmpty()) {
                log.error("未找到地区名称为 {} 的记录", regionName);
                return Result.error("未找到对应地区记录");
            }
            // 假设只有一条记录匹配（实际可能需更复杂逻辑处理多条匹配情况）
            String key = keys.iterator().next();
            // 从Redis获取原数据
            String regionJson = stringRedisTemplate.opsForValue().get(key);
            Region originalRegion = null;
            if (regionJson != null) {
                try {
                    originalRegion = JsonUtils.fromJson(regionJson, Region.class);
                    // 更新需要修改的字段，这里假设除了updateTime，其他字段如果传入有值就更新
                    if (region.getRegionName() != null) {
                        originalRegion.setRegionName(region.getRegionName());
                    }
                    if (region.getRegionDescription() != null) {
                        originalRegion.setRegionDescription(region.getRegionDescription());
                    }
                    originalRegion.setUpdateTime(LocalDateTime.now());
                } catch (Exception e) {
                    log.error("将Redis获取的JSON数据反序列化为Region对象时出错，错误信息: {}", e.getMessage());
                    return Result.error("地区数据反序列化失败");
                }
            }
            if (originalRegion != null) {
                // 将更新后的对象转换为JSON字符串并存回Redis
                String updatedRegionJson = JsonUtils.toJson(originalRegion);
                stringRedisTemplate.opsForValue().set(key, updatedRegionJson);
                return Result.success();
            } else {
                log.error("地区记录在Redis中存在问题");
                return Result.error("地区记录处理异常");
            }
        } catch (Exception e) {
            log.error("更新地区信息时出现异常", e);
            return Result.error("更新地区信息时出现异常: " + e.getMessage());
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
                String pattern = "region:" + id + ":*";
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
                return Result.error("ID为 " + idsString + " 的地区记录不存在，删除失败");
            }

            // 执行批量删除操作
            for (Integer id : ids) {
                String pattern = "region:" + id + ":*";
                Set<String> keys = stringRedisTemplate.keys(pattern);
                for (String key : keys) {
                    stringRedisTemplate.delete(key);
                }
            }
            return Result.success();
        } catch (Exception e) {
            // 记录详细的异常日志，实际应用中建议使用日志框架，如Logback或Log4j
            log.error("批量删除地区过程中出现异常", e);
            // 返回包含具体异常信息的错误结果
            return Result.error("删除过程中出现异常: " + e.getMessage());
        }
    }

   /* public Result<Void> batchDelete(List<Integer> ids) {
        // 检查传入的ID列表是否为空
        if (ids == null || ids.isEmpty()) {
            return Result.error("删除的ID列表不能为空");
        }

        try {
            // 获取数据库中所有存在的地区ID集合
            List<Integer> allExistingIds = regionMapper.getAllExistingRegionIds();
            Set<Integer> existingIdSet = allExistingIds.stream().collect(Collectors.toSet());

            // 用于存储不存在的ID
            List<Integer> nonExistingIds = new ArrayList<>();

            // 检查传入的每个ID是否存在
            for (Integer id : ids) {
                if (!existingIdSet.contains(id)) {
                    nonExistingIds.add(id);
                }
            }

            // 如果有不存在的ID，返回包含所有不存在ID的错误信息
            if (!nonExistingIds.isEmpty()) {
                String idsString = nonExistingIds.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                return Result.error("ID为 " + idsString + " 的地区记录不存在，删除失败");
            }

            // 执行批量删除操作
            int rows = regionMapper.batchDelete(ids);
            if (rows > 0) {
                return Result.success();
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            // 记录详细的异常日志，实际应用中建议使用日志框架，如Logback或Log4j
            log.error("批量删除地区过程中出现异常", e);
            // 返回包含具体异常信息的错误结果
            return Result.error("删除过程中出现异常: " + e.getMessage());
        }
    }*/

    @Override
    public PageResult queryRegion(Integer pageNum, Integer pageSize) {
        String pattern = "region:*:*";
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(pageSize)
                .build();

        List<Region> regionList = new ArrayList<>();
        long total = 0;

        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            int index = 0;
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = startIndex + pageSize;
            while (cursor.hasNext()) {
                String key = cursor.next();
                total++;
                if (index >= startIndex && index < endIndex) {
                    String regionJson = stringRedisTemplate.opsForValue().get(key);
                    if (regionJson != null) {
                        Region region = JsonUtils.fromJson(regionJson, Region.class);
                        regionList.add(region);
                    }
                }
                index++;
            }
        } catch (Exception e) {
            log.error("扫描Redis键时出错: {}", e.getMessage());
        }

        // 按regionId排序
        regionList.sort((r1, r2) -> r1.getRegionId().compareTo(r2.getRegionId()));

        return new PageResult(total, regionList);
    }
    /*public PageResult queryRegion(Integer pageNum, Integer pageSize) {
        // 设置分页参数
        PageHelper.startPage(pageNum, pageSize);
        List<Region> regionList = regionMapper.queryRegion(pageNum, pageSize);
        //获取分页元数据（总记录数、总页数等）
        Page list = (Page<Region>) regionList;
        PageResult pageResult = new PageResult(list.getTotal(), list.getResult());
        return pageResult;
    }*/

    @Override
    public Result<Region> queryRegionById(Integer id) {
        if (id == null) {
            log.error("传入的id为空");
            return Result.error("传入的id为空");
        }

        // 构建匹配键的模式
        String pattern = "region:" + id + ":*";
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys.isEmpty()) {
            return Result.error("地区Id不存在");
        }

        try {
            // 假设只有一个键匹配（实际可能需处理多个匹配情况）
            String key = keys.iterator().next();
            String regionJson = stringRedisTemplate.opsForValue().get(key);
            Region region = JsonUtils.fromJson(regionJson, Region.class);
            return Result.success(region);
        } catch (Exception e) {
            log.error("将Redis获取的JSON数据反序列化为Region对象时出错，错误信息: {}", e.getMessage());
            return Result.error("查询地区信息时出现异常: " + e.getMessage());
        }
    }

    @Override
    public Result<List<Region>> queryScenicByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.error("传入的地区名称为空");
            return Result.error("传入的地区名称为空");
        }

        log.info("[RegionServiceImpl] 开始查询地区 | name={}", name);

        // 使用SCAN遍历所有region keys
        String pattern = "region:*:*";
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

        List<Region> regionList = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                log.debug("[RegionServiceImpl] 扫描到key: {}", key);

                String regionJson = stringRedisTemplate.opsForValue().get(key);
                if (regionJson != null) {
                    Region region = JsonUtils.fromJson(regionJson, Region.class);
                    if (region != null && region.getRegionName() != null) {
                        // 模糊匹配地区名称
                        if (region.getRegionName().contains(name)) {
                            log.info("[RegionServiceImpl] 找到匹配地区 | regionName={}", region.getRegionName());
                            regionList.add(region);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[RegionServiceImpl] 查询地区失败 | name={} | error={}", name, e.getMessage(), e);
            return Result.error("查询地区信息时出现异常: " + e.getMessage());
        }

        if (regionList.isEmpty()) {
            log.warn("[RegionServiceImpl] 未找到匹配地区 | name={}", name);
            return Result.error("未找到对应地区");
        }

        log.info("[RegionServiceImpl] 查询成功 | name={} | 找到{}个地区", name, regionList.size());
        return Result.success(regionList);
    }

   /* public Result<Region> queryRegionById(Integer id) {
        if (id == null) {
            log.error("传入的id为空");
            // 返回错误结果，而不是null
            return Result.error("传入的id为空");
        }

        int num = regionMapper.countRegionById(id);
        if(num == 0){
            return Result.error("地区Id不存在");
        }
        try {
            Region region = regionMapper.queryRegionById(id);
            return Result.success(region);
        } catch (Exception e) {
            log.error("查询地区信息时出现异常", e);
            // 返回包含异常信息的错误结果
            return Result.error("查询地区信息时出现异常: " + e.getMessage());
        }
    }*/

}
