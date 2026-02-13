package com.navigation.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRoute {
    private Long id;
    @JsonIgnore// 主键ID
    private Integer userId;         // 用户ID

    private String originName;      // 起点名称
    private String destinationName; // 终点名称
    private String travelMode;      // 出行方式
    @JsonIgnore
    @JsonFormat(pattern = "yyyy年MM月dd日 HH:mm", timezone = "GMT+8")
    private Timestamp createTime;   // 创建时间
}