package com.anishan.content.service.impl;

import com.anishan.content.domain.entity.Faq;
import com.anishan.content.mapper.FaqMapper;
import com.anishan.content.service.FaqService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FaqServiceImpl extends ServiceImpl<FaqMapper, Faq> implements FaqService {
}
