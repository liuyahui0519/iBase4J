package org.ibase4j.service.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ibase4j.mapper.SysDicMapper;
import org.ibase4j.model.SysDic;
import org.ibase4j.service.ISysDicService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

import com.alibaba.dubbo.config.annotation.Service;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;

import top.ibase4j.core.Constants;
import top.ibase4j.core.base.BaseService;
import top.ibase4j.core.util.InstanceUtil;

/**
 * @author ShenHuaJie
 * @version 2016年5月20日 下午3:19:19
 */
@CacheConfig(cacheNames = "sysDic")
@Service(interfaceClass = ISysDicService.class)
@MotanService(interfaceClass = ISysDicService.class)
public class SysDicServiceImpl extends BaseService<SysDic, SysDicMapper> implements ISysDicService {

	@Cacheable(value = Constants.CACHE_NAMESPACE + "sysDics")
	public Map<String, Map<String, String>> getAllDic() {
		Map<String, Object> params = InstanceUtil.newHashMap();
		params.put("orderBy", "type_,sort_no");
		List<SysDic> list = queryList(params);
		Map<String, Map<String, String>> resultMap = InstanceUtil.newHashMap();
		for (SysDic sysDic : list) {
			if (sysDic != null) {
				String key = sysDic.getType();
				if (resultMap.get(key) == null) {
					Map<String, String> dicMap = InstanceUtil.newHashMap();
					resultMap.put(key, dicMap);
				}
				if (StringUtils.isNotBlank(sysDic.getParentCode())) {
					resultMap.get(key).put(sysDic.getParentCode() + sysDic.getCode(), sysDic.getCodeText());
				} else {
					resultMap.get(key).put(sysDic.getCode(), sysDic.getCodeText());
				}
			}
		}
		return resultMap;
	}

	@Cacheable(value = Constants.CACHE_NAMESPACE + "sysDics")
	public Map<String, String> queryDicByTypeMap(Map<String, Object> params) {
		return queryDicByType((String) params.get("type"));
	}

	@Cacheable(value = Constants.CACHE_NAMESPACE + "sysDics")
	public Map<String, String> queryDicByType(String key) {
		Map<String, Object> params = InstanceUtil.newHashMap();
		params.put("type", key);
		List<SysDic> list = queryList(params);
		Map<String, String> resultMap = InstanceUtil.newHashMap();
		for (SysDic sysDic : list) {
			resultMap.put(sysDic.getCode(), sysDic.getCodeText());
		}
		return resultMap;
	}
}
