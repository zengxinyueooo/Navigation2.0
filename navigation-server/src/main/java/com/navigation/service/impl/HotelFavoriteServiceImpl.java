package com.navigation.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.navigation.context.BaseContext;
import com.navigation.dto.HotelFavoriteDTO;
import com.navigation.entity.Hotel;
import com.navigation.entity.HotelFavorite;
import com.navigation.entity.ScenicFavorite;
import com.navigation.mapper.HotelFavoriteMapper;
import com.navigation.mapper.HotelMapper;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.HotelFavoriteService;

import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;


import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
public class HotelFavoriteServiceImpl implements HotelFavoriteService {

    private final HotelFavoriteMapper hotelFavoriteMapper;

    @Autowired
    private HotelMapper hotelMapper;

    public HotelFavoriteServiceImpl(HotelFavoriteMapper hotelFavoriteMapper) {
        this.hotelFavoriteMapper = hotelFavoriteMapper;
    }

    @Override
    public Result<Void> save(Integer hotelId) {
        // 从BaseContext获取当前用户ID
        Integer userId = BaseContext.getUserId();
        //Integer userId = 1;
        // 参数非空判断
        if (userId == null || hotelId == null) {
            return Result.error("用户ID或酒店ID不能为空");
        }

        // 检查用户是否存在
        int userCount = hotelFavoriteMapper.countUserById(userId);
        if (userCount == 0) {
            return Result.error("用户ID不存在");
        }

        // 检查酒店是否存在
        int hotelCount = hotelFavoriteMapper.countHotelById(hotelId);
        if (hotelCount == 0) {
            return Result.error("酒店ID不存在");
        }
        Hotel hotel = hotelMapper.queryHotelById(hotelId);
        String name = hotel.getHotelName();
        if (name == null) {
            return Result.error("酒店ID不存在");
        }
        System.out.println(name);

        HotelFavorite hotelFavorite = new HotelFavorite();
        hotelFavorite.setUserId(userId);
        hotelFavorite.setHotelId(hotelId);
        hotelFavorite.setStatus(1);
        hotelFavorite.setHotelName(name);
        hotelFavorite.setCreateTime(LocalDateTime.now());
        hotelFavorite.setUpdateTime(LocalDateTime.now());

        try {
            int num = hotelFavoriteMapper.save(hotelFavorite);
            if (num == 0) {
                return Result.error("收藏失败，请稍后重试");
            }
            return Result.success();
        } catch (DataIntegrityViolationException e) {
            log.warn("用户 {} 已收藏酒店 {}", userId, hotelId, e);
            return Result.success();
        } catch (Exception e) {
            log.error("保存酒店收藏信息时发生异常", e);
            return Result.error("保存酒店收藏信息失败，请稍后重试");
        }
    }

    @Override
    public Result<Void> cancel( Integer hotelId) {
        // 从BaseContext获取当前用户ID
        Integer userId = BaseContext.getUserId();
        //Integer userId = 1;
        // 参数非空判断
        if (userId == null || hotelId == null) {
            return Result.error("用户ID或酒店ID不能为空");
        }

        // 检查用户是否存在
        int userCount = hotelFavoriteMapper.countUserById(userId);
        if (userCount == 0) {
            return Result.error("用户ID不存在");
        }

        // 检查酒店是否存在
        int hotelCount = hotelFavoriteMapper.countHotelById(hotelId);
        if (hotelCount == 0) {
            return Result.error("酒店ID不存在");
        }
        try {
            int result = hotelFavoriteMapper.deleteByUserIdAndHotelId(userId, hotelId);
            if (result > 0) {
                return Result.success();
            }
            return Result.error("取消收藏失败，请稍后重试");
        } catch (Exception e) {
            log.error("取消酒店收藏时发生异常", e);
            return Result.error("取消酒店收藏时出现异常，请稍后重试");
        }
    }

    /*@Override
    public Result<List<HotelFavorite>> getHotelFavoritesByUserId(Integer userId) {
        // 参数非空判断
        if (userId == null ) {
            return Result.error("用户ID不能为空");
        }

        // 检查用户是否存在
        int userCount = hotelFavoriteMapper.countUserById(userId);
        if (userCount == 0) {
            return Result.error("用户ID不存在");
        }

        try {
            List<HotelFavorite> favorites = hotelFavoriteMapper.selectByUserId(userId);
            return Result.success(favorites);
        } catch (Exception e) {
            log.error("查询酒店收藏信息时发生异常", e);
            return Result.error("查询酒店收藏信息失败，请稍后重试");
        }
    }*/

    @Override
    public Result<Integer> isHotelFavorite(Integer hotelId) {
        Integer userId = BaseContext.getUserId();
        //Integer userId = 115;

        // 参数非空判断
        if (userId == null || hotelId == null) {
            return Result.error("用户ID或酒店ID不能为空");
        }

        // 检查用户是否存在
        int userCount = hotelFavoriteMapper.countUserById(userId);
        if (userCount == 0) {
            return Result.error("用户ID不存在");
        }

        // 检查酒店是否存在
        int hotelCount = hotelFavoriteMapper.countHotelById(hotelId);
        if (hotelCount == 0) {
            return Result.error("酒店ID不存在");
        }
        try {
            Integer favorite = hotelFavoriteMapper.existsByUserIdAndHotelId(userId, hotelId);
            return Result.success(favorite);
        } catch (Exception e) {
            return Result.error("查询酒店收藏状态时发生异常，请稍后重试");
        }
    }

    @Override
    public Result<HotelFavorite> getHotelFavoriteInfoByUserIdAndHotelId(Integer hotelId) {
        // 从UserHolder获取userId并设置到scenicReservation对象
        Integer userId = BaseContext.getUserId();
        //Integer userId = 115;

        // 参数非空判断
        if (userId == null || hotelId == null) {
            return Result.error("用户ID或酒店ID不能为空");
        }

        // 检查用户是否存在
        int userCount = hotelFavoriteMapper.countUserById(userId);
        if (userCount == 0) {
            return Result.error("用户ID不存在");
        }

        // 检查酒店是否存在
        int hotelCount = hotelFavoriteMapper.countHotelById(hotelId);
        if (hotelCount == 0) {
            return Result.error("酒店ID不存在");
        }
        try {
            HotelFavorite hotelFavorite = hotelFavoriteMapper.selectByUserIdAndHotelId(userId, hotelId);
            if (hotelFavorite != null) {
                return Result.success(hotelFavorite);
            }
            return Result.success(null, "未找到对应的酒店收藏信息");
        } catch (Exception e) {
            log.error("查询酒店收藏信息时发生异常", e);
            return Result.error("查询酒店收藏信息失败，请稍后重试");
        }
    }

    @Override
    public PageResult<HotelFavoriteDTO> queryPageByUserId(Integer page, Integer pageSize) {
        // 参数非空判断
        if (page == null || pageSize == null) {
            throw new IllegalArgumentException("页码或页大小不能为空");
        }

        // 假设这里从某个地方获取userId，比如从当前登录用户信息中获取，这里先简单模拟为固定值1
        //Integer userId = 1;
        Integer userId = BaseContext.getUserId();

        // 参数非空判断
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            // 设置分页参数
            PageHelper.startPage(page, pageSize);
            // 根据userId查询用户收藏的酒店列表，并关联查询 cover 和 hotelDescription
            List<HotelFavoriteDTO> favorites = hotelFavoriteMapper.selectDtoByUserId(userId);
            // 获取分页元数据（总记录数、总页数等）
            Page<HotelFavoriteDTO> pageInfo = (Page<HotelFavoriteDTO>) favorites;
            return new PageResult<>(pageInfo.getTotal(), pageInfo.getResult());
        } catch (Exception e) {
            // 记录日志，方便排查问题
            e.printStackTrace();
            throw new RuntimeException("查询酒店收藏信息失败，请稍后重试");
        }
    }

    @Override
    public Result<List<HotelFavorite>> getHotelFavoriteInfoByUserIdAndHotelName(String hotelName) {
        // 从UserHolder获取userId并设置到scenicReservation对象
        Integer userId = BaseContext.getUserId();
        //Integer userId = 1;

        // 参数非空判断
        if (userId == null || hotelName == null) {
            return Result.error("用户ID或酒店名称不能为空");
        }

        // 检查用户是否存在
        int userCount = hotelFavoriteMapper.countUserById(userId);
        if (userCount == 0) {
            return Result.error("用户ID不存在");
        }

        // 检查酒店是否存在
        int hotelCount = hotelFavoriteMapper.countHotelByName(hotelName);
        if (hotelCount == 0) {
            return Result.error("酒店名称不存在");
        }
        try {
            List<HotelFavorite> hotelFavoriteList = hotelFavoriteMapper.selectByUserIdAndHotelName(userId, hotelName);
            if (!hotelFavoriteList.isEmpty()) {
                return Result.success(hotelFavoriteList);
            }
            return Result.error("未找到对应的酒店收藏信息");
        } catch (Exception e) {
            log.error("查询酒店收藏信息时发生异常", e);
            return Result.error("查询酒店收藏信息失败，请稍后重试");
        }
    }
}