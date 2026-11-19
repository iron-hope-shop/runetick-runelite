package com.runetick;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * One-shot listener on 127.0.0.1 that receives the player's Runetick sign-in from their browser.
 *
 * The plugin opens {@code <site>/link?port=..&state=..}. Once the player confirms there, the site
 * sends the browser to {@code http://127.0.0.1:<port>/callback#state=..&apiKey=..&refreshToken=..}.
 * The page served here reads the fragment (which browsers never send over the network), removes it
 * from the address bar and posts it back to {@code /token}.
 */
public class LinkServer
{
	public interface Listener
	{
		void linked(String apiKey, String refreshToken) throws IOException;
	}

	private static final int MAX_BODY_BYTES = 16 * 1024;

	private static final String CALLBACK_PAGE = "<!doctype html><html><head><meta charset=\"utf-8\">"
		+ "<title>Runetick</title>"
		+ "<style>body{font-family:system-ui,sans-serif;background:#111;color:#eee;display:grid;"
		+ "place-items:center;min-height:100vh;margin:0}p{font-size:1.1rem}</style></head>"
		+ "<body><p id=\"m\">Connecting RuneLite to Runetick\u2026</p><script>"
		+ "var body=location.hash.slice(1);history.replaceState(null,'',location.pathname);"
		+ "var m=document.getElementById('m');"
		+ "fetch('/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:body})"
		+ ".then(function(r){m.textContent=r.ok?'RuneLite is connected to Runetick. You can close this tab.'"
		+ ":'Could not connect. Go back to RuneLite and press Connect again.';})"
		+ ".catch(function(){m.textContent='Could not reach RuneLite. Press Connect again.';});"
		+ "</script></body></html>";

	private final HttpServer server;
	private final String state;
	private final Listener listener;
	private boolean stopped;

	public LinkServer(Listener listener) throws IOException
	{
		this.listener = listener;

		byte[] random = new byte[24];
		new SecureRandom().nextBytes(random);
		StringBuilder sb = new StringBuilder();
		for (byte b : random)
		{
			sb.append(String.format("%02x", b));
		}
		state = sb.toString();

		server = HttpServer.create(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0), 4);
		server.createContext("/callback", this::handleCallback);
		server.createContext("/token", this::handleToken);
		server.start();
	}

	public int getPort()
	{
		return server.getAddress().getPort();
	}

	public String getState()
	{
		return state;
	}

	public synchronized void stop()
	{
		if (!stopped)
		{
			stopped = true;
			server.stop(1);
		}
	}

	/**
	 * Refuse requests addressed to any other host name, so a web page cannot reach this
	 * listener by pointing its own domain at 127.0.0.1.
	 */
	private boolean hostOk(HttpExchange exchange)
	{
		return ("127.0.0.1:" + getPort()).equals(exchange.getRequestHeaders().getFirst("Host"));
	}

	private void handleCallback(HttpExchange exchange) throws IOException
	{
		if (!hostOk(exchange) || !"GET".equals(exchange.getRequestMethod()))
		{
			respond(exchange, 403, "text/plain", "forbidden");
			return;
		}
		respond(exchange, 200, "text/html; charset=utf-8", CALLBACK_PAGE);
	}

	private void handleToken(HttpExchange exchange) throws IOException
	{
		if (!hostOk(exchange) || !"POST".equals(exchange.getRequestMethod()))
		{
			respond(exchange, 403, "text/plain", "forbidden");
			return;
		}

		Map<String, String> form = parseForm(readBody(exchange.getRequestBody()));
		String gotState = form.getOrDefault("state", "");
		String apiKey = form.getOrDefault("apiKey", "");
		String refreshToken = form.getOrDefault("refreshToken", "");

		boolean stateOk = MessageDigest.isEqual(
			state.getBytes(StandardCharsets.UTF_8), gotState.getBytes(StandardCharsets.UTF_8));
		if (!stateOk)
		{
			respond(exchange, 403, "text/plain", "bad state");
			return;
		}
		if (apiKey.isEmpty() || refreshToken.isEmpty())
		{
			respond(exchange, 400, "text/plain", "missing sign-in");
			return;
		}

		try
		{
			listener.linked(apiKey, refreshToken);
		}
		catch (IOException | RuntimeException e)
		{
			respond(exchange, 500, "text/plain", "could not save sign-in");
			throw e;
		}

		respond(exchange, 200, "text/plain", "ok");
		stop();
	}

	private static String readBody(InputStream in) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int n;
		while ((n = in.read(buf)) != -1)
		{
			out.write(buf, 0, n);
			if (out.size() > MAX_BODY_BYTES)
			{
				throw new IOException("request too large");
			}
		}
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

	static Map<String, String> parseForm(String body)
	{
		Map<String, String> out = new HashMap<>();
		for (String pair : body.split("&"))
		{
			int eq = pair.indexOf('=');
			if (eq <= 0)
			{
				continue;
			}
			try
			{
				out.put(
					URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
					URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
			}
			catch (IllegalArgumentException e)
			{
				// skip a badly encoded pair
			}
		}
		return out;
	}

	private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException
	{
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", contentType);
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody())
		{
			os.write(bytes);
		}
	}
}
