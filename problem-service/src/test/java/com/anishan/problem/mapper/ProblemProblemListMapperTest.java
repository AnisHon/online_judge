package com.anishan.problem.mapper;


import com.anishan.api.client.user.client.UserClient;
import com.anishan.problem.domain.entity.Records;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@SpringBootTest
public class ProblemProblemListMapperTest {


//    @Resource
//    private ProblemProblemListMapper problemProblemListMapper;



    @Resource
    RecordsMapper recordsMapper;
    @Autowired
    private UserClient userClient;

    @Test
    public void getListTest() {
//        List<ProblemListRelation> byListId = problemProblemListMapper.getByListId(1);
//        System.out.println(byListId);

        MPJLambdaWrapper<Records> wrapper = new MPJLambdaWrapper<Records>()
                .select("user_id")
                .selectSum(Records::getScore, "score")
                .groupBy(Records::getRecordId)
                .orderByAsc("score");

        List<Map<String, Object>> maps = recordsMapper.selectJoinMaps(wrapper);

        List<Object> userId = maps.stream().map(x -> x.get("user_id")).collect(Collectors.toList());

        System.out.println(userId.get(0).getClass().getName());
    }

}
