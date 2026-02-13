package com.navigation.entity;

import lombok.Data;

@Data
public class ScenicComment {
    private Integer scenicId;
    private String scenicName;
    private Comment comment;  // 使用您现有的Comment类
    // 可以根据需要添加其他字段，如评分、用户信息等
}