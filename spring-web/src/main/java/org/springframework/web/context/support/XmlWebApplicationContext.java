/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * {@link org.springframework.web.context.WebApplicationContext}的实现类，从xml中读取配置,
 * 明白一个 {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 * <p>本质上等价于{@link org.springframework.context.support.GenericXmlApplicationContext}的web环境
 * <p>默认，将从"/WEB-INF/applicationContext.xml"读取配置,
 * and "/WEB-INF/test-servlet.xml" for a context with the namespace
 * "test-servlet" (like for a DispatcherServlet instance with the servlet-name "test")
 *
 * <p>可以通过web.xml中的context-param 的"contextConfigLocation"参数覆盖默认 配置文件位置.
 * 配置文件可以是"/WEB-INF/context.xml"这样的具体文件，或者/WEB-INF/*-context.xml"这样的模式
 * (查看 {@link org.springframework.util.PathMatcher}javadoc 模式详情).
 *
 * <p>注意：如果有多个配置位置，后面的配置文件中的Bean将覆盖之前的配置文件中的Bean.
 * 可以利用额外的配置文件故意覆盖某些bean
 *
 * <p> WebApplicationContext读取一个不同格式的Bean，可以使用{@link AbstractRefreshableWebApplicationContext}的子类。
 * 也就是配置自定义容器，通过web.xml中的inti-param或context-param的参数名为“contextClass”的参数
 *
 *
 * <p>The config location defaults can be overridden via the "contextConfigLocation"
    context-param of {@link org.springframework.web.context.ContextLoader} and servlet
   init-param of {@link org.springframework.web.servlet.FrameworkServlet}.

 * @see #setNamespace
 * @see #setConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see org.springframework.web.context.ContextLoader#initWebApplicationContext
 * @see org.springframework.web.servlet.FrameworkServlet#initWebApplicationContext
 */
public class XmlWebApplicationContext extends AbstractRefreshableWebApplicationContext {

	/**
	 * 默认配置文件路径
	 */
	public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";

	/**
	 * 默认配置文件路径前缀
	 */
	public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

	/**
	 * 默认配置文件路径后缀
	 */
	public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".xml";


	/**
	 * 通过{@link XmlBeanDefinitionReader} 加载定义的Bean，需要传入{@link DefaultListableBeanFactory}
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// 通过给定的BeanFactory创建该类
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// 配置这个XmlBeanDefinitionReader
		//设置环境类,从该容器类中加载环境类，如果为空会创建(该类应该在ContextLoader类的configureAndRefreshWebApplicationContext()方法中创建过了)
		beanDefinitionReader.setEnvironment(getEnvironment());
		//将该容器作为 资源加载器
		beanDefinitionReader.setResourceLoader(this);
		//将该类作为参数，创建 资源实体解析器类,设置到环境类中
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// 允许一个子类为Bean提供自定义的初始化器
		//该方法默认为空
		initBeanDefinitionReader(beanDefinitionReader);
		//通过这个beanDefinitionReader加载Bean
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化加载Bean到这个{@link XmlBeanDefinitionReader}中，默认实现为空.
	 * 子类可以重写该方法
	 * @param beanDefinitionReader the bean definition reader used by this context
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setValidationMode
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * 使用给定的{@link XmlBeanDefinitionReader}加载定义的bean
	 * <p>BeanFactory的生命周期由{@link #refreshBeanFactory}方法处理.
	 * 因此，该方法只要加载和注册定义的bean即可
	 *
	 * <p>委托一个{@link org.springframework.core.io.support.ResourcePatternResolver}使用特定模式解析 资源实例
	 * <p>Delegates to a ResourcePatternResolver for resolving location patterns into Resource instances.
	 * @throws IOException 如果需要的xml文档找不到
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws IOException {
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				reader.loadBeanDefinitions(configLocation);
			}
		}
	}

	/**
	 * The default location for the root context is "/WEB-INF/applicationContext.xml",
	 * and "/WEB-INF/test-servlet.xml" for a context with the namespace "test-servlet"
	 * (like for a DispatcherServlet instance with the servlet-name "test").
	 */
	@Override
	protected String[] getDefaultConfigLocations() {
		if (getNamespace() != null) {
			return new String[] {DEFAULT_CONFIG_LOCATION_PREFIX + getNamespace() + DEFAULT_CONFIG_LOCATION_SUFFIX};
		}
		else {
			return new String[] {DEFAULT_CONFIG_LOCATION};
		}
	}

}
