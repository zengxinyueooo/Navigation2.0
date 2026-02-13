package com.navigation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScenicFavoriteDTO {
    // 原有ScenicFavorite的字段
    private Long id;
    private Integer userId;
    private Integer scenicId;
    private String scenicName; // 景点名称
    private LocalDateTime createTime; // 收藏时间
    // 其他你需要的原有字段...

    // 新增从Redis获取的字段
    private String scenicCover;
    private String scenicDescription;

}