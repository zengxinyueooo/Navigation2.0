package com.navigation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

/**
 * 酒店收藏实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelFavorite {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 收藏ID


    private Integer status; // 收藏状态（0：取消收藏，1：已收藏）


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime createTime; // 收藏时间


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime updateTime; // 更新时间


    private Integer userId; // 用户ID


    private Integer hotelId; // 酒店ID

    private String hotelName; // 酒店名称


}