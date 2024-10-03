package com.anishan.user.service;

import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.user.entity.dto.UserPagedQuery;
import com.anishan.user.entity.vo.UserVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Map;

@SpringBootTest
public class SysUserServiceTest {


    @Resource
    SysUserService sysUserService;

    @Test
    public void getUserByIdTest() {
        Map<String, String> stringStringMap = MysqlMappingUtils.mapColumn(UserPagedQuery.class);
        System.out.println(stringStringMap);
    }


}
