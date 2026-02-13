package com.navigation.mapper;

import com.navigation.entity.Region;
import com.navigation.entity.ScenicFavorite;
import com.navigation.result.Result;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScenicFavoriteMapper {

    @Insert("insert into scenic_favorite (user_id, scenic_id, scenic_name, status, create_time, update_time) " +
            "values (#{userId}, #{scenicId}, #{scenicName}, #{status}, #{createTime}, #{updateTime})")
    int save(ScenicFavorite scenicFavorite);



    // 根据用户ID和景点ID删除收藏记录
    @Delete("DELETE FROM scenic_favorite WHERE user_id = #{userId} AND scenic_id = #{scenicId}")
    int deleteByUserIdAndScenicId(@Param("userId") Integer userId, @Param("scenicId") Integer scenicId);

    @Select("SELECT " +
            "sf.id, " +
            "sf.status, " +
            "sf.create_time, " +
            "sf.update_time, " +
            "sf.user_id, " +
            "sf.scenic_id, " +
            "sf.scenic_name " +
            "FROM scenic_favorite sf " +
            "WHERE sf.user_id = #{userId}")
    List<ScenicFavorite> selectByUserId(@Param("userId") Integer userId);


    @Select("SELECT COUNT(*) FROM user WHERE user_id = #{userId}")
    int countUserById(@Param("userId") Integer userId);


    // 查询景点是否存在（假设景点表名为scenic，id字段为scenic_id）
    /*@Select("SELECT COUNT(*) FROM scenic WHERE id = #{scenicId}")
    int countScenicById(@Param("scenicId") Integer scenicId);*/

    @Select("SELECT COUNT(*) > 0 FROM scenic_favorite WHERE user_id = #{userId} AND scenic_id = #{scenicId}")
    Integer existsByUserIdAndHotelId(@Param("userId") Integer userId, @Param("scenicId") Integer scenicId);



    @Select("SELECT id, status, create_time, update_time, user_id, scenic_id, scenic_name " +
            "FROM scenic_favorite " +
            "WHERE user_id = #{userId} AND scenic_id = #{scenicId}")
    ScenicFavorite selectByUserIdAndScenicId(@Param("userId") Integer userId, @Param("scenicId") Integer scenicId);


    @Select("SELECT id, status, create_time, update_time, user_id, scenic_id, scenic_name " +
            "FROM scenic_favorite " +
            "WHERE user_id = #{userId} AND scenic_name LIKE CONCAT('%', #{scenicName}, '%')")
    List<ScenicFavorite> selectByUserIdAndScenicName(@Param("userId") Integer userId, @Param("scenicName") String scenicName);
}