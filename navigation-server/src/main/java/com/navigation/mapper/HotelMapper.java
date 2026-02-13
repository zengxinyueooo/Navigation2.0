package com.navigation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.navigation.entity.Hotel;
import com.navigation.entity.Hotel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HotelMapper extends BaseMapper<Hotel> {

    @Insert("insert into hotel(latitude, longitude, cover, region_id,  address, hotel_name, hotel_description, phone_number, create_time, update_time) " +
            "values(#{latitude}, #{longitude} ,#{cover},#{regionId}, #{address}, #{hotelName}, #{hotelDescription}, #{phoneNumber}, #{createTime}, #{updateTime})")
    void saveHotel(Hotel hotel);


    @Select("select * from hotel where id = #{id}")
    Hotel queryHotelById(Integer id);

    @Select("select * from hotel where hotel_name LIKE CONCAT('%', #{hotelName}, '%')")
    List<Hotel> queryHotelByName(String name);

    @Select("SELECT COUNT(*) FROM hotel WHERE hotel_name LIKE CONCAT('%', #{hotelName}, '%')")
    int countHotelByName(String name);

    void update(Hotel hotel);


    int batchDelete(@Param("ids") List<Integer> ids);


    @Select("select * from hotel ")
    List<Hotel> queryHotel(Integer pageNum, Integer pageSize);

    @Select("SELECT COUNT(*) FROM hotel WHERE id = #{id}")
    int countHotelById(Integer id);

    @Select("SELECT id FROM hotel")
    List<Integer> getAllExistingHotelIds();

    @Select("select * from hotel where region_id = #{regionId}")
    List<Hotel> queryHotelByRegionId(Integer regionId);

    @Select("<script>" +
            "SELECT * FROM hotel WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Hotel> queryHotelListByIds(@Param("ids") List<Integer> ids);

    // 根据酒店名称或地区查询酒店信息
    @Select("SELECT * FROM hotel WHERE hotel_name LIKE CONCAT('%', #{query}, '%') OR address LIKE CONCAT('%', #{query}, '%')")
    List<Hotel> searchHotels(@Param("query") String query);
}
