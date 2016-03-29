package com.almightyalpaca.discord.bot.system.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.almightyalpaca.discord.bot.system.util.emoji.EmojiUtil;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class NoteHubUploader {

	public static URL upload(String text, final String plainPassword) throws IOException, UnirestException {
		Objects.requireNonNull(text);
		Objects.requireNonNull(plainPassword);

		text = EmojiUtil.toUTF8(text);

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
