package org.molgenis.data.elasticsearch.client;

import org.molgenis.data.elasticsearch.index.IndexConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetSocketAddress;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Spring config for Elasticsearch server. Use this in your own app by importing this in your spring config:
 * <code> @Import(ElasticsearchConfig.class)</code>
 *
 * @author erwin
 */
@Configuration
@EnableScheduling
@Import({ IndexConfig.class })
public class ElasticsearchConfig
{
	@Value("${elasticsearch.cluster.name:molgenis}")
	private String elasticsearchClusterName;

	@Value("${elasticsearch.transport.addresses:127.0.0.1:9300}")
	private List<String> elasticsearchTransportAddresses;

	@Bean(destroyMethod = "close")
	public ClientFactory elasticsearchServiceFactory()
	{
		if (elasticsearchClusterName == null)
		{
			throw new IllegalArgumentException("Property 'elasticsearch.cluster.name' cannot be null");
		}
		if (elasticsearchTransportAddresses == null || elasticsearchTransportAddresses.isEmpty())
		{
			throw new IllegalArgumentException("Property 'elasticsearch.transport.addresses' cannot be null or empty");
		}

		List<InetSocketAddress> ipSocketAddresses = toIpSocketAddresses(elasticsearchTransportAddresses);
		return new ClientFactory(elasticsearchClusterName, ipSocketAddresses);
	}

	@Bean
	public ElasticsearchClientFacade elasticsearchClientFacade()
	{
		return new ElasticsearchClientFacade(elasticsearchServiceFactory().getClient());
	}

	private List<InetSocketAddress> toIpSocketAddresses(List<String> elasticsearchTransportAddresses)
	{
		return elasticsearchTransportAddresses.stream().map(this::toIpSocketAddress).collect(toList());
	}

	private InetSocketAddress toIpSocketAddress(String elasticsearchTransportAddress)
	{
		int idx = elasticsearchTransportAddress.lastIndexOf(':');
		if (idx == -1 || idx == elasticsearchTransportAddress.length() - 1)
		{
			throw new IllegalArgumentException(
					format("Invalid transport address '%s' in property elasticsearch.transport.addresses. Transport address should be of form 'hostname:port'",
							elasticsearchTransportAddress));
		}
		String hostname = elasticsearchTransportAddress.substring(0, idx);
		int port;
		try
		{
			port = Integer.valueOf(elasticsearchTransportAddress.substring(idx + 1));
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(
					format("Invalid transport address '%s' in property elasticsearch.transport.addresses. Transport address should be of form 'hostname:port'",
							elasticsearchTransportAddress));
		}
		return new InetSocketAddress(hostname, port);
	}
}
