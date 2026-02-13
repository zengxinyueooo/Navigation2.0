  package com.navigation.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

  /**
   * 用户实体类
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @TableName("hotel")
  public class Hotel {
      @TableId(value = "id", type = IdType.AUTO)
      private Integer id;

      private Integer regionId;

      @NotBlank(message = "酒店封面不能为空")
      private String cover;

      @NotBlank(message = "酒店地点不能为空")
      private String address;

      @NotBlank(message = "酒店描述不能为空")
      private String hotelDescription;

      @NotBlank(message = "酒店电话不能为空")
      private String phoneNumber;

      @NotBlank(message = "酒店名称不能为空")
      private String hotelName;

      private Double latitude;

      //double的包装类
      private Double longitude;




      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
      LocalDateTime createTime;

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
      LocalDateTime updateTime;

      private Integer rateCount = 0;      // 评分人数（默认0）

      private Double averageMark = 0.0;   // 平均评分（默认0.0）

      private String ratedUserIds = "";   // 已评分用户ID列表（用逗号分隔）

  }
