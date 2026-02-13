package com.navigation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.navigation.entity.Food;
import com.navigation.entity.Food;
import com.navigation.entity.Region;
import com.navigation.entity.Scenic;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FoodMapper extends BaseMapper<Food> {


    @Insert("insert into food(food_name, cover_image, region_id, food_description, " +
            "food_detail, create_time, update_time) " +
            "values(#{foodName}, #{coverImage}, #{regionId}, #{foodDescription}," +
            "        #{foodDetail}, #{createTime},#{updateTime})")
    void saveFood(Food food);


    void update(Food food);

    int batchDelete(@Param("ids") List<Integer> ids);

    @Select("select * from food where food_id = #{foodId}")
    Food queryFoodById(Integer id);
    @Select("select * from food ")
    List<Food> queryFood(Integer pageNum, Integer pageSize);

    @Select("select * from food where region_id = #{regionId}")
    List<Food> queryFoodByRegionId(Integer id);

    @Select("SELECT COUNT(*) FROM region WHERE region_id = #{regionId}")
    int countFoodById(Integer regionId);

    @Select("SELECT food_id FROM food")
    List<Integer> getAllExistingIds();


    // 根据地区查询美食信息（不包括价格）
    @Select("SELECT * FROM food WHERE region_id IN (SELECT region_id FROM region WHERE region_name = #{region})")
    List<Food> findByRegion(@Param("region") String region);
}
