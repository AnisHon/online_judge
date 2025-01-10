package com.anishan.problem.service.impl;

import com.anishan.problem.domain.entity.ContestAnswerRecords;
import com.anishan.problem.mapper.ContestAnswerRecordsMapper;
import com.anishan.problem.service.ContestAnswerRecordsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 比赛完成记录表，分表专门用于存储答案 服务实现类
 * </p>
 *
 * @author anishan
 * @since 2025-01-07
 */
@Service
public class ContestAnswerRecordsServiceImpl extends ServiceImpl<ContestAnswerRecordsMapper, ContestAnswerRecords> implements ContestAnswerRecordsService {

}
