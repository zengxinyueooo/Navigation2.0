package com.navigation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.navigation.dto.TicketReservationWithScenicDTO;
import com.navigation.entity.TicketReservation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TicketReservationMapper extends BaseMapper<TicketReservation> {


    @Insert("insert into ticket_reservation(user_id, ticket_id, quantity, status, " +
            "reservation_time, total_price, create_time, update_time) " +
            "values(#{userId}, #{ticketId}, #{quantity}, #{status}," +
            "        #{reservationTime},#{totalPrice}, #{createTime},#{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "reservationId", keyColumn = "reservation_id")
    void saveTicketReservation(TicketReservation ticketReservation);


    void update(TicketReservation ticketReservation);

    int batchDelete(@Param("ids") List<Integer> ids);


    /*@Select("select * from ticket_reservation ")
    List<TicketReservation> queryTicketReservation(Integer pageNum, Integer pageSize);*/

    @Select("select * from ticket_reservation where reservation_id = #{reservationId}")
    TicketReservation queryTicketReservationById(Integer id);


    @Update("UPDATE ticket_reservation SET status = 1 WHERE reservation_id = #{reservationId}")
    int updateStatusById(Integer reservationId);

    @Select("SELECT COUNT(1) FROM ticket_reservation WHERE user_id = #{userId} AND ticket_id = #{ticketId}")
    boolean existsByUserIdAndTicketId(Integer userId, Integer ticketId);

    // 获取数据库中所有存在的ID列表
    @Select("SELECT reservation_id FROM ticket_reservation")
    List<Integer> getAllExistingIds();


    int updateSelective(TicketReservation updateEntity);

    @Select("SELECT " +
            "tr.reservation_id, " +
            "tr.reservation_time, " +
            "s.scenicCover, " +
            "s.scenicName, " +
            "s.scenicLocateDescription " +
            "FROM " +
            "ticket_reservation tr " +
            "JOIN " +
            "scenic s ON tr.scenic_id = s.id " +
            "WHERE " +
            "tr.user_id = #{userId} " +
            "LIMIT #{pageNum}, #{pageSize}")
    List<TicketReservationWithScenicDTO> queryTicketReservationWithScenicByUserId(Integer userId, Integer pageNum, Integer pageSize);

    @Select("select * from ticket_reservation where user_id = #{userId}")
    List<TicketReservation> queryTicketReservationByUserId(Integer userId, Integer pageNum, Integer pageSize);
}
