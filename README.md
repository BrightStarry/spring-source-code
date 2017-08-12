### Spring源码学习 
2017年8月9日

#### 源码导入
1. 下载Gradle.配置环境变量.
2. 在gitHub上搜索Spring-Framework。下载源码zip.
3. 解压
4. 在解压出来的文件中找到build.gradle文件,在buildscript{}和configure(allprojects) {}的repositories{}中加上:  
    maven{ url 'http://maven.aliyun.com/nexus/content/groups/public/'}  
    放在第一句; 
5. 注意，不要删除原来的地址，因为有些依赖阿里云的镜像上并不全，还需要从原仓库下载(速度听天由命，有时候翻墙都翻不出去)
6. 然后导入IDEA,详见该博客http://blog.csdn.net/a153870727/article/details/50624584

#### ServletConfig
web容器在创建servlet实例时，会将配置在web.xml中的init-param参数封装到ServletConfig中，  
并在调用Servlet的init方法时将ServletConfig对象传递给Servlet

#### init-params 和 context-params
前者会被封装到ServletConfig对象(每个Servlet对应一个ServletConfig)中，后者会被封装到ServletContext对象(相当于Application，全局唯一)中  

#### ClassLoader：类加载器
* 当前线程类加载器

* 当前类的类加载器

* 系统类加载器


#### JAR、WAR、EAR
* jar：包含class、properties文件
* war：包含Servlet、JSP、JAR、静态资源等文件
* ear：包含jar、war和EJB组件

#### 强引用、软引用、弱引用、幽灵引用、引用队列
* 强引用：
    * 例如Date date = new Date()；对象可以在程序中到处传递；
    * 强引用限制了对象在内存中的存活时间；例如A对象中保存了B对象的强引用，那么如果
    A对象没有把B对象设为null的话，只有当A对象被回收后，B对象不再指向它了，才可能被回收.
* 软引用:SoftReference
    * 当JVM内存不足时，可以回收软引用对象，如果还不足才抛出OOM(OutOfMemory,内存泄露)；
    * 该引用非常适合创建缓存；
    * 注意，因为该对象可能被回收，所以每次get时，需要判断是否存在
* 弱引用：WeakReference
    * 引用一个对象，但是并不阻止该对象被回收
    * 在垃圾回收器运行的时候，如果一个对象的所有引用都是弱引用的话，该对象会被回收
    * 弱引用的作用在于解决强引用所带来的对象之间在存活时间上的耦合关系
    * 常用于集合类，例如HashMap中；
* 幽灵引用：PhantomReference
    * 任何时候调用get，返回的都是null，需要搭配引用队列使用
    * PhantomReference ref = new PhantomReference(new A(), queue); 这么写可以确保A对象完全被回收后才进入引用队列
    * 在创建幽灵引用PhantomReference的时候必须要指定一个引用队列。
    当一个对象的finalize方法已经被调用了之后，这个对象的幽灵引用会被加入到队列中。
    通过检查该队列里面的内容就知道一个对象是不是已经准备要被回收了。
    * 幽灵引用及其队列的使用情况并不多见，主要用来实现比较精细的内存使用控制，这对于移动设备来说是很有意义的。
    程序可以在确定一个对象要被回收之后，再申请内存创建新的对象。通过这种方式可以使得程序所消耗的内存维持在一个相对较低的数量
* 引用队列：ReferenceQueue
    * 在有些情况下，程序会需要在一个对象的可达到性发生变化的时候得到通知。
    比如某个对象的强引用都已经不存在了，只剩下软引用或是弱引用。但是还需要对引用本身做一些的处理。
    典型的情景是在哈希表中。引用对象是作为WeakHashMap中的键对象的，当其引用的实际对象被垃圾回收之后，
    就需要把该键值对从哈希表中删除。有了引用队列（ReferenceQueue），就可以方便的获取到这些弱引用对象，
    将它们从表中删除。在软引用和弱引用对象被添加到队列之前，其对实际对象的引用会被自动清空。
    通过引用队列的poll/remove方法就可以分别以非阻塞和阻塞的方式获取队列中的引用对象。
---

#### 以ContextLoaderListener（上下文加载器监听器）为入口
* ContextLoaderListener是配置在web.xml中的Spring环境加载监听器
* 它既继承了ContextLoader类，也实现了ServletContextListener接口
* 它的大部分方法都是通过父类ContextLoader实现的，
    他可以算作一个适配器，本身只是为了实现Servlet的监听器接口，以便在WEB应用启动时加载容器（ApplicationContext类）
* 当Web应用启动时，也就是ServletContext启动时;
    它的contextInitialized()方法监听到该事件，
    将执行其父类ContextLoader的initWebApplicationContext()方法，
    并以ServletContext作为参数
* 此外，它也有销毁方法，也就是在Servlet销毁时触发
* ServletContext是Servlet的最高级别范围，其优先级如下:
    * page -> request -> session -> application(servletContext)
    * 注意：ServletContext在jsp中为application,这两个是同一个东西
#### ContextLoader类（上下文加载器） : ContextLoaderListener的父类
* initWebApplicationContext()方法，初始化容器
    * 如果ServletContext中，已经设置了容器名.ROOT属性名的属性，表示容器已经加载，抛出异常
    
    * 否则，调用createWebApplicationContext()方法创建容器
        * 创建容器方法中，调用determineContextClass()方法确认容器的类对象()
            * 如果在web.xml中指定了contextClass参数，则使用自定义的容器类（该自定义容器类必须实现ConfigurableWebApplicationContext接口）
            * 否则会从Spring的一个默认策略文件中读取默认的容器类(WebApplicationContext)
        * 确认完容器的类对象后，会调用BeanUtils.instantiateClass()实例化该容器(该容器必须有无参构造方法)   
              
    * 创建完容器实例后，如果该容器是自定义容器(XMLWebApplicationContext也)，则
        * 尝试加载父容器并注入该自定义容器
        * 调用configureAndRefreshWebApplicationContext()配置并刷新该容器
            * 配置刷新方法中，如果容器名是默认的(容器类全名（例如：java.util.AClassName）@16进制hashcode)
                如果web.xml中配置了contextId参数，就使用该参数值作为id，
                否则使用 容器类全名:contextPath（项目路径名） 作为id  
            * 并且将ServletContext放到容器中，并从web.xml的contextConfigLocation属性中读取Spring配置文件路径值,放到容器中
            * 并获取容器的ConfigurableEnvironment属性，如果该属性实现了ConfigurableWebEnvironment接口，
                就调用该属性的initPropertySources()方法；
                * 在StandardServletEnvironment类中，该方法调用了WebApplicationContextUtils.initServletPropertySources()方法；
                    把ServletContext或ServletConfig中加载 到环境类中的MutablePropertySources属性中去   
            * 然后调用customizeContext()方法
                * 在该方法中，首先调用determineContextInitializerClasses()方法，
                    根据web.xml中的全局配置参数或非全局配置参数确认 容器初始化器的 类对象集合（可能有多个，用INIT_PARAM_DELIMITERS常量规定的字符分割）
                * 然后遍历这些容器初始化器，并使用GenericTypeResolver.resolveTypeArgument()方法判断这个 容器初始化器的泛型参数(该参数表示该容器初始化器对应的容器类),
                    如果对应的容器类不是 ConfigurableWebApplicationContext，也就是不能初始化自定义容器，就抛出异常
                * 然后将符合要求的容器初始化器类对象实例化 加入到 ContextLoader类的 容器初始化器集合中
                * 然后将该集合排序，并遍历该集合，调用每个容器初始化器的 initialize()方法 对自定义容器进行初始化操作
                    (ApplicationContextInitializer<C extends ConfigurableApplicationContext>接口没有已有的实现类，需要自行实现)
            * 调用WebApplicationContext的refresh()方法  
             
    * 此时，无论是默认容器或是自定义容器都已经加载完毕，则将 容器名.ROOT 作为属性名，容器实例作为值，放入ServletContext中（如果抛出异常，则将异常作为值放入该属性中）
    * 如果 当前线程类加载器 和 当前类的类加载器 是同一个，
        则将context(ContextLoader中的容器成员变量)赋值给currentContext(该类中的“当前”容器成员变量);
        否则，以 当前线程类加载器为key，以该容器为值，放入当前类的currentContextPerThread属性中，
        以便随后通过 当前线程类加载器 获取到容器(该类中的getCurrentWebApplicationContext()方法便是如此获取到当前容器的)
#### XMLWebApplicationContext容器类-该容器类也实现了ConfigurableApplicationContext接口
在ContextLoader类中，如果没有使用自定义容器类，则 createWebApplicationContext() 方法中，会从defaultStrategies(Properties类)属性中获取 默认配置；  
defaultStrategies属性在ContextLoader的static代码块中被加载；也就是 DEFAULT_STRATEGIES_PATH = "ContextLoader.properties"文件；  
该文件中只定义了默认WebApplicationContext的实现，也就是XMLWebApplicationContext类.


#### AbstractRefreshableWebApplicationContext类-XMLWebApplicationContext容器类的父类
   