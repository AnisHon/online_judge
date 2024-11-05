package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import com.anishan.problem.domain.vo.SysLanguageVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.SysLanguage;
import com.anishan.problem.service.SysLanguageService;
import com.anishan.problem.mapper.SysLanguageMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author happy
* @description 针对表【sys_language(编程语言表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class SysLanguageServiceImpl extends ServiceImpl<SysLanguageMapper, SysLanguage>
    implements SysLanguageService{

    private Map<Long, SysLanguageVo> languageCache;

    private List<SysLanguageVo> doGetLanguage() {
        if (languageCache == null) {
            languageCache = new HashMap<>();
            List<SysLanguage> list = this.list();
            List<SysLanguageVo> vos = BeanUtil.copyToList(list, SysLanguageVo.class);
            vos.forEach(x -> languageCache.put(x.getLanguageId(), x));
        }

        return ListUtil.toList(languageCache.values());
    }

    @Override
    public List<SysLanguageVo> listAll() {
        List<SysLanguage> list = this.list();
        return BeanUtil.copyToList(list, SysLanguageVo.class);
    }
}




