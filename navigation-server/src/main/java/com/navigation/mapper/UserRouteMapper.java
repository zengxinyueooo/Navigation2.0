package com.navigation.mapper;

import com.navigation.entity.UserRoute;
import org.apache.ibatis.annotations.*;

import java.sql.Timestamp;
import java.util.List;

/**
 * 用户路线数据访问层接口
 * 负责对 user_routes 表的增删查操作
 */
@Mapper
public interface UserRouteMapper {

    /**
     * 插入一条用户路线记录
     * @param route 要插入的用户路线对象
     */
    @Insert("INSERT INTO user_routes (user_id, origin_name, destination_name, travel_mode, create_time) " +
            "VALUES (#{userId}, #{originName}, #{destinationName}, #{travelMode}, #{createTime})")
    void insert(UserRoute route);

    /**
     * 根据用户ID查询该用户的所有路线记录，按创建时间倒序排列，并支持分页
     * @param userId 用户ID
     * @return 该用户的所有路线列表
     */
    @Select({
            "<script>",
            "SELECT * FROM user_routes WHERE user_id = #{userId} ORDER BY create_time DESC",
            "</script>"
    })
    List<UserRoute> findByUserIdPaged(@Param("userId") Integer userId);


    /**
     * 根据路线ID删除一条路线记录
     * @param id 路线主键ID
     */
    @Delete("DELETE FROM user_routes WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    /**
     * 根据路线 ID 查询用户路线
     * @param routeId 路线 ID
     * @return 用户路线对象
     */
    @Select("SELECT * FROM user_routes WHERE id = #{routeId}")
    UserRoute findById(@Param("routeId") Long routeId);

    /**
     * 综合搜索用户的路线记录，可按关键词和时间范围过滤，并支持分页
     * @param userId 用户ID（必须）
     * @param keyword 关键词，用于模糊匹配起点/终点（可选）
     * @param startTime 起始时间，创建时间 >= startTime（可选）
     * @param endTime 截止时间，创建时间 <= endTime（可选）
     * @return 搜索结果列表
     */
    @Select({
            "<script>",
            "SELECT * FROM user_routes WHERE user_id = #{userId}",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (origin_name LIKE CONCAT('%', #{keyword}, '%') OR destination_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "<if test='startTime != null'>",
            "AND create_time &gt;= #{startTime}",
            "</if>",
            "<if test='endTime != null'>",
            "AND create_time &lt;= #{endTime}",
            "</if>",
            "ORDER BY create_time DESC",
            "</script>"
    })
    List<UserRoute> searchRoutesPaged(@Param("userId") Integer userId,
                                      @Param("keyword") String keyword,
                                      @Param("startTime") Timestamp startTime,
                                      @Param("endTime") Timestamp endTime);

}