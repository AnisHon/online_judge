package com.anishan.problem.service.impl;

import com.anishan.problem.domain.entity.JudgeCaseLog;
import com.anishan.problem.mapper.JudgeCaseLogMapper;
import com.anishan.problem.service.JudgeCaseLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class JudgeCaseLogServiceImpl extends ServiceImpl<JudgeCaseLogMapper, JudgeCaseLog>
        implements JudgeCaseLogService {
}
