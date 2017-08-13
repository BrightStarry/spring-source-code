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

package org.springframework.util.xml;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 检测XML流是否使用DTD或基于xs验证的验证
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XmlValidationModeDetector {

	/**
	 * 表示验证应该被禁用
	 */
	public static final int VALIDATION_NONE = 0;

	/**
	 * 表明验证模式应该是自动控制的，因为我们找不到 一个清楚的指示（可能会阻塞指定的字符?）
	 * Indicates that the validation mode should be auto-guessed, since we cannot find
	  a clear indication (probably choked on some special characters, or the like).
	 */
	public static final int VALIDATION_AUTO = 1;

	/**
	 *表示应该使用DTD验证(我们找到了一个“DOCTYPE”声明)。
	 */
	public static final int VALIDATION_DTD = 2;

	/**
	 * 表示应该使用XSD验证(没有发现“DOCTYPE”声明)。
	 */
	public static final int VALIDATION_XSD = 3;


	/**
	 * XML文档中的令牌，声明用于验证的DTD 因此，正在使用DTD验证。
	 */
	private static final String DOCTYPE = "DOCTYPE";

	/**
	 * 表示XML注释的开始的标记
	 */
	private static final String START_COMMENT = "<!--";

	/**
	 * 表示XML注释的结束的标记
	 */
	private static final String END_COMMENT = "-->";


	/**
	 * 表示当前的解析位置是否在XML注释中
	 */
	private boolean inComment;


	/**
	 * 在提供的{@link InputStream}中检测XML文档的验证模式.
	  请注意，在返回之前，关闭InputStream
	 * @param inputStream inputStream解析
	 * @throws IOException I/O失败的情况下
	 * @see #VALIDATION_DTD
	 * @see #VALIDATION_XSD
	 */
	public int detectValidationMode(InputStream inputStream) throws IOException {
		// Peek into the file to look for DOCTYPE.
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			boolean isDtdValidated = false;
			String content;
			while ((content = reader.readLine()) != null) {
				content = consumeCommentTokens(content);
				if (this.inComment || !StringUtils.hasText(content)) {
					continue;
				}
				if (hasDoctype(content)) {
					isDtdValidated = true;
					break;
				}
				if (hasOpeningTag(content)) {
					// End of meaningful data...
					break;
				}
			}
			return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
		}
		catch (CharConversionException ex) {
			// Choked on some character encoding...
			// Leave the decision up to the caller.
			return VALIDATION_AUTO;
		}
		finally {
			reader.close();
		}
	}


	/**
	 * 检测 内容是否包含DTD DOCTYPE声明?
	 */
	private boolean hasDoctype(String content) {
		return content.contains(DOCTYPE);
	}

	/**所提供的内容包含XML ope tag;
	 * 如果解析状态是当前状态 在XML注释中，这个方法总是返回false;
	 * 它是预期的，所有注解符号将会消耗提供的内容 ，在将剩余内容传递给该方法之前
	 * It is expected that all comment tokens will have consumed for the supplied content before passing the remainder to this method.
	 */
	private boolean hasOpeningTag(String content) {
		if (this.inComment) {
			return false;
		}
		int openTagIndex = content.indexOf('<');
		return (openTagIndex > -1 && (content.length() > openTagIndex + 1) &&
				Character.isLetter(content.charAt(openTagIndex + 1)));
	}

	/**
	 * Consumes all the leading comment data in the given String and returns the remaining content, which
	 * may be empty since the supplied content might be all comment data. For our purposes it is only important
	 * to strip leading comment content on a line since the first piece of non comment content will be either
	 * the DOCTYPE declaration or the root element of the document.
	 */
	@Nullable
	private String consumeCommentTokens(String line) {
		if (!line.contains(START_COMMENT) && !line.contains(END_COMMENT)) {
			return line;
		}
		String currLine = line;
		while ((currLine = consume(currLine)) != null) {
			if (!this.inComment && !currLine.trim().startsWith(START_COMMENT)) {
				return currLine;
			}
		}
		return null;
	}

	/**
	 * Consume the next comment token, update the "inComment" flag
	 * and return the remaining content.
	 */
	private String consume(String line) {
		int index = (this.inComment ? endComment(line) : startComment(line));
		return (index == -1 ? null : line.substring(index));
	}

	/**
	 * Try to consume the {@link #START_COMMENT} token.
	 * @see #commentToken(String, String, boolean)
	 */
	private int startComment(String line) {
		return commentToken(line, START_COMMENT, true);
	}

	private int endComment(String line) {
		return commentToken(line, END_COMMENT, false);
	}

	/**
	 * Try to consume the supplied token against the supplied content and update the
	 * in comment parse state to the supplied value. Returns the index into the content
	 * which is after the token or -1 if the token is not found.
	 */
	private int commentToken(String line, String token, boolean inCommentIfPresent) {
		int index = line.indexOf(token);
		if (index > - 1) {
			this.inComment = inCommentIfPresent;
		}
		return (index == -1 ? index : index + token.length());
	}

}
