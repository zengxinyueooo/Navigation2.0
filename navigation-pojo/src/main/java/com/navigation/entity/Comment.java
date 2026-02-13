package com.navigation.entity;

import lombok.Data;

@Data
public class Comment {
    private String content;
    private Integer userId;
    // 可以添加更多评价相关的属性，如评价时间等
    private String nickName;
    private String head;
}