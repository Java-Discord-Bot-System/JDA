package com.almightyalpaca.discord.bot.system.util;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class URLUtils {

	private static CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();

	public static String expand(final String urlArg) throws IOException {
		String originalUrl = urlArg;
		String newUrl = URLUtils.expandSingleLevel(originalUrl);
		while (!originalUrl.equals(newUrl)) {
			originalUrl = newUrl;
			newUrl = URLUtils.expandSingleLevel(originalUrl);
		}
		return newUrl;
	}

	public static String expandSingleLevel(final String url) throws IOException {
		HttpHead request = null;
		try {
			request = new HttpHead(url);
			final HttpResponse httpResponse = URLUtils.client.execute(request);

			final int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode != 301 && statusCode != 302) {
				return url;
			}
			final Header[] headers = httpResponse.getHeaders(HttpHeaders.LOCATION);
			if (headers.length != 1){
				throw new IllegalStateException();
			}
			final String newUrl = headers[0].getValue();
			return newUrl;
		} catch (final IllegalArgumentException uriEx) {
			return url;
		} finally {
			if (request != null) {
				request.releaseConnection();
			}
		}
	}

	public static void shutdown() {
		try {
			URLUtils.client.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
