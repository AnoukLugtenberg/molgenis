package org.molgenis.rdconnect;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.molgenis.data.Entity;
import org.molgenis.data.MolgenisDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.stream.JsonReader;

// FIXME handle timeouts
@Service
public class IdCardBiobankServiceImpl implements IdCardBiobankService
{
	private static final Logger LOG = LoggerFactory.getLogger(IdCardBiobankServiceImpl.class);

	private static final int ID_CARD_CONNECT_TIMEOUT = 2000;
	private static final int ID_CARD_CONNECTION_REQUEST_TIMEOUT = 2000;
	private static final int ID_CARD_SOCKET_TIMEOUT = 2000;

	private final HttpClient httpClient;
	private final IdCardBiobankIndexerSettings idCardBiobankIndexerSettings;
	private final RequestConfig requestConfig;
	private final IdCardBiobankMapper idCardBiobankMapper;

	@Autowired
	public IdCardBiobankServiceImpl(HttpClient httpClient, IdCardBiobankIndexerSettings idCardBiobankIndexerSettings,
			IdCardBiobankMapper idCardBiobankMapper)
	{
		this.httpClient = requireNonNull(httpClient);
		this.idCardBiobankIndexerSettings = requireNonNull(idCardBiobankIndexerSettings);
		this.idCardBiobankMapper = requireNonNull(idCardBiobankMapper);

		this.requestConfig = RequestConfig.custom().setConnectTimeout(ID_CARD_CONNECT_TIMEOUT)
				.setConnectionRequestTimeout(ID_CARD_CONNECTION_REQUEST_TIMEOUT)
				.setSocketTimeout(ID_CARD_SOCKET_TIMEOUT).build();
	}

	public IdCardBiobank getIdCardBiobank(String id)
	{
		// Construct uri
		StringBuilder uriBuilder = new StringBuilder().append(idCardBiobankIndexerSettings.getApiBaseUri()).append('/')
				.append(idCardBiobankIndexerSettings.getBiobankResource()).append('/').append(id);

		return getIdCardResource(uriBuilder.toString(), new JsonResponseHandler<IdCardBiobank>()
		{
			@Override
			public IdCardBiobank deserialize(JsonReader jsonReader) throws IOException
			{
				return idCardBiobankMapper.toIdCardBiobank(jsonReader);
			}
		});
	}

	@Override
	public Iterable<Entity> getIdCardBiobanks(Iterable<String> ids)
	{
		// FIXME batching for each x ids
		String value = StreamSupport.stream(ids.spliterator(), false).collect(Collectors.joining(",", "[", "]"));
		try
		{
			value = URLEncoder.encode(value, UTF_8.name());
		}
		catch (UnsupportedEncodingException e1)
		{
			throw new RuntimeException(e1);
		}
		StringBuilder uriBuilder = new StringBuilder().append(idCardBiobankIndexerSettings.getApiBaseUri()).append('/')
				.append(idCardBiobankIndexerSettings.getBiobankCollectionSelectionResource()).append('/').append(value);

		return getIdCardResource(uriBuilder.toString(), new JsonResponseHandler<Iterable<Entity>>()
		{
			@Override
			public Iterable<Entity> deserialize(JsonReader jsonReader) throws IOException
			{
				return idCardBiobankMapper.toIdCardBiobanks(jsonReader);
			}
		});
	}

	private <T> T getIdCardResource(String url, ResponseHandler<T> responseHandler)
	{
		HttpGet request = new HttpGet(url);
		request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		request.setConfig(requestConfig);
		try
		{
			LOG.info("Retrieving [" + url + "]");
			return httpClient.execute(request, responseHandler);
		}
		catch (IOException e)
		{
			throw new MolgenisDataException(e);
		}
	}

	@Override
	public Iterable<Entity> getIdCardBiobanks()
	{
		// Construct uri
		StringBuilder uriBuilder = new StringBuilder().append(idCardBiobankIndexerSettings.getApiBaseUri()).append('/')
				.append(idCardBiobankIndexerSettings.getBiobankCollectionResource());

		// Retrieve biobank ids
		Iterable<IdCardOrganization> idCardOrganizations = getIdCardResource(uriBuilder.toString(),
				new JsonResponseHandler<Iterable<IdCardOrganization>>()
				{
					@Override
					public Iterable<IdCardOrganization> deserialize(JsonReader jsonReader) throws IOException
					{
						return idCardBiobankMapper.toIdCardOrganizations(jsonReader);
					}
				});

		// Retrieve biobanks
		return this.getIdCardBiobanks(new Iterable<String>()
		{
			@Override
			public Iterator<String> iterator()
			{
				return StreamSupport.stream(idCardOrganizations.spliterator(), false)
						.map(IdCardOrganization::getOrganizationId).iterator();
			}
		});
	}

	private static abstract class JsonResponseHandler<T> implements ResponseHandler<T>
	{
		@Override
		public T handleResponse(final HttpResponse response) throws ClientProtocolException, IOException
		{
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() < 100 || statusLine.getStatusCode() >= 300)
			{
				throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
			}

			HttpEntity entity = response.getEntity();
			if (entity == null)
			{
				throw new ClientProtocolException("Response contains no content");
			}

			JsonReader jsonReader = new JsonReader(new InputStreamReader(entity.getContent(), UTF_8));
			try
			{
				return deserialize(jsonReader);
			}
			finally
			{
				jsonReader.close();
			}
		}

		public abstract T deserialize(JsonReader jsonReader) throws IOException;
	}
}