/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * Interface to provide configuration for a web application. This is read-only while
 * the application is running, but may be reloaded if the implementation supports this.
 *
 * 为WEB应用提供配置的接口.这是只读的，虽然应用在运行，如果实现类支持，可能会重新加载
 * <p>这个接口在{@link ApplicationContext}接口上增加了getServletContext()方法.
 * 定义了一个众所皆知的应用属性名.
 * root context必须绑定到引导过程中.
 * <p>与ApplicationContext一样，webApplicationContext是分层的。
 * 每个应用程序都有一个根上下文，而应用程序中的每个servlet (包括MVC框架中的dispatcher servlet)有它自己的子上下文。
 * <p>除了标准ApplicationContext的生命周期功能外，该类还需要检测{@link ServletContextAware}，
 * 并调用{@code setServletContext}方法
 * @see ServletContextAware#setServletContext
 */
public interface WebApplicationContext extends ApplicationContext {

	/**
	 * 成功启动时，将该属性绑定到ServletContext上
	 * <p>如果启动失败,该属性可以包含异常信息；使用{@link org.springframework.web.context.support.WebApplicationContextUtils}
	 * 可以方便的查找到root WebApplicationContext
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
	 */
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";


	/**
	 * request范围的范围标识符; 也就是每个request中的bean的作用域
	 * 支持除此之外的singleton(单例) 和 prototype（多例，每个请求(线程)一个实例）
	 */
	String SCOPE_REQUEST = "request";

	/**
	 * session范围的范围标识符;也就是每个session中的bean的作用域
	 * 支持除此之外的singleton(单例) 和 prototype（多例，每个请求(线程)一个实例）
	 */
	String SCOPE_SESSION = "session";

	/**
	 * application(全局)范围的范围标识符;也就是每个application中的bean的作用域
	 * 支持除此之外的singleton(单例) 和 prototype（多例，每个请求(线程)一个实例）
	 */
	String SCOPE_APPLICATION = "application";

	/**
	 * ServletContext在容器中的Bean name
	 */
	String SERVLET_CONTEXT_BEAN_NAME = "servletContext";

	/**
	 * ServletContext/PortletContext的初始化参数(web.xml中自定义的参数)在容器中的Bean name
	 * 注意：可能与ServletConfig/PortletConfig 参数合并。
	 * ServletConfig 参数覆盖相同名字的{@link ServletContext}参数.
	 *
	 * <p>web容器在创建servlet实例时，会将配置在web.xml中的init-param参数封装到ServletConfig中，
	 * 并在调用Servlet的init方法时将ServletConfig对象传递给Servlet
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 * @see javax.servlet.ServletConfig#getInitParameterNames()
	 * @see javax.servlet.ServletConfig#getInitParameter(String)
	 */
	String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

	/**
	 * ServletContext/PortletContext 的属性在容器中的Bean name
	 * @see javax.servlet.ServletContext#getAttributeNames()
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";

	/**
	 * 从这个容器(ApplicationContext)中返回标准ServletContext
	 */
	@Nullable
	ServletContext getServletContext();

}
