package org.apache.jmeter.reporters;

import java.util.concurrent.Callable;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

class HTTPCallable
        implements Callable<Void>
{
    final String metric;
    final HttpPost http;
    final CloseableHttpClient client;

    public HTTPCallable(CloseableHttpClient client, HttpPost http, String metric)
    {
        this.client = client;
        this.http = http;
        this.metric = metric;
    }

    public Void call()
            throws Exception
    {
        this.http.setEntity(new StringEntity(this.metric));
        CloseableHttpResponse response = this.client.execute(this.http);
        EntityUtils.consumeQuietly(response.getEntity());
        response.close();
        return null;
    }
}
