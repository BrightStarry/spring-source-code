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

package org.springframework.beans.factory.parsing;

/**
 * 空的{@link ReaderEventListener}接口实现类
 * 提供所有回调方法的无操作实现
 *
 * 也就是当事件触发，不做任何处理
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class EmptyReaderEventListener implements ReaderEventListener {

	@Override
	public void defaultsRegistered(DefaultsDefinition defaultsDefinition) {
		// no-op
	}

	@Override
	public void componentRegistered(ComponentDefinition componentDefinition) {
		// no-op
	}

	@Override
	public void aliasRegistered(AliasDefinition aliasDefinition) {
		// no-op
	}

	@Override
	public void importProcessed(ImportDefinition importDefinition) {
		// no-op
	}

}
