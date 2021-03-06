package com.zjy.oauth2server.config.service;

import com.zjy.oauth2server.integration.IntegrationAuthentication;
import com.zjy.oauth2server.integration.IntegrationAuthenticationContext;
import com.zjy.oauth2server.integration.IntegrationAuthenticator;
import com.zjy.oauth2server.pojo.entity.oauth2.Authorize;
import com.zjy.oauth2server.pojo.entity.oauth2.SysUserAuthentication;
import com.zjy.oauth2server.pojo.entity.oauth2.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

/**
 * 抽象用户登录验证实现，必须继承它实现自己的登录验证
 *
 * @author liugenlai
 * @since 2021/7/26 9:57
 */
public abstract class AbstractUserDetailsService implements UserDetailsService {
    private List<IntegrationAuthenticator> authenticators;

    @Autowired(required = false)
    public void setAuthenticators(List<IntegrationAuthenticator> authenticators) {
        this.authenticators = authenticators;
    }

    /**
     * 每次登录都会调用这个方法验证用户信息
     *
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 判断是否是集成登录
        IntegrationAuthentication integrationAuthentication = IntegrationAuthenticationContext.get();
        if (integrationAuthentication == null) {
            integrationAuthentication = new IntegrationAuthentication();
        }
        integrationAuthentication.setUsername(username);
        // 调用集成验证器自身的验证方法
        System.out.println(integrationAuthentication);
        SysUserAuthentication sysUserAuthentication = this.authenticate(integrationAuthentication);

        if (sysUserAuthentication == null) {
            throw new UsernameNotFoundException("未查询到有效用户信息");
        }
        // 填充权限角色信息
        Authorize authorize = getAuthorize(sysUserAuthentication.getId());
        User user = this.setAuthorize(authorize, sysUserAuthentication);
        return user;
    }

    /**
     * 调用集成验证器自身的验证方法
     *
     * @param integrationAuthentication 集成登录参数对象
     * @return SysUserAuthentication
     */
    public SysUserAuthentication authenticate(IntegrationAuthentication integrationAuthentication) {
        if (this.authenticators != null) {
            for (IntegrationAuthenticator authenticator : authenticators) {
                if (authenticator.support(integrationAuthentication)) {
                    return authenticator.authenticate(integrationAuthentication);
                }
            }
        }
        return null;
    }

    /**
     * 设置角色权限信息
     *
     * @param authorize
     * @param authorize
     * @throws UsernameNotFoundException
     */
    public User setAuthorize(Authorize authorize, SysUserAuthentication sysUserAuthentication) throws UsernameNotFoundException {
        User user = new User();
        BeanUtils.copyProperties(sysUserAuthentication, user);
        if (user == null) {
            throw new UsernameNotFoundException("未查询到有效用户信息");
        }
        // 无权限
        if (authorize == null) {
            return user;
        }
        user.setRoles(authorize.getRoles());
        user.setResources(authorize.getResources());
        return user;
    }

    /**
     * 获取角色和权限列表
     *
     * @param id 用户ID
     * @return Authorize
     */
    public abstract Authorize getAuthorize(Long id);
}
