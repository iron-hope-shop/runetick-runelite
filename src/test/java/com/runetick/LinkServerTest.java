package com.runetick;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class LinkServerTest
{
	private final OkHttpClient http = new OkHttpClient();
	private final List<String> received = new ArrayList<>();
	private LinkServer server;

	@Before
	public void setUp() throws Exception
	{
		server = new LinkServer((apiKey, refreshToken) -> received.add(apiKey + "|" + refreshToken));
	}

	@After
	public void tearDown()
	{
		server.stop();
	}

	private String url(String path)
	{
		return "http://127.0.0.1:" + server.getPort() + path;
	}

	private Response postToken(String state, String apiKey, String refreshToken) throws IOException
	{
		return http.newCall(new Request.Builder()
			.url(url("/token"))
			.post(new FormBody.Builder().add("state", state).add("apiKey", apiKey).add("refreshToken", refreshToken).build())
			.build()).execute();
	}

	@Test
	public void servesCallbackPageThatPostsTheFragmentBack() throws Exception
	{
		try (Response response = http.newCall(new Request.Builder().url(url("/callback")).build()).execute())
		{
			assertEquals(200, response.code());
			assertEquals("no-store", response.header("Cache-Control"));
			String html = response.body().string();
			assertTrue(html.contains("location.hash"));
			assertTrue(html.contains("history.replaceState"));
			assertTrue(html.contains("fetch('/token'"));
		}
	}

	@Test
	public void acceptsSignInWithMatchingStateThenStops() throws Exception
	{
		try (Response response = postToken(server.getState(), "web-key", "refresh+token/with=odd&chars"))
		{
			assertEquals(200, response.code());
		}
		assertEquals(1, received.size());
		assertEquals("web-key|refresh+token/with=odd&chars", received.get(0));

		// one-shot: the listener is gone afterwards
		try (Response again = postToken(server.getState(), "web-key", "x"))
		{
			fail("expected the server to be stopped, got HTTP " + again.code());
		}
		catch (IOException expected)
		{
			// expected
		}
	}

	@Test
	public void rejectsWrongStateMissingFieldsAndForeignHost() throws Exception
	{
		try (Response response = postToken("wrong", "web-key", "refresh"))
		{
			assertEquals(403, response.code());
		}
		try (Response response = postToken(server.getState(), "", "refresh"))
		{
			assertEquals(400, response.code());
		}
		try (Response response = http.newCall(new Request.Builder().url(url("/token")).build()).execute())
		{
			assertEquals(403, response.code());
		}
		// a page that pointed its own domain at 127.0.0.1
		try (Response response = http.newCall(new Request.Builder()
			.url(url("/token"))
			.header("Host", "evil.example:" + server.getPort())
			.post(new FormBody.Builder().add("state", server.getState()).add("apiKey", "k").add("refreshToken", "t").build())
			.build()).execute())
		{
			assertEquals(403, response.code());
		}
		assertTrue(received.isEmpty());
	}

	@Test
	public void listenerFailureIsReportedToTheBrowser() throws Exception
	{
		server.stop();
		server = new LinkServer((apiKey, refreshToken) ->
		{
			throw new IOException("sign-in did not work");
		});
		try (Response response = postToken(server.getState(), "web-key", "refresh"))
		{
			assertEquals(500, response.code());
		}
	}

	@Test
	public void stateIsRandomPerServer() throws Exception
	{
		LinkServer other = new LinkServer((a, b) -> { });
		try
		{
			assertEquals(48, server.getState().length());
			assertNotEquals(server.getState(), other.getState());
		}
		finally
		{
			other.stop();
		}
	}
}
