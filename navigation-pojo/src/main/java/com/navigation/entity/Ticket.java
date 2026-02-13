package com.navigation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门票实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ticket")
public class Ticket {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 门票ID

    @NotNull(message = "景点ID不能为空")
    private Integer scenicSpotId; // 景点ID（外键，关联到景点表的id）

    @NotBlank(message = "票种类型不能为空")
    private String ticketType; // 票种类型（如成人票、儿童票、学生票、老年票等）


    @NotNull(message = "门票价格不能为空")
    private BigDecimal price; // 门票价格

    @NotNull(message = "门票数量不能为空")
    private Integer availability; // 门票剩余数量

    @NotNull(message = "门票开放时间不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime validFrom; // 门票有效期开始时间

    @NotNull(message = "门票开放时间不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime validTo; // 门票有效期结束时间

    private String version; // 乐观锁版本号字段

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    LocalDateTime createTime; // 创建时间

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
     LocalDateTime updateTime; // 修改时间
}