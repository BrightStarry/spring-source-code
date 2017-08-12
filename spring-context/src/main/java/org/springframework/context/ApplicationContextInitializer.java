/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context;

/**
 *
 * 回调接口，在{@link ConfigurableApplicationContext#refresh()}刷新前,初始化{@link ConfigurableApplicationContext}
 * <p>通常用在web应用程序中需要对应用程序上下文做一些程序初始化时。例如：注册property资源文件或者
 * 激活对上下文环境的环境配置文件{@link ConfigurableApplicationContext#getEnvironment()}。
 * 分别参见ContextLoader和FrameworkServlet，分别通过在<context-param>和<init-param>中声明contextInitializerClasses参数来的支持。
 * <p>该类处理器检测是否实现了 {@link org.springframework.core.Ordered Ordered}接口，
 * 或者是否有 @{@link org.springframework.core.annotation.Order Order}注解的实例存在，以此按声明的顺序进行调用
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.web.context.ContextLoader#customizeContext
 * @see org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * @see org.springframework.web.servlet.FrameworkServlet#setContextInitializerClasses
 * @see org.springframework.web.servlet.FrameworkServlet#applyInitializers
 */
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

	/**
	 * 初始化给定的application context（必须继承ConfigurableApplicationContext）
	 * @param applicationContext
	 */
	void initialize(C applicationContext);

}
