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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

/**
 * 用于加载XML{@link Document}的策略接口
 * @author Rob Harrop
 * @since 2.0
 * @see DefaultDocumentLoader
 */
public interface DocumentLoader {

	/**
	 * 通过提供的{@link InputSource}加载一个{@link Document}
	 * @param inputSource 要加载的文档的源文件
	 * @param entityResolver 用来解析任何实体的解析器{@link EntityResolver}
	 * @param errorHandler 用于在文档加载期间报告任何错误{@link ErrorHandler}
	 * @param validationMode 验证类型
	 * {@link org.springframework.util.xml.XmlValidationModeDetector#VALIDATION_DTD DTD}
	 * or {@link org.springframework.util.xml.XmlValidationModeDetector#VALIDATION_XSD XSD})
	 * @param namespaceAware 如果要提供对XML名称空间的支持，则为true
	 * @return 加载的{@link Document }
	 * @throws Exception 如果发生错误
	 */
	Document loadDocument(
			InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware)
			throws Exception;

}
