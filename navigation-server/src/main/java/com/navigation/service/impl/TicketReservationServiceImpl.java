package com.navigation.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.navigation.context.BaseContext;
import com.navigation.dto.TicketReservationWithScenicDTO;
import com.navigation.entity.Scenic;
import com.navigation.entity.ScenicFavorite;
import com.navigation.entity.Ticket;
import com.navigation.entity.TicketReservation;
import com.navigation.handler.OrderWebSocketHandler;
import com.navigation.mapper.TicketMapper;
import com.navigation.mapper.TicketReservationMapper;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.TicketReservationService;
import com.navigation.utils.JsonUtils;
import io.swagger.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TicketReservationServiceImpl extends ServiceImpl<TicketReservationMapper, TicketReservation> implements TicketReservationService {


    @Autowired
    private TicketReservationMapper ticketReservationMapper;


    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    ObjectMapper objectMapper = JsonUtils.getObjectMapper();

    @Autowired
    private OrderWebSocketHandler orderWebSocketHandler; // 注入 OrderWebSocketHandler



    @Override
    @Transactional
    public Result<Integer> saveTicketReservation(TicketReservation ticketReservation) {
        // 参数校验
        if (ticketReservation == null) {
            log.error("传入的 TicketReservation 对象为空，无法保存景点预约信息");
            return Result.error("传入的景点预约信息为空");
        }

        // 从UserHolder获取userId并设置到ticketReservation对象
        Integer userId = 2; //
        //Integer userId = BaseContext.getUserId();
        ticketReservation.setUserId(userId);

        // 检查用户是否存在
        int count = ticketMapper.countUserById(userId);
        if (count == 0) {
            log.error("用户ID: {} 不存在，无法保存景点预订信息", userId);
            return Result.error("用户ID对应的用户不存在，请检查");
        }

        // 检查ticketId是否存在
        Integer ticketId = ticketReservation.getTicketId();
        if (ticketId == null) {
            log.error("门票ID为空，无法获取门票单价计算总价");
            return Result.error("门票ID不能为空");
        }
        Ticket ticket = ticketMapper.queryTicketById(ticketId);
        if (ticket == null) {
            log.error("未找到ID为 {} 的门票信息，无法计算总价", ticketId);
            return Result.error("未找到对应的门票信息");
        }

        // 检查购买数量是否为空
        Integer buyNumber = ticketReservation.getQuantity();
        if (buyNumber == null) {
            log.error("购买数量为空，无法计算总价");
            return Result.error("购买数量不能为空");
        }

        // 新增：检查用户是否已经预约过该门票
        boolean existsReservation = ticketReservationMapper.existsByUserIdAndTicketId(userId, ticketId);
        if (existsReservation) {
            log.error("用户ID: {} 已经预约过门票ID: {}，不能重复预约", userId, ticketId);
            return Result.error("您已经预约过该门票，不能重复预约");
        }

        // 计算总价
        BigDecimal ticketPrice = ticket.getPrice();
        BigDecimal totalPrice = ticketPrice.multiply(new BigDecimal(buyNumber));

        // 获取门票库存
        int stock = ticket.getAvailability();
        if (buyNumber > stock) {
            log.error("购买数量 {} 超过门票库存 {}", buyNumber, stock);
            return Result.error("购买数量超过门票库存");
        }

        // 使用Validator校验除userId和ticketId外的其他字段
        Set<ConstraintViolation<TicketReservation>> violations = validator.validate(ticketReservation);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("以下字段校验不通过: ");
            for (ConstraintViolation<TicketReservation> violation : violations) {
                errorMessage.append(violation.getPropertyPath()).append(" - ").append(violation.getMessage()).append("; ");
            }
            log.error(errorMessage.toString());
            return Result.error(errorMessage.toString());
        }

        try {
            ticketReservation.setTotalPrice(totalPrice);
            ticketReservation.setStatus(0);
            ticketReservation.setCreateTime(LocalDateTime.now());
            ticketReservation.setUpdateTime(LocalDateTime.now());
            ticketReservationMapper.saveTicketReservation(ticketReservation);
            Integer reservationId = ticketReservation.getReservationId(); // 假设保存后会自动填充ID

            handleOrderSuccess(userId);//实时推送
            // 更新门票库存
            ticket.setAvailability(stock - buyNumber);
            ticketMapper.updateTicketStock(ticket);

            return Result.success(reservationId);
        } catch (Exception e) {
            log.error("保存景点预约信息时出现异常", e);
            return Result.error("保存景点预约信息失败，请稍后重试");
        }
    }



    // 处理订单成功并推送消息，可以自定义构建返回的数据
    public void handleOrderSuccess(Integer userId) {
        String successMessage = "您的预约已成功！";
        orderWebSocketHandler.sendMessageToUser(userId, successMessage);  // 调用 sendMessageToUser 向用户推送成功消息
    }

    @Override
    public Result<Void> update(TicketReservation ticketReservation) {
        try {
            // 基础校验
            if (ticketReservation == null) {
                log.error("传入的预约信息为空");
                return Result.error("预约信息不能为空");
            }

            // 强制要求reservationId
            Integer reservationId = ticketReservation.getReservationId();
            if (reservationId == null) {
                log.error("缺少必要参数reservationId");
                return Result.error("必须提供预约订单ID");
            }

            // 获取当前用户ID
            //Integer userId = 1; //
            Integer userId = BaseContext.getUserId();
            if (userId == null) {
                log.error("用户未登录");
                return Result.error("请先登录");
            }

            // 获取原始预约记录
            TicketReservation originalReservation = ticketReservationMapper.queryTicketReservationById(reservationId);
            if (originalReservation == null) {
                log.error("预约记录不存在，reservationId: {}", reservationId);
                return Result.error("预约记录不存在");
            }

            // 校验用户权限
            if (!originalReservation.getUserId().equals(userId)) {
                log.error("用户{}尝试修改他人预约记录{}", userId, reservationId);
                return Result.error("无权修改该预约记录");
            }

            // 新门票ID校验（如果传入了新的ticketId）
            Integer newTicketId = ticketReservation.getTicketId();
            if (newTicketId != null) {
                // 检查新门票是否存在
                Ticket newTicket = ticketMapper.queryTicketById(newTicketId);
                if (newTicket == null) {
                    log.error("指定门票不存在，ticketId: {}", newTicketId);
                    return Result.error("选择的门票不存在");
                }

                // 如果修改了门票ID，需要检查是否重复预约（同一用户同门票只能有一条）
                boolean exists = ticketReservationMapper.existsByUserIdAndTicketId(userId, newTicketId);
                if (exists) {
                    log.error("用户{}已存在门票{}的预约", userId, newTicketId);
                    return Result.error("您已预约过该门票");
                }
            }

            // 构建更新对象
            TicketReservation updateEntity = new TicketReservation();
            updateEntity.setReservationId(reservationId);

            // 字段更新逻辑
            boolean needUpdate = false;

            // 1. 处理数量变更
            Integer newQuantity = ticketReservation.getQuantity();
            if (newQuantity != null && newQuantity > 0) {
                if (!newQuantity.equals(originalReservation.getQuantity())) {
                    updateEntity.setQuantity(newQuantity);
                    needUpdate = true;
                }
            }

            // 2. 处理预约时间变更
            LocalDateTime newReservationTime = ticketReservation.getReservationTime();
            if (newReservationTime != null) {
                if (!newReservationTime.equals(originalReservation.getReservationTime())) {
                    updateEntity.setReservationTime(newReservationTime);
                    needUpdate = true;
                }
            }

            // 3. 处理门票ID变更
            if (newTicketId != null && !newTicketId.equals(originalReservation.getTicketId())) {
                updateEntity.setTicketId(newTicketId);
                needUpdate = true;
            }

            // 无有效修改时提前返回
            if (!needUpdate) {
                log.warn("未检测到有效修改，reservationId: {}", reservationId);
                return Result.error("未提交任何有效修改");
            }

            // 重新计算总价（以下情况需要计算）:
            // - 修改了门票ID（价格可能不同）
            // - 修改了数量
            boolean needRecalculate = updateEntity.getTicketId() != null || updateEntity.getQuantity() != null;
            if (needRecalculate) {
                // 获取最终使用的门票信息
                Ticket finalTicket = updateEntity.getTicketId() != null ?
                        ticketMapper.queryTicketById(updateEntity.getTicketId()) :
                        ticketMapper.queryTicketById(originalReservation.getTicketId());

                // 获取最终使用的数量
                Integer finalQuantity = updateEntity.getQuantity() != null ?
                        updateEntity.getQuantity() :
                        originalReservation.getQuantity();

                BigDecimal totalPrice = finalTicket.getPrice().multiply(new BigDecimal(finalQuantity));
                updateEntity.setTotalPrice(totalPrice);
            }

            // 设置更新时间
            updateEntity.setUpdateTime(LocalDateTime.now());

            // 执行更新
            int affectedRows = ticketReservationMapper.updateSelective(updateEntity);
            if (affectedRows == 0) {
                log.error("数据库更新失败，reservationId: {}", reservationId);
                return Result.error("更新失败，请稍后重试");
            }

            return Result.success();
        } catch (Exception e) {
            log.error("预约更新异常", e);
            return Result.error("系统异常，请稍后重试");
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
            List<Integer> allExistingIds = ticketReservationMapper.getAllExistingIds();
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
            int rows = ticketReservationMapper.batchDelete(ids);
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
    public PageResult queryTicketReservation(Integer pageNum, Integer pageSize) {
        // 从BaseContext获取当前用户ID
        Integer userId = BaseContext.getUserId();
        //Integer userId = 1;
        try {
            // 设置分页参数
            PageHelper.startPage(pageNum, pageSize);
            // 根据用户ID查询票务预订记录
            List<TicketReservation> ticketList = ticketReservationMapper.queryTicketReservationByUserId(userId, pageNum, pageSize);
            Page<TicketReservation> pageInfo = (Page<TicketReservation>) ticketList;

            // 转换为DTO并补充Redis数据
            List<TicketReservationWithScenicDTO> dtoList = ticketList.stream().map(ticketReservation -> {
                TicketReservationWithScenicDTO dto = new TicketReservationWithScenicDTO();
                // 复制原有属性
                BeanUtils.copyProperties(ticketReservation, dto);

                Integer ticketId = ticketReservation.getTicketId();
                Ticket ticket = ticketMapper.findTicketByTicketId(ticketId);
                if (ticket != null) {
                    Integer scenicSpotId = ticket.getScenicSpotId();
                    dto.setScenicId(scenicSpotId);

                    // 模糊查询Redis中的景点信息
                    String pattern = "scenic:" + scenicSpotId + ":*";
                    Set<String> keys = stringRedisTemplate.keys(pattern);
                    if (keys.isEmpty()) {
                        log.error("未在Redis中找到景点Id为 {} 的记录", scenicSpotId);
                    } else {
                        String key = keys.iterator().next();
                        String scenicJson = stringRedisTemplate.opsForValue().get(key);
                        if (scenicJson != null) {
                            try {
                                Scenic scenic = objectMapper.readValue(scenicJson, Scenic.class);
                                if (scenic != null && scenic.getId() != null) {
                                    dto.setScenicCover(scenic.getScenicCover());
                                    dto.setScenicName(scenic.getScenicName());
                                    dto.setScenicLocateDescription(scenic.getScenicLocateDescription());
                                } else {
                                    log.error("从Redis获取的景点数据不完整或格式错误，景点Id: {}", scenicSpotId);
                                }
                            } catch (Exception e) {
                                log.error("解析Redis中的景点信息出错，景点Id: {}", scenicSpotId, e);
                            }
                        }
                    }
                }
                return dto;
            }).collect(Collectors.toList());

            return new PageResult<>(pageInfo.getTotal(), dtoList);
        } catch (Exception e) {
            log.error("查询票务预订记录失败，userId: {}", userId, e);
            throw new RuntimeException("查询票务预订记录失败，请稍后重试");
        }
    }


    /*@Override
    public Result<TicketReservation> queryTicketReservationById(Integer id) {
        if (id == null) {
            log.error("传入的景点预约ID为空");
            return Result.error("id为空！");
        }

        try {
            TicketReservation ticketReservation = ticketReservationMapper.queryTicketReservationById(id);
            if (ticketReservation == null) {
                log.error("景点预约id为 {} 的记录不存在", id);
                return Result.error("id不存在！");
            }
            return Result.success(ticketReservation);
        } catch (Exception e) {
            log.error("根据id查询景点预约信息时发生异常", e);
            return Result.error("根据id查询景点预约信息失败，请稍后重试");
        }
    }*/

    @Override
    public Result<Void> confirmPurchase(Integer reservationId) {
        if (reservationId == null) {
            log.error("传入的预订ID为空");
            return Result.error("预订ID不能为空");
        }
        try {
            // 检查预订ID是否存在
            TicketReservation existingReservation = ticketReservationMapper.queryTicketReservationById(reservationId);
            if (existingReservation == null) {
                log.error("预订ID为 {} 的记录不存在", reservationId);
                return Result.error("预订ID对应的记录不存在");
            }
            int updatedRows = ticketReservationMapper.updateStatusById(reservationId);
            if(updatedRows > 0){
                //门票预约Id-->门票Id-->门票数量减量
                TicketReservation ticketReservation = ticketReservationMapper.queryTicketReservationById(reservationId);
                Integer ticketId = ticketReservation.getTicketId();
                if (ticketId == null) {
                    log.error("门票ID为空，无法获取门票单价计算总价");
                    return Result.error("门票ID不能为空");
                }
                Ticket ticket = ticketMapper.queryTicketById(ticketId); // 根据ID查询门票信息
                if (ticket == null) {
                    return Result.error("未找到对应的门票信息");
                }
                Integer buyNumber = ticketReservation.getQuantity();
                Integer num = ticketMapper.updateTicketAvailability(ticketId, buyNumber); //减少票的数量
                if(num == 0){
                    return Result.error("剩余门票数量不足！");
                }
                return Result.success();
            }
            else{
                return Result.error("支付失败");
            }
        } catch (Exception e) {
            log.error("更新预约状态时出现异常，预约ID：" + reservationId, e);
            return Result.error("更新预约状态时出现异常，预约ID：" + reservationId);
        }
    }


}
