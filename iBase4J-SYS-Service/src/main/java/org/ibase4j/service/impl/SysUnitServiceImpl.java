package org.ibase4j.service.impl;

import org.ibase4j.mapper.SysUnitMapper;
import org.ibase4j.model.SysUnit;
import org.ibase4j.service.ISysUnitService;
import org.springframework.cache.annotation.CacheConfig;

import com.alibaba.dubbo.config.annotation.Service;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;

import top.ibase4j.core.base.BaseService;

/**
 * @author ShenHuaJie
 *
 */
@CacheConfig(cacheNames = "sysUnit")
@Service(interfaceClass = ISysUnitService.class)
@MotanService(interfaceClass = ISysUnitService.class)
public class SysUnitServiceImpl extends BaseService<SysUnit, SysUnitMapper> implements ISysUnitService {

}
