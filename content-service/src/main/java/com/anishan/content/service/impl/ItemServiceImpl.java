package com.anishan.content.service.impl;

import com.anishan.content.domain.entity.Item;
import com.anishan.content.mapper.ItemMapper;
import com.anishan.content.service.ItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 轮播图表 服务实现类
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements ItemService {

}
