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

import java.io.IOException;
import java.io.InputStream;

/**
 * 用于{@link InputStream}类提供的对象的简单接口.
 * <p>{@link Resource}接口的父接口
 * <p>对于只使用一次的流来说，该类可以使用任何给定的{@link InputStream}。
 * Spring的{@link ByteArrayResource}或任何{@link Resource}的子类都是该接口的实例,可以复用该底层流
 *
 * @see java.io.InputStream
 * @see Resource
 * @see InputStreamResource
 * @see ByteArrayResource
 */
public interface InputStreamSource {

	/**
	 * Return an {@link InputStream} for the content of an underlying resource.
	 * <p>It is expected that each call creates a <i>fresh</i> stream.
	 * <p>This requirement is particularly important when you consider an API such
	 * as JavaMail, which needs to be able to read the stream multiple times when
	 * creating mail attachments. For such a use case, it is <i>required</i>
	 * that each {@code getInputStream()} call returns a fresh stream.
	 * @return the input stream for the underlying resource (must not be {@code null})
	 * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
	 * @throws IOException if the content stream could not be opened
	 */
	InputStream getInputStream() throws IOException;

}
