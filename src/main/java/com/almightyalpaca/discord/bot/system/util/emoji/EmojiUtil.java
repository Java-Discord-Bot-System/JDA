package com.almightyalpaca.discord.bot.system.util.emoji;

import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EmojiUtil {
	private static final Map<String, String>	toUTF8;
	private static final Map<String, String>	toDiscord;
	private static final Map<String, String>	shortcuts;

	static {
		final Map<String, String> tempToUTF8 = new HashMap<>();
		final Map<String, String> tempToDiscord = new HashMap<>();

		try {
			final JsonArray array = new JsonParser().parse(new InputStreamReader(EmojiUtil.class.getResourceAsStream("/emojis.json"), "UTF-8")).getAsJsonArray();

			for (int i = 0; i < array.size(); i++) {
				final JsonObject object = array.get(i).getAsJsonObject();

				final String name = ":" + object.get("emoji").getAsString() + ":";
				final JsonArray utfArray = object.get("surrogates").getAsJsonArray();

				tempToUTF8.put(name, utfArray.get(0).getAsString());

				for (int j = 0; j < utfArray.size(); j++) {
					final String emoji = utfArray.get(j).getAsString();
					tempToDiscord.put(emoji, name);
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}

		toUTF8 = Collections.unmodifiableMap(tempToUTF8);
		toDiscord = Collections.unmodifiableMap(tempToDiscord);

		final Map<String, String> tempShortcuts = new HashMap<>();

		try {
			final JsonArray array = new JsonParser().parse(new InputStreamReader(EmojiUtil.class.getResourceAsStream("/shortcuts.json"), "UTF-8")).getAsJsonArray();

			for (int i = 0; i < array.size(); i++) {
				final JsonObject object = array.get(i).getAsJsonObject();

				final String name = ":" + object.get("emoji").getAsString() + ":";
				final JsonArray shortcutsArray = object.get("shortcuts").getAsJsonArray();

				for (int j = 0; j < shortcutsArray.size(); j++) {
					final String emoji = shortcutsArray.get(j).getAsString();
					tempShortcuts.put(emoji, name);
				}

			}

		} catch (final Exception e) {
			e.printStackTrace();
		}

		shortcuts = Collections.unmodifiableMap(tempShortcuts);
	}

	public static String escapeShortcuts(String string) {
		for (final Entry<String, String> entry : EmojiUtil.shortcuts.entrySet()) {
			string = string.replace(entry.getKey(), entry.getValue());

		}
		return string;
	}

	public static String toDiscord(String string) {
		for (final Entry<String, String> entry : EmojiUtil.toDiscord.entrySet()) {
			string = string.replace(entry.getValue(), entry.getKey());
		}
		return string;
	}

	public static String toUTF8(String string) {
		for (final Entry<String, String> entry : EmojiUtil.toUTF8.entrySet()) {
			string = string.replace(entry.getKey(), entry.getValue());
		}
		return string;
	}
}
