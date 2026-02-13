/*
import com.alibaba.druid.pool.DruidDataSource;
import com.navigation.utils.DruidConfig;
import com.navigation.utils.JwtUtils;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

public class DruidDataSourceTest {

    @Test
    public void testDruidDataSourceConnection() {

        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTUiLCJyb2xlIjoidXNlciIsImlhdCI6MTc0NTA2NjA4NSwiZXhwIjoxNzQ1MTUyNDg1fQ.F_XfrKD8PkMj0QIZJnZ9jNmlmh9cs2lNUHNNDi6Egbc";
        Integer userId = JwtUtils.getUserId(token);
        if (userId != null) {
            System.out.println("解析得到的用户 ID 是: " + userId);
        } else {
            System.out.println("无法解析出用户 ID，请检查 Token 是否有效。");
        }
    }
}
*/
