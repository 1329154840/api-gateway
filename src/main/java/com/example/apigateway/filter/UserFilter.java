package com.example.apigateway.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * Created by IntelliJ IDEA.
 * Description:  ---——require需求|ask问题|jira
 * Design :  ----the  design about train of thought 设计思路
 * User: yezuoyao
 * Date: 2019-02-17
 * Time: 19:32
 * Email:yezuoyao@huli.com
 *
 * @author yezuoyao
 * @since 1.0-SNAPSHOT
 */
@Component
@Slf4j
public class UserFilter extends ZuulFilter {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        /**
         * 调用一般服务，需要校验token
         */
        if (!request.getRequestURI().equals("/account") && !request.getRequestURI().equals("/account/") && !request.getRequestURI().contains("/regist") && !request.getRequestURI().contains("/login")) {
            log.info(request.getRequestURI());
            return true;
        }
        return false;
    }

    @Override
    public Object run() throws ZuulException {
        String loginHtmlUrl = "http://127.0.0.1:8080/account";

        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        HttpServletResponse response =requestContext.getResponse();
        Cookie[] cookies = request.getCookies();
        if(cookies==null ||cookies.length ==0 ){
            try {
                response.sendRedirect(loginHtmlUrl);
            }catch (Exception e){
                log.error("重定向错误 {}",e.getMessage());
            }
        }else {
            boolean isFind = false;
            for (Cookie cookie:cookies){
                if ("token".equals(cookie.getName())){
                    isFind =true;
                    if (!check(cookie.getValue(), response)){
                        try {
                            response.sendRedirect(loginHtmlUrl);
                        }catch (Exception e){
                            log.error("重定向错误 {}",e.getMessage());
                        }
                    }
                }
            }
            if (isFind){
                try {
                    response.sendRedirect(loginHtmlUrl);
                }catch (Exception e){
                    log.error("重定向错误 {}",e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 从redis获取cookie校验token
     * @param token
     * @return
     */
    private boolean check(String token,  HttpServletResponse response){
        String[] buffer = token.split("-");
        if (buffer.length != 3){
            return false;
        }
        String rdToken = stringRedisTemplate.opsForValue().get(buffer[0]);
        if (rdToken.equals(token)){
            response.setHeader("uid",buffer[0]);
            return true;
        }
        return false;
    }
}
