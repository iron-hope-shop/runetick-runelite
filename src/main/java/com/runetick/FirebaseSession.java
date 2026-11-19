package com.runetick;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The player's Runetick (Firebase) sign-in. Holds the long-lived refresh token handed over by
 * the Runetick website and trades it for short-lived ID tokens that the Runetick API accepts.
 * All network calls block; call from a background thread.
 */
@Singleton
public class FirebaseSession
{
	static final String TOKEN_URL = "https://securetoken.googleapis.com/v1/token";

	/** Errors that mean the sign-in is gone for good and the player has to connect again. */
	private static final Set<String> SIGNED_OUT_ERRORS = new HashSet<>(Arrays.asList(
		"TOKEN_EXPIRED", "USER_DISABLED", "USER_NOT_FOUND", "INVALID_REFRESH_TOKEN", "INVALID_GRANT_TYPE"));

	private static final long EXPIRY_MARGIN_MS = 60_000;

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final SessionStore store;
	private final HttpUrl tokenUrl;

	private boolean loaded;
	private SessionStore.Session session;
	private String idToken;
	private long idTokenExpiresAt;

	@Inject
	public FirebaseSession(OkHttpClient httpClient, Gson gson, SessionStore store)
	{
		this(httpClient, gson, store, HttpUrl.parse(TOKEN_URL));
	}

	FirebaseSession(OkHttpClient httpClient, Gson gson, SessionStore store, HttpUrl tokenUrl)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.store = store;
		this.tokenUrl = tokenUrl;
	}

	public synchronized boolean isLinked() throws IOException
	{
		return current() != null;
	}

	public synchronized void link(String apiKey, String refreshToken) throws IOException
	{
		SessionStore.Session s = new SessionStore.Session(apiKey, refreshToken);
		store.save(s);
		session = s;
		loaded = true;
		invalidateIdToken();
	}

	public synchronized void unlink() throws IOException
	{
		session = null;
		loaded = true;
		invalidateIdToken();
		store.clear();
	}

	/** Forget the cached ID token, e.g. after the API rejected it. */
	public synchronized void invalidateIdToken()
	{
		idToken = null;
		idTokenExpiresAt = 0;
	}

	/**
	 * @return a valid ID token, refreshing it if needed
	 * @throws NotLinkedException if the player is not connected, or the sign-in was revoked
	 */
	public synchronized String getIdToken() throws IOException
	{
		SessionStore.Session s = current();
		if (s == null)
		{
			throw new NotLinkedException("Not connected to Runetick");
		}

		if (idToken != null && System.currentTimeMillis() < idTokenExpiresAt - EXPIRY_MARGIN_MS)
		{
			return idToken;
		}

		Request request = new Request.Builder()
			.url(tokenUrl.newBuilder().addQueryParameter("key", s.getApiKey()).build())
			.post(new FormBody.Builder()
				.add("grant_type", "refresh_token")
				.add("refresh_token", s.getRefreshToken())
				.build())
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			ResponseBody body = response.body();
			JsonObject json = parse(body == null ? "" : body.string());

			if (!response.isSuccessful())
			{
				String error = errorMessage(json);
				if (SIGNED_OUT_ERRORS.contains(error))
				{
					unlink();
					throw new NotLinkedException("Runetick sign-in ended (" + error + "); connect again");
				}
				throw new IOException("Runetick sign-in refresh failed: HTTP " + response.code() + " " + error);
			}

			String newIdToken = string(json, "id_token");
			if (newIdToken == null)
			{
				throw new IOException("Runetick sign-in refresh returned no token");
			}

			long expiresInSec = 3600;
			try
			{
				expiresInSec = Long.parseLong(string(json, "expires_in"));
			}
			catch (NumberFormatException e)
			{
				// keep the default
			}

			idToken = newIdToken;
			idTokenExpiresAt = System.currentTimeMillis() + expiresInSec * 1000;

			String rotated = string(json, "refresh_token");
			if (rotated != null && !rotated.equals(s.getRefreshToken()))
			{
				session = new SessionStore.Session(s.getApiKey(), rotated);
				store.save(session);
			}
			return idToken;
		}
	}

	private SessionStore.Session current() throws IOException
	{
		if (!loaded)
		{
			session = store.load();
			loaded = true;
		}
		return session;
	}

	private JsonObject parse(String body)
	{
		try
		{
			JsonObject json = gson.fromJson(body, JsonObject.class);
			return json == null ? new JsonObject() : json;
		}
		catch (JsonParseException e)
		{
			return new JsonObject();
		}
	}

	private static String string(JsonObject json, String key)
	{
		return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : null;
	}

	private static String errorMessage(JsonObject json)
	{
		if (json.has("error") && json.get("error").isJsonObject())
		{
			String message = string(json.getAsJsonObject("error"), "message");
			return message == null ? "" : message;
		}
		return "";
	}

	public static class NotLinkedException extends IOException
	{
		public NotLinkedException(String message)
		{
			super(message);
		}
	}
}
