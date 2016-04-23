package com.almightyalpaca.discord.bot.system.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class NoteHubUploader {

	private static final Pattern pattern = Pattern
		.compile("((([\uD83C\uDF00-\uD83D\uDDFF]|[\uD83D\uDE00-\uD83D\uDE4F]|[\uD83D\uDE80-\uD83D\uDEFF]|[\u2600-\u26FF]|[\u2700-\u27BF])[\\x{1F3FB}-\\x{1F3FF}]?))");

	private static String grabTheRightIcon(final String rawText) {
		return NoteHubUploader.toCodePoint(rawText.indexOf('\u200D') < 0 ? rawText.replace("\uFE0F", "") : rawText, null);
	}

	private static String parse(final String text) {
		final StringBuffer sb = new StringBuffer();
		final Matcher matcher = NoteHubUploader.pattern.matcher(text);
		String iconUrl = null;
		while (matcher.find()) {
			final String rawCode = matcher.group(2);
			final String iconId = NoteHubUploader.grabTheRightIcon(rawCode);
			iconUrl = "https://twemoji.maxcdn.com/2/svg/" + iconId + ".svg";
			matcher.appendReplacement(sb, "<img class=\"emoji\" draggable=\"false\" margin=\" 0 .05em 0 .1em!important\" vertical-align=\"-.4em\" alt=\"" + rawCode
				+ "\"  height=\"22px\" width=\"22px\" src=\"" + iconUrl + "\" />");
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private static String toCodePoint(final String unicodeSurrogates, String sep) {
		final ArrayList<String> r = new ArrayList<>();
		int c = 0, p = 0, i = 0;
		if (sep == null) {
			sep = "-";
		}
		while (i < unicodeSurrogates.length()) {
			c = unicodeSurrogates.charAt(i++);
			if (p != 0) {
				r.add(Integer.toString(0x10000 + (p - 0xD800 << 10) + c - 0xDC00, 16));
				p = 0;
			} else if (0xD800 <= c && c <= 0xDBFF) {
				p = c;
			} else {
				r.add(Integer.toString(c, 16));
			}
		}
		return StringUtils.join(r, sep);
	}

	public static URL upload(String text, final String plainPassword) throws IOException, UnirestException {
		Objects.requireNonNull(text);
		Objects.requireNonNull(plainPassword);

		text = NoteHubUploader.parse(text);

		final Connection.Response loginForm = Jsoup.connect("https://notehub.org/new").method(Connection.Method.GET).execute();

		final Document doc = Jsoup.parse(loginForm.body());

		final String action = doc.getElementById("action").attr("value");
		final String id = doc.getElementById("id").attr("value");
		final String session = doc.getElementById("session").attr("value");
		final String note = URLEncoder.encode(text.replace("\n", "\n\n"), "UTF-8");
		final String signature = DigestUtils.md5Hex(session + text.replaceAll("\n", "").replaceAll("\r", ""));
		final String password = DigestUtils.md5Hex(plainPassword);
		final String data = "action=" + action + "&id=" + id + "&password=" + password + "&session=" + session + "&signature=" + signature + "&note=" + note;

		final String response = Unirest.post("https://notehub.org/note").header("content-type", "application/x-www-form-urlencoded").header("accept", "text/html").body(data).asString().getBody();

		final String subURL = response.substring(response.indexOf("\"") + 1, response.indexOf("\"", response.indexOf("\"") + 1));

		if (!subURL.startsWith("/")) {
			throw new RuntimeException("Error while parsing notehub result: " + response);
		}

		return new URL("https://notehub.org" + subURL);
	}

}
