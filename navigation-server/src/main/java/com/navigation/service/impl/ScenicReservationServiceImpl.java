package com.navigation.service.impl;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.navigation.context.BaseContext;
import com.navigation.dto.ScenicReservationDTO;
import com.navigation.entity.Scenic;
import com.navigation.entity.ScenicReservation;

import com.navigation.mapper.ScenicMapper;
import com.navigation.mapper.ScenicReservationMapper;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.ScenicReservationService;
import com.navigation.service.ScenicService;
import com.navigation.utils.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScenicReservationServiceImpl extends ServiceImpl<ScenicReservationMapper, ScenicReservation> implements ScenicReservationService {


    @Resource
    private ScenicReservationMapper scenicReservationMapper;

    @Resource
    private ScenicMapper scenicMapper;

    @Autowired
    private Validator validator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<Void> saveScenicReservation(ScenicReservation scenicReservation) {
        // 参数校验
        if (scenicReservation == null) {
            log.error("传入的 ScenicReservation 对象为空，无法保存景点预约信息");
            return Result.error("传入的景点预约信息为空");
        }

        // 从UserHolder获取userId并设置到scenicReservation对象
        Integer userId = BaseContext.getUserId();
        //Integer userId = 1;
        scenicReservation.setUserId(userId);


        if (scenicReservation.getScenicId() == null) {
            log.error("景点ID为空，无法保存景点预约信息");
            return Result.error("景点ID为空");
        }
        // 判断peopleCount是否为空
        if (scenicReservation.getPeopleCount() == null) {
            log.error("预约人数为空，无法保存景点预约信息");
            return Result.error("预约人数为空");
        }

        // 判断reservationDate是否为空
        if (scenicReservation.getReservationDate() == null) {
            log.error("预约日期为空，无法保存景点预约信息");
            return Result.error("预约日期为空");
        }

        Integer scenicId = scenicReservation.getScenicId();

        // 检查该用户对该景点是否已存在预约记录
        boolean isExist = scenicReservationMapper.existsByUserIdAndScenicId(userId, scenicId);
        if (isExist) {
            log.error("用户ID: {} 已存在对景点ID: {} 的预约记录，不允许重复预约", userId, scenicId);
            return Result.error("您已预约过该景点，不允许重复预约");
        }

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
            int total = scenicReservation.getPeopleCount() + totalPeopleCount;
            if (total >= maxCapacity * 0.8) {
                scenicReservation.setIsCongested(1);
            } else {
                scenicReservation.setIsCongested(0);
            }
        } catch (IOException e) {
            log.error("反序列化景点数据时出现异常", e);
            return Result.error("反序列化景点数据时出现异常");
        }

        // 使用Validator校验除userId和scenicId外的其他字段
        Set<ConstraintViolation<ScenicReservation>> violations = validator.validate(scenicReservation);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("以下字段校验不通过: ");
            for (ConstraintViolation<ScenicReservation> violation : violations) {
                errorMessage.append(violation.getPropertyPath()).append(" - ").append(violation.getMessage()).append("; ");
            }
            log.error(errorMessage.toString());
            return Result.error(errorMessage.toString());
        }

        try {
            scenicReservation.setCreateTime(LocalDateTime.now());
            scenicReservation.setUpdateTime(LocalDateTime.now());
            scenicReservationMapper.saveScenicReservation(scenicReservation);
            return Result.success();
        } catch (Exception e) {
            log.error("保存景点预约信息时出现异常", e);
            return Result.error("保存景点预约信息失败，请稍后重试");
        }

    }

    @Override
    public Result<Void> update(ScenicReservation scenicReservation) {
        if (scenicReservation == null) {
            log.error("传入的 ScenicReservation 对象为空，无法更新景点预约信息");
            return Result.error("传入的景点预约信息为空");
        }

        // 必须传递reservationId，进行校验
        if (scenicReservation.getReservationId() == null) {
            log.error("reservationId为空，无法进行更新操作");
            return Result.error("reservationId为空，请检查");
        }

        try {
            // 从UserHolder获取当前用户ID
            Integer userId = BaseContext.getUserId();
            if (userId == null) {
                log.error("无法获取当前用户ID，更新操作失败");
                return Result.error("无法获取当前用户ID，请检查");
            }
            //scenicReservation.setUserId(1);

            // 获取传入的景点ID
            Integer scenicId = scenicReservation.getScenicId();
            if (scenicId == null) {
                scenicReservation.setUpdateTime(LocalDateTime.now());
                scenicReservationMapper.update(scenicReservation);
                return Result.success();
            }

            // 从Redis中检查scenicId是否存在
            Set<String> keys = stringRedisTemplate.keys("scenic:" + scenicId + ":*");
            if (keys.isEmpty()) {
                log.info("Redis中不存在景点ID为 {} 的记录，不进行数据库更新", scenicId);
                return Result.error("景点id不存在");
            }

            // 检查该用户对该景点是否已存在预约记录
            boolean isExist = scenicReservationMapper.existsByUserIdAndScenicId(userId, scenicId);
            if (isExist) {
                log.error("用户ID: {} 已存在对景点ID: {} 的预约记录，不允许重复预约", userId, scenicId);
                return Result.error("您已预约过该景点，不允许重复预约");
            } else {
                // 没有找到相同userid和scenicid的记录，进行参数更改操作
                scenicReservation.setUpdateTime(LocalDateTime.now());
                scenicReservationMapper.update(scenicReservation);
                return Result.success();
            }
        } catch (Exception e) {
            log.error("更新景点预约信息时出现异常", e);
            return Result.error("更新景点预约信息失败，请稍后重试");
        }
    }

    @Override
    @Transactional  // 添加事务管理（确保批量删除原子性）
    public Result<Void> batchDelete(List<Integer> ids) {
        // 检查传入的ID列表是否为空
        if (ids == null || ids.isEmpty()) {
            return Result.error("删除的ID列表不能为空");
        }

        try {
            // 获取数据库中所有存在的ID集合
            List<Integer> allExistingIds = scenicReservationMapper.getAllExistingIds();
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
                        .collect(Collectors.joining(","));
                return Result.error("ID为 " + idsString + " 的记录不存在，删除失败");
            }

            // 执行批量删除操作
            int rows = scenicReservationMapper.batchDelete(ids);
            if (rows > 0) {
                return Result.success();
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除操作出现异常", e);
            return Result.error("批量删除操作失败，请稍后重试");
        }
    }

    @Override
    public PageResult queryScenicReservation(Integer pageNum, Integer pageSize) {
        // 从BaseContext获取当前用户ID
        Integer userId = BaseContext.getUserId();
        // 设置分页参数
        PageHelper.startPage(pageNum, pageSize);
        List<ScenicReservation> scenicList = scenicReservationMapper.queryScenicReservation(userId, pageNum, pageSize);
        //获取分页元数据（总记录数、总页数等）
        Page list = (Page<ScenicReservation>) scenicList;
        PageResult pageResult = new PageResult(list.getTotal(), list.getResult());
        return pageResult;
    }

    @Override
    public PageResult queryScenicReservationTop10(Integer page, Integer pageSize) {
        // 1. 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 2. 从MySQL查询预约人数排名（只返回scenic_id和人数）
        List<Map<String, Object>> reservationRank = scenicReservationMapper.selectReservationRank(
                (page - 1) * pageSize,
                pageSize
        );
        if (reservationRank == null || reservationRank.isEmpty()) {
            return new PageResult(0L, Collections.emptyList());
        }

        // 3. 批量从Redis获取景点信息
        List<ScenicReservationDTO> resultList = new ArrayList<>();
        for (Map<String, Object> item : reservationRank) {
            Integer scenicId = (Integer) item.get("scenic_id");
            Long totalPeople = ((Number) item.get("total_people")).longValue();

            // 3.1 扫描Redis获取景点Key
            Set<String> scenicKeys = stringRedisTemplate.keys("scenic:" + scenicId + ":*");
            if (scenicKeys == null || scenicKeys.isEmpty()) {
                resultList.add(new ScenicReservationDTO(scenicId, "未知景点", totalPeople, ""));
                continue;
            }

            // 3.2 获取景点完整信息
            String scenicJson = stringRedisTemplate.opsForValue().get(scenicKeys.iterator().next());
            try {
                Scenic scenic = JsonUtils.fromJson(scenicJson, Scenic.class);
                resultList.add(new ScenicReservationDTO(
                        scenicId,
                        scenic.getScenicName(),
                        totalPeople,
                        scenic.getScenicCover() // 新增封面路径
                ));
            } catch (Exception e) {
                log.error("解析Redis景点数据失败，scenicId: {}", scenicId, e);
                resultList.add(new ScenicReservationDTO(scenicId, "数据异常", totalPeople, ""));
            }
        }

        // 4. 获取总景点数（用于分页）
        Long total = scenicReservationMapper.countDistinctScenicIds();
        return new PageResult(total, resultList);
    }



   /* @Override
    public Result<ScenicReservation> queryScenicReservationById(Integer id) {
        if (id == null) {
            log.error("传入的景点预约ID为空");
            return Result.error("id为空！");
        }

        try {
            ScenicReservation scenicReservation = scenicReservationMapper.queryScenicReservationById(id);
            if (scenicReservation == null) {
                log.error("景点预约id不存在");
                return Result.error("id不存在！");
            }
            return Result.success(scenicReservation);
        } catch (Exception e) {
            log.error("根据id查询景点预约信息时发生异常", e);
            return Result.error("根据id查询景点预约信息失败，请稍后重试");
        }
    }*/


}
