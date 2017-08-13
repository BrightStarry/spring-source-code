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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * Simple {@link ProblemReporter} implementation that exhibits fail-fast
  behavior when errors are encountered.

 * 简单的{@link ProblemReporter} 实现类，当遇到错误时，快速失败
 * (不知道和Iterator中的快速失败是不是一个东西，如果是的话，应该是执行某些操作时，会直接抛出异常)
 *
 * <p>遇到第一个error时抛出{@link BeanDefinitionParsingException}？ </>
 * <p>The first error encountered results in a {@link BeanDefinitionParsingException}
  being thrown.
 *
 * <p>在该类中，警告被写入{@link #setLogger(org.apache.commons.logging.Log)}日志.
 */
public class FailFastProblemReporter implements ProblemReporter {

	private Log logger = LogFactory.getLog(getClass());


	/**
	 * 设置用于报告Warn的@link日志记录器
	 * <p>If set to {@code null} then a default {@link Log logger} set to
	 * the name of the instance class will be used.
	 * @param logger the {@link Log logger} that is to be used to report warnings
	 */
	public void setLogger(@Nullable Log logger) {
		this.logger = (logger != null ? logger : LogFactory.getLog(getClass()));
	}


	/**
	 	抛出一个 {@link BeanDefinitionParsingException}异常，发生时
	 * @param problem error来源
	 */
	@Override
	public void fatal(Problem problem) {
		throw new BeanDefinitionParsingException(problem);
	}

	/**
	 * 抛出一个 {@link BeanDefinitionParsingException}异常，发生时
	 * @param problem the source of the error
	 */
	@Override
	public void error(Problem problem) {
		throw new BeanDefinitionParsingException(problem);
	}

	/**
	 * 当Warn级别时，将Problem写入log
	 * @param problem the source of the warning
	 */
	@Override
	public void warning(Problem problem) {
		this.logger.warn(problem, problem.getRootCause());
	}

}
