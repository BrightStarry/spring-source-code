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

package org.springframework.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;



/**
 * {@link Resource} 为了类资源路径实现.
 * 使用给定的{@link ClassLoader}或{@link Class}加载资源.
 * <p>如果类路径资源在文件系统中，支持解析为{@link java.io.File}，而不是用于JAR中的资源。
 * 也就是不能把JAR中的资源解析为File.但全都支持解析为URL.
 * <p>其实就类似一个File,可以根据类路径或类加载器路径获取资源文件
 * @see ClassLoader#getResourceAsStream(String)
 * @see Class#getResourceAsStream(String)
 */
public class ClassPathResource extends AbstractFileResolvingResource {

	private final String path;

	@Nullable
	private ClassLoader classLoader;

	@Nullable
	private Class<?> clazz;


	/**
	 * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
	 * A leading slash will be removed, as the ClassLoader resource access
	 * methods will not accept it.
	 * <p>The thread context class loader will be used for
	 * loading the resource.
	 * @param path the absolute path within the class path
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */

	public ClassPathResource(String path) {
		this(path, (ClassLoader) null);
	}

	/**
	 * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
	 * A leading slash will be removed, as the ClassLoader resource access
	 * methods will not accept it.
	 * @param path the absolute path within the classpath
	 * @param classLoader the class loader to load the resource with,
	 * or {@code null} for the thread context class loader
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public ClassPathResource(String path, @Nullable ClassLoader classLoader) {
		Assert.notNull(path, "Path must not be null");
		//将其变成规范的路径，确保不为空，替换路径分隔符等
		String pathToUse = StringUtils.cleanPath(path);
		//如果路径不以/开头，去掉第一个字符
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		this.path = pathToUse;
		//如果类加载器为空，获取默认类加载器，
		//优先级为 线程上下文类加载器 -> 该工具类的类加载器 -> 系统类加载器 -> 空加载器(科科，这个spring的程序员还卖萌)
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new {@code ClassPathResource} for {@code Class} usage.
	 * The path can be relative to the given class, or absolute within
	 * the classpath via a leading slash.
	 * @param path relative or absolute path within the class path
	 * @param clazz the class to load resources with
	 * @see java.lang.Class#getResourceAsStream
	 */
	public ClassPathResource(String path, @Nullable Class<?> clazz) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.clazz = clazz;
	}

	/**
	 * Create a new {@code ClassPathResource} with optional {@code ClassLoader}
	 * and {@code Class}. Only for internal usage.
	 * @param path relative or absolute path within the classpath
	 * @param classLoader the class loader to load the resource with, if any
	 * @param clazz the class to load resources with, if any
	 */
	protected ClassPathResource(String path, @Nullable ClassLoader classLoader, @Nullable Class<?> clazz) {
		this.path = StringUtils.cleanPath(path);
		this.classLoader = classLoader;
		this.clazz = clazz;
	}


	/**
	 * Return the path for this resource (as resource path within the class path).
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * Return the ClassLoader that this resource will be obtained from.
	 */
	@Nullable
	public final ClassLoader getClassLoader() {
		return (this.clazz != null ? this.clazz.getClassLoader() : this.classLoader);
	}


	/**
	 * This implementation checks for the resolution of a resource URL.
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	@Override
	public boolean exists() {
		return (resolveURL() != null);
	}

	/**
	 * Resolves a URL for the underlying class path resource.
	 * @return the resolved URL, or {@code null} if not resolvable
	 */
	@Nullable
	protected URL resolveURL() {
		if (this.clazz != null) {
			return this.clazz.getResource(this.path);
		}
		else if (this.classLoader != null) {
			return this.classLoader.getResource(this.path);
		}
		else {
			return ClassLoader.getSystemResource(this.path);
		}
	}

	/**
	 * This implementation opens an InputStream for the given class path resource.
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see java.lang.Class#getResourceAsStream(String)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is;
		if (this.clazz != null) {
			is = this.clazz.getResourceAsStream(this.path);
		}
		else if (this.classLoader != null) {
			is = this.classLoader.getResourceAsStream(this.path);
		}
		else {
			is = ClassLoader.getSystemResourceAsStream(this.path);
		}
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	/**
	 * This implementation returns a URL for the underlying class path resource,
	 * if available.
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = resolveURL();
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * This implementation creates a ClassPathResource, applying the given path
	 * relative to the path of the underlying resource of this descriptor.
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new ClassPathResource(pathToUse, this.classLoader, this.clazz);
	}

	/**
	 * This implementation returns the name of the file that this class path
	 * resource refers to.
	 * @see org.springframework.util.StringUtils#getFilename(String)
	 */
	@Override
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * This implementation returns a description that includes the class path location.
	 */
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder("class path resource [");
		String pathToUse = path;
		if (this.clazz != null && !pathToUse.startsWith("/")) {
			builder.append(ClassUtils.classPackageAsResourcePath(this.clazz));
			builder.append('/');
		}
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		builder.append(pathToUse);
		builder.append(']');
		return builder.toString();
	}


	/**
	 * This implementation compares the underlying class path locations.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ClassPathResource) {
			ClassPathResource otherRes = (ClassPathResource) obj;
			return (this.path.equals(otherRes.path) &&
					ObjectUtils.nullSafeEquals(this.classLoader, otherRes.classLoader) &&
					ObjectUtils.nullSafeEquals(this.clazz, otherRes.clazz));
		}
		return false;
	}

	/**
	 * This implementation returns the hash code of the underlying
	 * class path location.
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
