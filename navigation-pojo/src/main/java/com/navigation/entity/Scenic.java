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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

  /**
 * 用户实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("scenic")
public class Scenic {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @NotBlank(message = "景区名称不能为空")
    private String scenicName;

    @NotBlank(message = "景区封面不能为空")
    private String scenicCover;

    private String scenicFavoriteIds;

    private Integer scenicStatus;

    @NotBlank(message = "景区描述不能为空")
    private String scenicDescription;

    private String scenicLocateDescription;

    @NotNull(message = "景区最大容量不能为空")
    private Integer maxCapacity;

    //预约的景点的总人数
    private Integer totalPeople;

    //星级 5A 4A
    private  Integer  scenicStar;

    //景点评分
    private double averageMark;

    //评分次数
    private Integer rateCount;

    //评分用户
    private List<Integer> ratedUsers;

    //景点评价
    private Map<String, Comment> comments;


    @NotNull(message = "景区开放时间不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime openStartTime;

    @NotNull(message = "景区关闭时间不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime openEndTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    LocalDateTime createTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    LocalDateTime updateTime;

    private Integer regionId; // 地区ID

}
