package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.service.SubmitLogService;
import com.anishan.problem.mapper.SubmitLogMapper;
import org.springframework.stereotype.Service;

/**
* @author happy
* @description 针对表【submit_log(OJ判题提交记录)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class SubmitLogServiceImpl extends ServiceImpl<SubmitLogMapper, SubmitLog>
    implements SubmitLogService{

}




