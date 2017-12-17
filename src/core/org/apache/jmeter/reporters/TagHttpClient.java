package org.apache.jmeter.reporters;

import java.io.IOException;
import java.net.URI;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class TagHttpClient
{
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    CloseableHttpClient client;
    HttpPost httpPost;
    boolean isEnabled;

    TagHttpClient(int connctionTimeOut, int requestTimeOut, int socketTimeOut, URI influxURI)
    {
        this.connManager.setDefaultMaxPerRoute(2);
        this.connManager.setMaxTotal(2);

        RequestConfig config = RequestConfig.custom().setConnectTimeout(connctionTimeOut).setConnectionRequestTimeout(requestTimeOut).setSocketTimeout(socketTimeOut).build();

        this.client = HttpClients.custom().setConnectionManager(this.connManager).setDefaultRequestConfig(config).build();

        this.httpPost = new HttpPost();
        this.httpPost.setURI(influxURI);
        this.httpPost.setHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        this.isEnabled = true;
    }

    public CloseableHttpClient getClient()
    {
        return this.client;
    }

    public HttpPost getHTTPPost()
    {
        return this.httpPost;
    }

    public boolean isOpen()
    {
        return this.isEnabled;
    }

    public void close()
            throws IOException
    {
        this.client.close();
        this.connManager.close();
    }
}
