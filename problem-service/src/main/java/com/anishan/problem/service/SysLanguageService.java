package com.anishan.problem.service;

import com.anishan.problem.domain.entity.SysLanguage;
import com.anishan.problem.domain.vo.SysLanguageVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author happy
* @description 针对表【sys_language(编程语言表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface SysLanguageService extends IService<SysLanguage> {

    List<SysLanguageVo> listAll();

}
