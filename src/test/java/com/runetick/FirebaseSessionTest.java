package com.runetick;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import net.runelite.client.util.Filepath;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FirebaseSessionTest
{
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private final Gson gson = new Gson();
	private MockWebServer google;
	private Path file;
	private SessionStore store;

	@Before
	public void setUp() throws Exception
	{
		google = new MockWebServer();
		google.start();
		PluginFiles files = new PluginFiles();
		files.use(Filepath.Unchecked.getRooted(tmp.getRoot().toPath()));
		store = new SessionStore(files, gson);
		file = Filepath.Unchecked.getPath(files.file("session.json"));
	}

	@After
	public void tearDown() throws Exception
	{
		google.shutdown();
	}

	private FirebaseSession newSession()
	{
		return new FirebaseSession(new OkHttpClient(), gson, store, google.url("/v1/token"));
	}

	private static MockResponse tokens(String idToken, String refreshToken, int expiresIn)
	{
		return new MockResponse().setBody("{\"id_token\":\"" + idToken + "\",\"refresh_token\":\"" + refreshToken
			+ "\",\"expires_in\":\"" + expiresIn + "\"}");
	}

	@Test
	public void notLinkedByDefault() throws Exception
	{
		FirebaseSession session = newSession();
		assertFalse(session.isLinked());
		try
		{
			session.getIdToken();
			fail("expected NotLinkedException");
		}
		catch (FirebaseSession.NotLinkedException expected)
		{
			// expected
		}
		assertEquals(0, google.getRequestCount());
	}

	@Test
	public void exchangesRefreshTokenAndCachesIdToken() throws Exception
	{
		google.enqueue(tokens("id-1", "refresh-1", 3600));
		FirebaseSession session = newSession();
		session.link("web-key", "refresh-1");

		assertEquals("id-1", session.getIdToken());
		assertEquals("id-1", session.getIdToken());
		assertEquals(1, google.getRequestCount());

		RecordedRequest request = google.takeRequest();
		assertEquals("POST", request.getMethod());
		assertEquals("/v1/token?key=web-key", request.getPath());
		assertEquals("grant_type=refresh_token&refresh_token=refresh-1", request.getBody().readUtf8());
	}

	@Test
	public void refreshesWhenIdTokenIsAboutToExpireOrInvalidated() throws Exception
	{
		google.enqueue(tokens("id-1", "refresh-1", 30));
		google.enqueue(tokens("id-2", "refresh-1", 3600));
		google.enqueue(tokens("id-3", "refresh-1", 3600));
		FirebaseSession session = newSession();
		session.link("web-key", "refresh-1");

		assertEquals("id-1", session.getIdToken());
		assertEquals("id-2", session.getIdToken());
		session.invalidateIdToken();
		assertEquals("id-3", session.getIdToken());
	}

	@Test
	public void savesSessionAndSurvivesRestart() throws Exception
	{
		google.enqueue(tokens("id-1", "refresh-2", 3600));
		google.enqueue(tokens("id-2", "refresh-2", 3600));
		FirebaseSession session = newSession();
		session.link("web-key", "refresh-1");
		session.getIdToken();

		assertTrue(Files.exists(file));

		// A new instance (client restart) picks up the rotated refresh token.
		FirebaseSession restarted = newSession();
		assertTrue(restarted.isLinked());
		assertEquals("id-2", restarted.getIdToken());
		google.takeRequest();
		assertTrue(google.takeRequest().getBody().readUtf8().endsWith("refresh_token=refresh-2"));
	}

	@Test
	public void revokedSignInUnlinks() throws Exception
	{
		google.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":{\"code\":400,\"message\":\"TOKEN_EXPIRED\"}}"));
		FirebaseSession session = newSession();
		session.link("web-key", "refresh-1");

		try
		{
			session.getIdToken();
			fail("expected NotLinkedException");
		}
		catch (FirebaseSession.NotLinkedException expected)
		{
			// expected
		}
		assertFalse(session.isLinked());
		assertFalse(Files.exists(file));
	}

	@Test
	public void temporaryFailureKeepsSignIn() throws Exception
	{
		google.enqueue(new MockResponse().setResponseCode(503).setBody("try later"));
		FirebaseSession session = newSession();
		session.link("web-key", "refresh-1");

		try
		{
			session.getIdToken();
			fail("expected IOException");
		}
		catch (FirebaseSession.NotLinkedException e)
		{
			fail("a temporary failure must not sign the player out");
		}
		catch (java.io.IOException expected)
		{
			// expected
		}
		assertTrue(session.isLinked());
	}

	@Test
	public void unlinkRemovesFileAndCorruptFileIsIgnored() throws Exception
	{
		FirebaseSession session = newSession();
		session.link("web-key", "refresh-1");
		session.unlink();
		assertFalse(Files.exists(file));

		Files.write(file, "{not json".getBytes());
		assertNull(store.load());
		assertFalse(newSession().isLinked());
	}
}
