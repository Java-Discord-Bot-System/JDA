package com.almightyalpaca.discord.bot.system.util;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

	public static void replaceAll(final StringBuilder builder, final String from, final String to) {
		int index;
		while ((index = builder.indexOf(from)) != -1) {
			builder.replace(index, index + from.length(), to);
		}

	}

	public static String replaceFirst(final String text, final String searchString, final String replacement) {
		return org.apache.commons.lang3.StringUtils.replaceOnce(text, searchString, replacement);
	}

	/**
	 * Thanks StackOverflow
	 */
	public static String replaceLast(final String text, final String regex, final String replacement) {
		return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
	}

	public static String[] split(String string, final int lenth, final String split) {
		if (string.length() <= lenth) {
			return new String[] { string };
		}
		final List<String> strings = new ArrayList<>();

		while (string.length() > lenth) {
			final String current = string.substring(0, lenth + split.length());

			final int index = current.lastIndexOf(split);

			if (index == -1) {
				throw new UnsupportedOperationException("One or more substrings were too long!");
			}

			final String substring = current.substring(0, index);

			strings.add(substring);
			string = StringUtils.replaceFirst(string, substring + split, "");

		}

		return strings.toArray(new String[strings.size()]);
	}

}
