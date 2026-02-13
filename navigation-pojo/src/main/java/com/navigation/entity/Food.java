package com.navigation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 美食实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("food")
public class Food {

    @TableId(value = "food_id", type = IdType.AUTO)
    private Integer foodId; // 美食ID

    @NotNull(message = "地区ID不能为空")
    private Integer regionId; // 地区ID，外键，关联到地区表的region_id

    @NotBlank(message = "美食名称不能为空")
    private String foodName; // 美食名称

    @NotBlank(message = "美食简介不能为空")
    private String foodDescription; // 美食简介

    @NotBlank(message = "美食详情不能为空")
    private String foodDetail; // 美食详情

    @NotBlank(message = "美食封面图片不能为空")
    private String coverImage; // 美食封面图片URL或路径

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime updateTime; // 修改时间
}