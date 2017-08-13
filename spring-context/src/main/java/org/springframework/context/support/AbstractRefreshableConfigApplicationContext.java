/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 抽象的可刷新的配置容器
 * {@link AbstractRefreshableApplicationContext}(抽象的可刷新容器)的子类，
 * 增加了通用的处理 指定配置文件位置的方法。
 * <p作为>xml基础容器，实现了{@link ClassPathXmlApplicationContext}、
 *  {@link FileSystemXmlApplicationContext}、{@link org.springframework.web.context.support.XmlWebApplicationContext}
 *
 * @see #setConfigLocation
 * @see #setConfigLocations
 * @see #getDefaultConfigLocations
 */
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
		implements BeanNameAware, InitializingBean {
	/**
	 * 配置文件路径 数组
	 */
	@Nullable
	private String[] configLocations;
	/**
	 * 是否调用过 setId方法，默认false
	 */
	private boolean setIdCalled = false;


	/**
	 * 创建一个新的该类，没有父容器
	 */
	public AbstractRefreshableConfigApplicationContext() {
	}

	/**
	 * 创建该类，使用给定的父容器
	 *
	 * @param parent the parent context
	 */
	public AbstractRefreshableConfigApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 在web.xml的init-param中设置这个容器的配置位置，
	 * 使用逗号、分号或空格 分隔不同的配置路径
	 * <p>如果没有配置，则使用默认值
	 */
	public void setConfigLocation(String location) {
		setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
	}

	/**
	 * 在容器中设置 配置路径
	 * <p>如果没有配置，则使用默认值
	 */
	public void setConfigLocations(@Nullable String... locations) {
		//没看明白它为什么两次不为空判断，一个if，一个断言
		if (locations != null) {
			Assert.noNullElements(locations, "Config locations must not be null");

			//将配置文件路径 解析后（关于占位符的解析） 放入该数组
			this.configLocations = new String[locations.length];
			for (int i = 0; i < locations.length; i++) {
				this.configLocations[i] = resolvePath(locations[i]).trim();
			}
		}
		else {
			this.configLocations = null;
		}
	}

	/**
	 * 返回资源位置数组，引用XML bean definition files建立这个容器，
	 * 还可以包括location pattern，这是通过一个{@link org.springframework.core.io.support.ResourcePatternResolver} 来解析
	 *
	 * <p>{@link #getDefaultConfigLocations}默认的实现返回null。子类可以重写这个方法返回资源路径，来加载bean definition
	 * @return an array of resource locations, or {@code null} if none
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	@Nullable
	protected String[] getConfigLocations() {
		return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
	}

	/**
	 * 返回使用的默认 config locations，如果不是这样的情况，已经指定了显示的 配置文件路径(web.xml中配置了)
	 * <p>默认返回空，需要指定显示的 配置文件路径
	 * @return an array of default config locations, if any
	 * @see #setConfigLocations
	 */
	@Nullable
	protected String[] getDefaultConfigLocations() {
		return null;
	}

	/**
	 * 如果必要的话,解析给定路径，将占位符替换为相应的环境属性值，用于 config location
	 * @param path 原来的文件路径
	 * @return 解析后的路径
	 * @see org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)
	 */
	protected String resolvePath(String path) {
		//调用环境类中的 解析必要的占位符方法
		return getEnvironment().resolveRequiredPlaceholders(path);
	}

	/**
	 *
	 */
	@Override
	public void setId(String id) {
		super.setId(id);
		this.setIdCalled = true;
	}

	/**
	 * Sets the id of this context to the bean name by default,
	 * for cases where the context instance is itself defined as a bean.
	 */
	@Override
	public void setBeanName(String name) {
		if (!this.setIdCalled) {
			super.setId(name);
			setDisplayName("ApplicationContext '" + name + "'");
		}
	}

	/**
	 * Triggers {@link #refresh()} if not refreshed in the concrete context's
	 * constructor already.
	 */
	@Override
	public void afterPropertiesSet() {
		if (!isActive()) {
			refresh();
		}
	}

}
