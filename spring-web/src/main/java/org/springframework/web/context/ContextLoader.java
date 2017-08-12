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

package org.springframework.web.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;


/**
 * 上下文加载器
 * <p>对应用环境进行初始化工作.通过{@link ContextLoaderListener}类进行调用
 * <P>查找{@link #CONTEXT_CLASS_PARAM "contextClass"}参数在web.xml中的context-param等级来指定context类类型，
 * 如果没找到，下降到 {@link org.springframework.web.context.support.XmlWebApplicationContext}类
 * <p>使用默认的ContextLoader实现，任何context类都需要实现{@link ConfigurableWebApplicationContext}接口
 * <p>处理{@link #CONFIG_LOCATION_PARAM "contextConfigLocation"} context-param，然后将值传递给context实例.
 * (contextConfigLocation，也就是web.xml中用来配置spring文件存储位置的参数名).
 * 解析这些路径名，可能有多个路径名，通过逗号或空格分割，并支持a/*aaa.xml这样的形式。
 * 如果没有指定，则使用{@link org.springframework.web.context.support.XmlWebApplicationContext#DEFAULT_CONFIG_LOCATION}类默认的位置：/WEB-INF/applicationContext.xml
 * <p>注意，在多个配置文件的情况下，后面的配置文件中的bean将覆盖前面的配置文件中的bean.
 * 在使用Spring默认实现类的时候，可以添加额外的xml文件特意覆盖某些实现.
 * <p>在加载应用上下文之后，该类可以加载父应用上下文，更多信息查看{@link #loadParentContext(ServletContext)}方法
 * <p>在Spring3.1后，且servlet3.0+,该类支持注入root application context,通过{@link #ContextLoader(WebApplicationContext)}这个构造方法
 * <p>查看{@link org.springframework.web.WebApplicationInitializer}类中使用例子
 *
 * @see ContextLoaderListener
 * @see ConfigurableWebApplicationContext
 * @see org.springframework.web.context.support.XmlWebApplicationContext
 */
public class ContextLoader {

	/**
	 * web.xml中init-params中contextId参数配置的容器id,
	 * 底层BeanFactory的序列化id
	 */
	public static final String CONTEXT_ID_PARAM = "contextId";

	/**
	 * servlet中context param的名字，也就是在web.xml中，获取配置文件路径默认从哪个参数名中获取
	 */
	public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

	/**
	 * WebApplicationContext 实现类的配置参数
	 *
	 * @see #determineContextClass(ServletContext)
	 */
	public static final String CONTEXT_CLASS_PARAM = "contextClass";

	/**
	 * 配置参数为{@link ApplicationContextInitializer} 类使用.
	 * <p>用来初始化 root web application context
	 * <p>例如在web.xml中如下配置：
	 * <p><context-param>
	 * <br><param-name>contextInitializerClasses</param-name>
	 * <br><param-value>com.zx.xxx</param-value>
	 * <br></context-param>
	 * <br>即可使用自定义的类
	 *
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String CONTEXT_INITIALIZER_CLASSES_PARAM = "contextInitializerClasses";

	/**
	 * 配置参数为全局 {@link ApplicationContextInitializer} 类使用
	 * 用来初始化 容器的 容器初始化器
	 *
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String GLOBAL_INITIALIZER_CLASSES_PARAM = "globalInitializerClasses";

	/**
	 * 初始化参数(web.xml中的参数)的分隔符  逗号、分号、空格还有制表符、换行符
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

	/**
	 * 类路径资源名（相对于ContextLoader类),
	 * 定义了ContextLoader的默认策略名
	 * 就是从该文件名的文件中读取默认策略
	 * 该文件中只是将默认的ApplicationContext设置为了{@link org.springframework.web.context.support.XmlWebApplicationContext}
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "ContextLoader.properties";

	/**
	 * 默认策略 - 属性文件
	 */
	private static final Properties defaultStrategies;

	static {
		//从属性文件中加载默认的策略实现.
		//这是严格的内部的，不能自定义.
		try {
			//根据该类的路径，定义到ContextLoader.properties文件的位置
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
			//加载该配置文件成 Properties对象
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		} catch (IOException ex) {
			//如果读取出错，抛出异常
			throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
		}
	}


	/**
	 * 根据（线程类加载器）类加载器获取对应的{@link WebApplicationContext}的Map
	 * 这里map的初始化大小设置为1，应该是当前该map只保存线程类加载器和{@link WebApplicationContext}的映射
	 */
	private static final Map<ClassLoader, WebApplicationContext> currentContextPerThread =
			new ConcurrentHashMap<>(1);


	/**
	 * “当前”的WebApplicationContext，如果该类就是web app classLoader,否则为空
	 * 只有当当前线程类加载器和该类的类加载器是同一个，才不为空,
	 * 且值等于{@link #context}
	 */
	@Nullable
	private static volatile WebApplicationContext currentContext;


	/**
	 * {@link WebApplicationContext}类实例
	 */
	@Nullable
	private WebApplicationContext context;

	/**
	 * {@link ApplicationContextInitializer}类List
	 */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<>();


	/**

	 * <p>This constructor is typically used when declaring the {@code
	  ContextLoaderListener} subclass as a {@code <listener>} within {@code web.xml}, as
	  a no-arg constructor is required.
	 * 创建一个新的该类，并将创建一个容器(applicationContext)，根据 web.xml中的 "contextClass" and "contextConfigLocation"参数
	 * <p>这个构造函数通常在声明ContextLoaderListener类作为子类时使用，因为子类需要一个父类的无参构造函数</>
	 * <p>创建容器，将会把{@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}作为属性名注册到ServletContext中，
	 * 并且子类可以自由的调用{@link #closeWebApplicationContext}方法来关闭容器</>
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader() {
	}

	/**
	 * 创建一个新的该类，使用给定的容器。这个构造函数在Servlet3.0+的环境中非常有用。并通过
	 * {@link ServletContext#addListener}方法可以实现监听器的注册
	 * <p>The context may or may not yet be {@linkplain
	  ConfigurableApplicationContext#refresh() refreshed}. If it (a) is an implementation
	  of {@link ConfigurableWebApplicationContext} and (b) has <strong>not</strong>
	  already been refreshed (the recommended approach), then the following will occur:

	   <p>这个容器可以通过{@link ConfigurableApplicationContext#refresh()}刷新.
	 	如果它实现了{@link ConfigurableWebApplicationContext}接口，并且没有刷新过：下列情况将发生:

		<p>下面懒得看了,大致和{@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)}差不多
	 * <ul>
	 * <li>If the given context has not already been assigned an {@linkplain
	 * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
	 * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
	 * the application context</li>
	 * <li>{@link #customizeContext} will be called</li>
	 * <li>Any {@link ApplicationContextInitializer}s specified through the
	 * "contextInitializerClasses" init-param will be applied.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called</li>
	 * </ul>
	 * If the context has already been refreshed or does not implement
	 * {@code ConfigurableWebApplicationContext}, none of the above will occur under the
	 * assumption that the user has performed these actions (or not) per his or her
	 * specific needs.
	 * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
	 * <p>In any case, the given application context will be registered into the
	 * ServletContext under the attribute name {@link
	 * WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} and subclasses are
	 * free to call the {@link #closeWebApplicationContext} method on container shutdown
	 * to close the application context.
	 *
	 * @param context the application context to manage
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader(WebApplicationContext context) {
		this.context = context;
	}


	/**
	 * 指定哪个{@link ApplicationContextInitializer}实例应该使用，
	 * 来初始化这个应用上下文（该类？）
	 * Specify which {@link ApplicationContextInitializer} instances should be used
	 * to initialize the application context used by this {@code ContextLoader}.
	 *
	 * @see #configureAndRefreshWebApplicationContext
	 * @see #customizeContext
	 */
	@SuppressWarnings("unchecked")
	public void setContextInitializers(@Nullable ApplicationContextInitializer<?>... initializers) {
		if (initializers != null) {
			for (ApplicationContextInitializer<?> initializer : initializers) {
				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
			}
		}
	}


	/**
	 * 为给定的{@link ServletContext}初始化Spring的WebApplicationContext，
	 * 或者根据{@link #CONTEXT_CLASS_PARAM contextClass} 和 {@link #CONFIG_LOCATION_PARAM contextConfigLocation}
	 * 创建一个新的
	 * @param servletContext
	 * see #ContextLoader(WebApplicationContext)
	 * @see #CONTEXT_CLASS_PARAM
	 * @see #CONFIG_LOCATION_PARAM@return
	 */
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		//从ServletContext中获取ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE key的value
		//当该属性不为空的时候，表示已经加载了ApplicationContext
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
							"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}
		//使用common-logging作为日志
		Log logger = LogFactory.getLog(ContextLoader.class);
		//在ServletContext的日志中写入消息
		servletContext.log("Initializing Spring root WebApplicationContext");
		//如果日志级别info启用，写入日志
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {

			//在该类属性中存储容器(ApplicationContext),保证在ServletContext关闭时它是可用的
			//如果容器为空，调用创建容器的方法
			if (this.context == null) {
				this.context = createWebApplicationContext(servletContext);
			}
			//如果该容器是 ConfigurableWebApplicationContext的实现类
			//也就是说它是自定义的 ApplicationContext
			if (this.context instanceof ConfigurableWebApplicationContext) {
				//强转
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				//如果不是激活的
				if (!cwac.isActive()) {
					//如果ServletContext没有刷新(也就是没有parent context) -> 提供服务，设置parent context，设置context id 等
					if (cwac.getParent() == null) {
						//ServletContext没有显式的父类容器(ApplicationContext)注入
						//如果有的话，就确定父类容器并注入(但是5.0版本后，loadParentContext()方法默认返回null)
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					//配置并刷新容器
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			//因为上一步如果ApplicationContext，已经创建了，所以，此将容器保存到ServletContext
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			//获取当前线程的类加载器
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			//反正，下面的if就是为了让当前线程的类加载器可以获取到容器
			//如果当前线程的类加载器和 当前类的类加载器是同一个，则将容器赋值给currentContext
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = this.context;
			} else if (ccl != null) {
				//如果不是同一个类加载器
				//则将 cc1这个类加载器 和 容器 建立映射，存入map
				currentContextPerThread.put(ccl, this.context);
			}

			//如果日志debug级别启用,  将容器发布到ServletContext，名字为ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name [" +
						WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
			}
			//如果info启用，输出容器初始化时间
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
			}

			return this.context;
		} catch (RuntimeException ex) {
			//此处没有checkException，所以可以直接用RuntimeException catch 所有异常
			//如果抛出运行时异常，输出日志，并将错误信息保存到ServletContext的ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE属性中
			//也就是上面说的，如果容器加载成功，保存容器实例，如果加载失败，保存错误信息
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		} catch (Error err) {
			//如果是error，同样的操作。
			//不过error后，整个程序就已经崩了
			logger.error("Context initialization failed", err);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, err);
			throw err;
		}
	}

	/**
	 * 使用这个加载器实例化WebApplicationContext，如果指定了，则加载自定义ApplicationContext或默认ApplicationContext
	 * <p>该ApplicationContext必须实现{@link ConfigurableWebApplicationContext}接口
	 * <p>此外，如果{@link #customizeContext}在刷新前被被调用，还允许该ApplicationContext修改自定义配置
	 * @param sc current servlet context
	 * @return the root WebApplicationContext
	 * @see ConfigurableWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		//确定是那个ApplicationContext，并返回类对象
		Class<?> contextClass = determineContextClass(sc);
		//如果ApplicationContext是ConfigurableWebApplicationContext的子类
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
					"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
		}
		//生成实例强转成ConfigurableWebApplicationContext，并返回
		return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

	/**
	 * 确定Context类方法-返回ApplicationContext实例
	 * <p>返回{@link WebApplicationContext}实现类来使用，可以是XmlWebApplicationContext或自定义的实现类
	 *
	 * @param servletContext current servlet context
	 * @return the WebApplicationContext implementation class to use
	 * @see #CONTEXT_CLASS_PARAM
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected Class<?> determineContextClass(ServletContext servletContext) {
		//使用ServletContext从web.xml中获取自定义的初始化参数，key为contextClass
		String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
		//如果不为空
		if (contextClassName != null) {
			try {
				//返回该 ApplicationContext类实例
				return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
			} catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load custom context class [" + contextClassName + "]", ex);
			}
		} else {
			//如果为空，从默认配置文件中读取默认的applicationContext名，也就是WebApplicationContext的类路径
			contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
			try {
				//返回默认类对象
				//默认使用{@link org.springframework.web.context.support.XmlWebApplicationContext}类作为容器
				return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
			} catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load default context class [" + contextClassName + "]", ex);
			}
		}
	}

	/**
	 * 自定义容器才需要进行的操作
	 * 配置并刷新{@link WebApplicationContext}
	 * @param wac
	 * @param sc
	 */
	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
		//ObjectUtils.identityToString(wac)该方法返回    对象类名+对象hashcode的16进制的格式显示,
		//将其与对象自己的id(该id默认就是上面那的方法返回的值)比较，如果相同，表明容器id未修改过
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			//如果该容器id还是默认值，根据可用信息分配一个更好的id
			//使用ServletContext中从web.xml获取自定义的容器id值
			String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
			//如果不为空，将该值设置为容器id，也就覆盖了默认值
			if (idParam != null) {
				wac.setId(idParam);
			} else {
				//否则生成默认id
				//默认id 为 类名 + : + ServletContext的路径名
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(sc.getContextPath()));
			}
		}

		//并将ServletContext放到容器中
		wac.setServletContext(sc);
		//获取web.xml中配置的  Spring配置文件路径
		String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
		//如果该值不为空，在容器中设置配置文件路径
		if (configLocationParam != null) {
			wac.setConfigLocation(configLocationParam);
		}

		// The wac environment's #initPropertySources will be called in any case when the context
		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
		// use in any post-processing or initialization that occurs below prior to #refresh

		//这个容器环境的 initPropertySource方法将在任何情况下被调用
		//是刷新的；在这里要确保servlet属性源的位置
		//在刷新前的任何 处理 或 初始化 中使用

		//获取该容器的 ConfigurableEnvironment类
		ConfigurableEnvironment env = wac.getEnvironment();
		//如果其实现了ConfigurableWebEnvironment接口，也就是initPropertySources方法
		if (env instanceof ConfigurableWebEnvironment) {
			//强转并调用该方法，
			//该方法的作用是把ServletContext或ServletConfig中加载 到环境类中的MutablePropertySources属性中去
			//在StandardServletEnvironment类中，该方法调用了WebApplicationContextUtils.initServletPropertySources()方法
			((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
		}
		//自定义上下文方法
		customizeContext(sc, wac);
		//使用容器的加载或刷新配置方法
		wac.refresh();
	}



	/**
	 * 在配置文件提供给ServletContext后，(应该是调用initPropertySources方法后)
	 * 根据该类自定义创建{@link ConfigurableWebApplicationContext}类，在此之前，该方法的作用是刷新
	 * <p>The default implementation {@linkplain #determineContextInitializerClasses(ServletContext)
	determines} what (if any) context initializer classes have been specified through
	 {@linkplain #CONTEXT_INITIALIZER_CLASSES_PARAM context init parameters} and
	 {@linkplain ApplicationContextInitializer#initialize invokes each} with the
	 given web application context.
	 <p>Any {@code ApplicationContextInitializers} implementing
	 {@link org.springframework.core.Ordered Ordered} or marked with @{@link
	org.springframework.core.annotation.Order Order} will be sorted appropriately.
	 *  @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
	 */
	protected void customizeContext(ServletContext sc, ConfigurableWebApplicationContext wac) {
		//容器初始化器的类对象的list集合,从web.xml中自定义的参数中获取
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializerClasses =
				determineContextInitializerClasses(sc);
		//遍历
		for (Class<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerClass : initializerClasses) {
			//应该是提取出这个Class<ApplicationContextInitializer<ConfigurableApplicationContext>>类类型
			//的泛型中的泛型，也就是ConfigurableApplicationContext
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			//判断这个类类型不为空，且wac不是ConfigurableWebApplicationContext的实例
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				//抛出错误信息：
				//不能引用初始化器，因为它的泛型参数和这个容器无关
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
								"is not assignable from the type of application context used by this " +
								"context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			//将 初始化器实例化 并增加到list中
			this.contextInitializers.add(BeanUtils.instantiateClass(initializerClass));
		}
		//将初始化器list 排序
		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		//调用所有 容器初始化器 对 容器执行初始化操作
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	/**
	 * 确认容器初始化器的类对象集合
	 * 如果web.xml中已经指定了{@link #CONTEXT_INITIALIZER_CLASSES_PARAM}参数的值，就用该值
	 * @param servletContext current servlet context
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 */
	protected List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>
	determineContextInitializerClasses(ServletContext servletContext) {
		//返回的list
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> classes =
				new ArrayList<>();
		//获取自定义配置的 全局容器初始化器类名
		String globalClassNames = servletContext.getInitParameter(GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			//将 自定义配置的类名 根据 逗号、分号、空格、回车、制表符分割成 类名数组，因为可以配置多个
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}
		//获取不是全局配置的 容器初始化器类名
		String localClassNames = servletContext.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
		if (localClassNames != null) {
			//同上操作,也就是说，如果配置了全局的和非全局的 类名参数，将是累加，而不是覆盖
			for (String className : StringUtils.tokenizeToStringArray(localClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}
		return classes;
	}

	/**
	 * 根据name返回类类型
	 * 加载初始化器类类型
	 */
	@SuppressWarnings("unchecked")
	private Class<ApplicationContextInitializer<ConfigurableApplicationContext>> loadInitializerClass(String className) {
		try {
			Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
			//判断 该初始化器类是否实现 ApplicationContextInitializer接口
			if (!ApplicationContextInitializer.class.isAssignableFrom(clazz)) {
				throw new ApplicationContextException(
						"Initializer class does not implement ApplicationContextInitializer interface: " + clazz);
			}
			//强转
			return (Class<ApplicationContextInitializer<ConfigurableApplicationContext>>) clazz;
		} catch (ClassNotFoundException ex) {
			throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
		}
	}

	/**
	 *	模版模式-模版方法-可能会被子类重写
	 * <p>加载或获取一个容器实例，作为WebApplicationContext的父容器,如果为空，不设置父容器.
	 * <p>在这里加载父容器的原因是，允许多个子容器共享一个 EAR Context（类似ServletContext）和 父容器
	 * <p>对于普通的WEB应用，不需要考虑parent context到容器
	 * <p>默认实现，简单地返回null,在5.0版本
	 * @param servletContext current servlet context
	 * @return the parent application context, or {@code null} if none
	 */
	@Nullable
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		return null;
	}

	/**
	 * 关闭给定ServletContext中的容器
	 * <p>如果重写了{@link #loadParentContext(ServletContext)}方法，你可以重写这个方法</>
	 * @param servletContext the ServletContext that the WebApplicationContext runs in
	 */
	public void closeWebApplicationContext(ServletContext servletContext) {
		//开始关闭。记录servletContext的日志
		servletContext.log("Closing Spring root WebApplicationContext");
		try {
			//如果容器是自定义容器，调用它的关闭方法
			if (this.context instanceof ConfigurableWebApplicationContext) {
				((ConfigurableWebApplicationContext) this.context).close();
			}
		} finally {
			//获取线程的类加载器
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			//如果和当前类的类加载器相同
			if (ccl == ContextLoader.class.getClassLoader()) {
				//将容器置为空
				currentContext = null;
			} else if (ccl != null) {
				//否则删除 map中的容器，这个容器就是以这个线程类加载器为key存在map中的
				currentContextPerThread.remove(ccl);
			}
			//在ServletContext中删除这个 容器属性
			servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
	}


	/**
	 * 获取当前线程的 root web application context，也就是{@link WebApplicationContext}类
	 * <br>获取这个 web application 的类加载器，需要当前线程的context 类加载器
	 * <br>从该类维护的currentContextPerThread Map中通过线程类加载器作为key，获取{@link WebApplicationContext}
	 * <br>如果map中没有，直接获取该类中的{@link WebApplicationContext}，如果该类就是 web应用类加载器
	 * <br>返回值可为空
	 *
	 * @see org.springframework.web.context.support.SpringBeanAutowiringSupport
	 */
	@Nullable
	public static WebApplicationContext getCurrentWebApplicationContext() {
		//获取线程的类加载器
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		if (ccl != null) {
			//从该类维护的currentContextPerThread Map中通过线程类加载器作为key，获取WebApplicationContext
			WebApplicationContext ccpt = currentContextPerThread.get(ccl);
			if (ccpt != null) {
				return ccpt;
			}
		}
		//如果map中没有，直接获取该类中的{@link WebApplicationContext}，如果该类就是 web应用类加载器
		return currentContext;
	}

}
