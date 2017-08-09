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
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 环境加载监听器 - 实现了servlet环境监听器接口，可以监听servlet的各个事件，例如启动、session等事件</br>
 * 启动关闭Spring的root,{@link WebApplicationContext}类</br>
 * 简单地委托了{@link ContextLoader}类和{@link ContextCleanupListener}类</br>
 * 在Spring3.1版本，该类支持注入{@link WebApplicationContext}类的构造函数，允许在Servlet3.0+版本配置</br>
 * {@link org.springframework.web.WebApplicationInitializer}类有使用例子</br>
 */
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

	/**
	 * 创建一个新的该类，它将创建一个web应用环境基于contextClass和contextConfigLocation（servlet中的context-params）.
	 * <p>查看{@link ContextLoader}，获取关于他的详细信息.
	 * <p>在web.xml中声明监听器时，需要一个无参的构造方法.
	 * <p>该类会被注册到ServletContext中，当servlet销毁时，将会调用该类的 {@link #contextDestroyed}方法来销毁Spring application context.
	 * 也就是说该类的生命周期和servletContext的生命周期相同
	 *
	 * @see ContextLoader
	 * @see #ContextLoaderListener(WebApplicationContext)
	 * @see #contextInitialized(ServletContextEvent)
	 * @see #contextDestroyed(ServletContextEvent)
	 */
	public ContextLoaderListener() {
	}

	/**
	 * 创建一个新的该类，使用给定的{@link WebApplicationContext}环境.
	 * <p>这在servlet3.0+中非常有用.注册监听器是通过这个接口{@link javax.servlet.ServletContext#addListener}.
	 * <p>上下文可以通过{@linkplain org.springframework.context.ConfigurableApplicationContext#refresh}方法刷新.
	 * <p>如果它实现了{@link ConfigurableWebApplicationContext}接口，并且没有刷新(推荐的方法),
	 * <br>将发生下列情况:
	 *	<ul>
	 *	   <li>如果给定的上下文已经通过{@link org.springframework.context.ConfigurableApplicationContext#setId }
	 *	   	方法分配到id，将会分配一个给它（不明）.
	 *	   </li>
	 *	   <li>
	 *	       ServletContext和ServletConfig对象，将被授权到该应用上下文.
	 *	   </li>
	 *	   <li>
	 *	       {@link #customizeContext} 方法将被调用
	 *	   </li>
	 *	   <li>
	 *	       任何{@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}
	 *	       类,通过contextInitializerClasses指定的的初始化参数将被应用.
	 *	   </li>
	 *	   <li>{@link org.springframework.context.ConfigurableApplicationContext#refresh refresh()}方法将被调用</li>
	 *	</ul>
	 * <p>如果它已经刷新或没有实现{@link ConfigurableWebApplicationContext}接口，上述情况将都不会发生,
	 * 如果用户已经执行了这些操作具体需求(不明)
	 * <p>查看 {@link org.springframework.web.WebApplicationInitializer} 使用例子.
	 * <p>在任何情况下，给定的应用上下文参数将被注册到{@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
	 * 这个参数中去。
	 * 并且，当{@link #contextDestroyed}方法被调用时，该spring应用上下文将被关闭
	 * @param context 管理应用的上下文
	 * @see #contextInitialized(ServletContextEvent)
	 * @see #contextDestroyed(ServletContextEvent)
	 */
	public ContextLoaderListener(WebApplicationContext context) {
		super(context);
	}


	/**
	 * 初始化该web应用上下文-也就是当web应用启动时，Spring应用通过该方法启动
	 * 使用了父类的{@link ContextLoader#initWebApplicationContext(ServletContext)}方法初始化
	 * ServletContextEvent该类是事件监听器
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		initWebApplicationContext(event.getServletContext());
	}


	/**
	 * 关闭这个web应用的上下文
	 * <p>使用了父类的{@link ContextLoader#closeWebApplicationContext(ServletContext)}方法关闭
	 * <p>并调用{@link ContextCleanupListener#cleanupAttributes(ServletContext)}方法来清除所有ServletContext
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		closeWebApplicationContext(event.getServletContext());
		ContextCleanupListener.cleanupAttributes(event.getServletContext());
	}

}
