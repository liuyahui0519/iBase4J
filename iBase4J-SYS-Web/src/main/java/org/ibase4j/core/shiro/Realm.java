package org.ibase4j.core.shiro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.ibase4j.model.SysSession;
import org.ibase4j.model.SysUser;
import org.ibase4j.service.ISysAuthorizeService;
import org.ibase4j.service.ISysSessionService;
import org.ibase4j.service.ISysUserService;
import org.springframework.stereotype.Component;

import top.ibase4j.core.support.shiro.IRealm;
import top.ibase4j.core.support.shiro.RedisSessionDAO;
import top.ibase4j.core.util.SecurityUtil;
import top.ibase4j.core.util.ShiroUtil;

/**
 * 权限检查类
 * 
 * @author ShenHuaJie
 * @version 2016年5月20日 下午3:44:45
 */
@Component
public class Realm extends AuthorizingRealm implements IRealm {
    private final Logger logger = LogManager.getLogger();
    @Resource
    private ISysAuthorizeService sysAuthorizeService;
    @Resource
    private ISysUserService sysUserService;
    @Resource
    private ISysSessionService sysSessionService;

    private RedisSessionDAO sessionDAO;

    public void setSessionDAO(RedisSessionDAO sessionDAO) {
        this.sessionDAO = sessionDAO;
    }

    // 权限
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        Long userId = (Long)ShiroUtil.getCurrentUser();
        List<?> list = sysAuthorizeService.queryPermissionByUserId(userId);
        for (Object permission : list) {
            if (StringUtils.isNotBlank((String)permission)) {
                // 添加基于Permission的权限信息
                info.addStringPermission((String)permission);
            }
        }
        // 添加用户权限
        info.addStringPermission("user");
        return info;
    }

    // 登录验证
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken)
        throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken)authcToken;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("enable", 1);
        params.put("account", token.getUsername());
        List<?> list = sysUserService.queryList(params);
        if (list.size() == 1) {
            SysUser user = (SysUser)list.get(0);
            StringBuilder sb = new StringBuilder(100);
            for (int i = 0; i < token.getPassword().length; i++) {
                sb.append(token.getPassword()[i]);
            }
            if (user.getPassword().equals(SecurityUtil.encryptPassword(sb.toString()))) {
                ShiroUtil.saveCurrentUser(user.getId());
                saveSession(user.getAccount(), token.getHost());
                AuthenticationInfo authcInfo = new SimpleAuthenticationInfo(user.getAccount(), sb.toString(),
                    user.getUserName());
                return authcInfo;
            }
            logger.warn("USER [{}] PASSWORD IS WRONG: {}", token.getUsername(), sb.toString());
            return null;
        } else {
            logger.warn("No user: {}", token.getUsername());
            return null;
        }
    }

    /** 保存session */
    private void saveSession(String account, String host) {
        // 踢出用户
        SysSession record = new SysSession();
        record.setAccount(account);
        List<?> sessionIds = sysSessionService.querySessionIdByAccount(record);
        Subject currentUser = SecurityUtils.getSubject();
        Session session = currentUser.getSession();
        String currentSessionId = session.getId().toString();
        if (sessionIds != null) {
            for (Object sessionId : sessionIds) {
                record.setSessionId((String)sessionId);
                sysSessionService.deleteBySessionId(record);
                if (!currentSessionId.equals(sessionId)) {
                    sessionDAO.delete((String)sessionId);
                }
            }
        }
        // 保存用户
        record.setSessionId(currentSessionId);
        record.setIp(StringUtils.isBlank(host) ? session.getHost() : host);
        record.setStartTime(session.getStartTimestamp());
        sysSessionService.update(record);
    }
}
