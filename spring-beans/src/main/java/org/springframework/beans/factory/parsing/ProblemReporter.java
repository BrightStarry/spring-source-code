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
 * 允许工具和其他外部进程来处理错误的SPI接口 在bean定义解析期间报告了警告
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see Problem
 */
public interface ProblemReporter {

	/**
	 * 在解析过程中遇到致命错误时调用。
	 * <p>实现必须将给定的问题视为致命的.
	 * 也就是说，他们最终必须抛出？一个exception
	 * @param problem 问题的来源 (从不为null)
	 */
	void fatal(Problem problem);

	/**
	 *	在解析过程中遇到error时调用
	 * <p>实现可以选择将错误视为致命的.
	 * @param problem 问题的来源 (从不为null)
	 */
	void error(Problem problem);

	/**
	 * 在解析过程中发出warn时调用。
	 * <p>警告永远不会被认为是致命的。
	 * @param problem 问题的来源 (从不为null)
	 */
	void warning(Problem problem);

}
