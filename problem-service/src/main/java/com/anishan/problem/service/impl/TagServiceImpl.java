package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Tag;
import com.anishan.problem.service.TagService;
import com.anishan.problem.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author happy
* @description 针对表【tag(题目标签表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




