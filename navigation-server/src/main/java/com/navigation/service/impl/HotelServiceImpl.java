package com.navigation.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.navigation.context.BaseContext;
import com.navigation.entity.Food;
import com.navigation.entity.Hotel;

import com.navigation.mapper.HotelMapper;

import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.HotelService;

import com.navigation.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<Void> saveHotel(Hotel hotel) {
        // 参数校验
        if (hotel == null) {
            log.error("传入的 hotel 对象为空，无法保存酒店信息");
            return Result.error("传入的酒店信息为空");
        }

        List<String> missingFields = new ArrayList<>();

        if (hotel.getRegionId() == null || hotel.getRegionId().toString().isEmpty()) {
            missingFields.add("region_id");
        }
        if (hotel.getLatitude() == null || hotel.getLatitude().toString().isEmpty()) {
            missingFields.add("latitude");
        }
        if (hotel.getLongitude() == null || hotel.getLongitude().toString().isEmpty()) {
            missingFields.add("longitude");
        }
        if (hotel.getCover() == null || hotel.getCover().trim().isEmpty()) {
            missingFields.add("cover");
        }
        if (hotel.getAddress() == null || hotel.getAddress().trim().isEmpty()) {
            missingFields.add("address");
        }
        if (hotel.getPhoneNumber() == null || hotel.getPhoneNumber().trim().isEmpty()) {
            missingFields.add("phone_number");
        }
        if (hotel.getHotelName() == null || hotel.getHotelName().trim().isEmpty()) {
            missingFields.add("hotel_name");
        }
        if (hotel.getHotelDescription() == null || hotel.getHotelDescription().trim().isEmpty()) {
            missingFields.add("hotel_description");
        }

        if (!missingFields.isEmpty()) {
            String fieldsString = String.join(", ", missingFields);
            return Result.error("以下必填参数未传入: " + fieldsString);
        }

        // 构建匹配键的模式
        String pattern = "region:" + hotel.getRegionId() + ":*";
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys.isEmpty()) {
            return Result.error("地区Id不存在");
        }

        try {
            hotel.setCreateTime(LocalDateTime.now());
            hotel.setUpdateTime(LocalDateTime.now());
            hotelMapper.saveHotel(hotel);
            return Result.success();
        } catch (Exception e) {
            log.error("保存酒店信息时出现异常", e);
            return Result.error("保存酒店信息失败，请稍后重试");
        }
    }

    @Override
    public Result<Void> update(Hotel hotel) {
        if (hotel == null) {
            log.error("传入的 hotel 对象为空，无法更新酒店信息");
            return Result.error("传入的酒店信息为空");
        }

        try {
            // 检查 hotelId 是否存在
            Integer hotelId = hotel.getId();
            if (hotelId != null) {
                int count = hotelMapper.countHotelById(hotelId);
                if (count == 0) {
                    log.error("酒店id不存在");
                    return Result.error("酒店id不存在");
                }
            } else {
                log.error("传入的 Hotel 对象中 hotelId 为空");
                return Result.error("传入的 Hotel 对象中 hotelId 为空");
            }
            if (hotel.getRegionId() != null) {
                // 构建匹配键的模式
                String pattern = "region:" + hotel.getRegionId() + ":*";
                Set<String> keys = stringRedisTemplate.keys(pattern);
                if (keys.isEmpty()) {
                    return Result.error("地区Id不存在");
                }
            }

            hotel.setUpdateTime(LocalDateTime.now());
            hotelMapper.update(hotel);
            return Result.success();
        } catch (Exception e) {
            log.error("更新酒店信息时出现异常", e);
            return Result.error("更新酒店信息时出现异常: " + e.getMessage());
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
            // 获取数据库中所有存在的酒店ID集合
            List<Integer> allExistingIds = hotelMapper.getAllExistingHotelIds();
            Set<Integer> existingIdSet = new HashSet<>(allExistingIds);

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
                return Result.error("ID为 " + idsString + " 的酒店记录不存在，删除失败");
            }

            // 执行批量删除操作
            int rows = hotelMapper.batchDelete(ids);
            if (rows > 0) {
                return Result.success();
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            // 记录详细的异常日志，实际应用中建议使用日志框架，如Logback或Log4j
            log.error("删除酒店过程中出现异常", e);
            // 返回包含具体异常信息的错误结果
            return Result.error("删除过程中出现异常: " + e.getMessage());
        }
    }

    @Override
    public PageResult queryHotel(Integer pageNum, Integer pageSize) {
        // 设置分页参数
        PageHelper.startPage(pageNum, pageSize);
        List<Hotel> hotelList = hotelMapper.queryHotel(pageNum, pageSize);
        //获取分页元数据（总记录数、总页数等）
        Page list = (Page<Hotel>) hotelList;
        PageResult pageResult = new PageResult(list.getTotal(), list.getResult());
        return pageResult;
    }

    @Override
    public PageResult queryHotel2(Integer page, Integer pageSize) {
        // 1. 参数校验和默认值设置
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 2. 固定ID和URL数据
        List<Map<String, Object>> fixedData = List.of(
                Map.of("id", 1, "cover", "https://d.ifengimg.com/q100/img1.ugc.ifeng.com/newugc/20201104/17/wemedia/a702213a87e916d91ce4cd3605d8fefa11c7d3c0_size425_w1280_h853.jpg"),
                Map.of("id", 10, "cover", "https://th.bing.com/th/id/R.917d8ab60e601e83620b7a97a0ca20f5?rik=qSrSlwbMelg%2bLw&riu=http%3a%2f%2fwww.zsgsly.com%2fuploadfile%2f2020%2f0522%2f20200522044415422.jpg&ehk=gwPq6A9k9PorlEQdb8EjT7D8MZmRgrdEcYgDDiwi63c%3d&risl=&pid=ImgRaw&r=0"),
                Map.of("id", 24, "cover", "https://dimg04.c-ctrip.com/images/0225f12000b4fblnmB61C_C_750_340_Q70.png"),
                Map.of("id", 29, "cover", "https://photo.16pic.com/00/52/30/16pic_5230044_b.jpg"),
                Map.of("id", 36, "cover", "https://th.bing.com/th/id/R.32687a1119f293bef1f3f88cb6c0c6d4?rik=DMCnV%2fUDdT0CDA&riu=http%3a%2f%2fimg1n.soufunimg.com%2fzxb%2f201609%2f05%2f856%2f01f8da9be45791c9a07802bfdf159fd9.jpeg&ehk=qp%2bec8afHbwn%2fedfbXzFo8PtB6kZmbYwgIPS0Scs09k%3d&risl=&pid=ImgRaw&r=0"),
                Map.of("id", 40, "cover", "https://p8.itc.cn/images01/20230131/c82f57f9581d49499b349c95f2e2afd7.jpeg")
        );

        // 3. 内存分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, fixedData.size());
        List<Map<String, Object>> pageList = start < fixedData.size()
                ? fixedData.subList(start, end)
                : Collections.emptyList();

        return new PageResult((long) fixedData.size(), pageList);
    }

    @Override
    public Result<List<Hotel>> searchHotels(String query) {
        // 调用 Mapper 查询酒店数据
        List<Hotel> hotels = hotelMapper.searchHotels(query);

        // 如果查询结果为空，则返回提示信息
        if (hotels.isEmpty()) {
            return Result.error("未找到符合条件的酒店。");
        }

        return Result.success(hotels);
    }

    @Override
    public Result<Hotel> queryHotelById(Integer id) {
        if (id == null) {
            log.error("传入的id为空");
            return null;
        }
        int num = hotelMapper.countHotelById(id);
        if (num == 0) {
            return Result.error("酒店Id不存在");
        }
        try {
            Hotel hotel = hotelMapper.queryHotelById(id);
            return Result.success(hotel);
        } catch (Exception e) {
            log.error("根据酒店ID查询酒店信息时发生异常", e);
            return Result.error("根据酒店ID查询酒店信息失败，请稍后重试");
        }
    }

    @Override
    public Result<Void> hotelMark(Integer hotelId, Integer mark) {
        // 1. 获取当前用户ID（根据您的安全框架调整）
        //Integer userId = 1;
        Integer userId = BaseContext.getUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 2. 查询酒店
        Hotel hotel = hotelMapper.queryHotelById(hotelId);
        if (hotel == null) {
            return Result.error("酒店不存在");
        }

        // 3. 检查是否已评分
        if (hotel.getRatedUserIds().contains(userId.toString())) {
            return Result.error("您已评过分");
        }

        // 4. 计算新评分
        int newRateCount = hotel.getRateCount() + 1;
        double newAverageMark = (hotel.getAverageMark() * hotel.getRateCount() + mark) / newRateCount;

        // 使用String.format格式化，保留一位小数
        String formattedAverageMark = String.format("%.1f", newAverageMark);
        double finalAverageMark = Double.parseDouble(formattedAverageMark);

        // 5. 更新评分信息
        hotel.setRateCount(newRateCount);
        hotel.setAverageMark(finalAverageMark);
        hotel.setRatedUserIds(
                hotel.getRatedUserIds().isEmpty() ?
                        userId.toString() :
                        hotel.getRatedUserIds() + "," + userId
        );

        // 6. 保存到数据库
        hotelMapper.updateById(hotel);

        return Result.success();
    }

    @Override
    public Result<List<Hotel>> queryHotelByName(String name) {
        if (name == null) {
            log.error("传入的名称为空");
            return Result.error("传入的名称为空");
        }
        int num = hotelMapper.countHotelByName(name);
        if (num == 0) {
            return Result.error("酒店名称不存在");
        }
        try {
            List<Hotel> hotelList = hotelMapper.queryHotelByName(name);
            return Result.success(hotelList);
        } catch (Exception e) {
            log.error("根据酒店名称查询酒店信息时发生异常", e);
            return Result.error("根据酒店名称查询酒店信息失败，请稍后重试");
        }
    }

    @Override
    public Result<List<Hotel>> queryHotelByRegionId(Integer regionId) {
        // 参数校验
        if (regionId == null) {
            log.error("查询酒店失败: 区域ID不能为空");
            return Result.error("区域ID不能为空");
        }

        try {
            // 构建Redis键模式：region:{regionId}:*
            String pattern = "region:" + regionId + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                log.info("区域ID {} 下未找到任何酒店", regionId);
                return Result.error("区域ID下未找到任何酒店");
            }

            List<Hotel> hotels = hotelMapper.queryHotelByRegionId(regionId);
            return Result.success(hotels);
        } catch (Exception e) {
            log.error("根据区域ID查询酒店信息时发生异常", e);
            return Result.error("根据区域ID查询酒店信息失败，请稍后重试");
        }
    }


}
