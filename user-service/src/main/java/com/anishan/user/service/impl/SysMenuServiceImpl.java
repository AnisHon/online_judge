package com.anishan.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.entity.po.SysMenu;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

/**
* @author anishan
* @description 针对表【sys_menu(菜单权限表)】的数据库操作Service实现
* @createDate 2024-10-03 01:02:49
*/
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
    implements SysMenuService{

}




