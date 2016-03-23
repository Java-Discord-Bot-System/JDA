package com.almightyalpaca.discord.bot.system.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StringUtils {

	public static String replaceFirst(final String string, final String target, final String replacement) {
		return target.replaceFirst(Pattern.quote(target), replacement);
	}

	/**
	 * Thanks StackOverflow
	 */
	public static String replaceLast(final String text, final String regex, final String replacement) {
		return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
	}

	public static String[] split(String string, final int lenth, final String split) {
		final List<String> strings = new ArrayList<>();

		while (string.length() > lenth) {
			String temp = string.substring(0, lenth);
			final int index = temp.lastIndexOf(split);
			if (index == -1) {
				throw new UnsupportedOperationException("One or more substrings were too long!");
			}

			temp = temp.substring(0, index + split.length());

			string = StringUtils.replaceFirst(string, temp, "");

			strings.add(StringUtils.replaceLast(temp.substring(0, index + split.length()), split, ""));
		}
		strings.add(string);

		return strings.toArray(new String[strings.size()]);
	}

}
