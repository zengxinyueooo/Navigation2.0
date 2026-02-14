package com.navigation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.navigation.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
