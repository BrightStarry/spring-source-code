/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;

/**
 * Context清理监听器-实现Servlet监听器接口
 * web容器监听器，清理ServletContext中剩余可用的属性,实现{@link DisposableBean}接口的以前没被移除的属性.
 * 该类通常用于在application作用域范围内销毁对象，容器的关闭意味着生命周期的结束，
 *
 * @see org.springframework.web.context.support.ServletContextScope
 * @see ContextLoaderListener
 */
public class ContextCleanupListener implements ServletContextListener {

	private static final Log logger = LogFactory.getLog(ContextCleanupListener.class);


	@Override
	public void contextInitialized(ServletContextEvent event) {
	}

	/**
	 * 监听容器销毁事件，调用cleanupAttributes()方法
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		cleanupAttributes(event.getServletContext());
	}


	/**
	 * 找到ServletContext中所有实现{@link DisposableBean}接口的属性，并且销毁他们.
	 * 删除所有受影响的属性.
	 * @param sc the ServletContext to check
	 */
	static void cleanupAttributes(ServletContext sc) {
		//返回ServletContext中所有可用的属性列表
		Enumeration<String> attrNames = sc.getAttributeNames();
		//迭代
		while (attrNames.hasMoreElements()) {
			//取出属性名
			String attrName = attrNames.nextElement();
			//如果该属性名以 这个字符串开头
			if (attrName.startsWith("org.springframework.")) {
				//获取属性值
				Object attrValue = sc.getAttribute(attrName);
				//如果该值实现了 DisposableBean接口，
				if (attrValue instanceof DisposableBean) {
					try {
						//调用该值的销毁方法
						((DisposableBean) attrValue).destroy();
					}
					catch (Throwable ex) {
						logger.error("Couldn't invoke destroy method of attribute with name '" + attrName + "'", ex);
					}
				}
			}
		}
	}

}
