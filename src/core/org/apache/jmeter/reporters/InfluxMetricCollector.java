package org.apache.jmeter.reporters;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InfluxMetricCollector
{
    private final StringBuilder samples;
    private final String suite;
    private final String herculesrunid;

    static Logger log = LoggerFactory.getLogger(InfluxMetricCollector.class);
    public InfluxMetricCollector(String project, String suite)
    {
        this.herculesrunid = project;
        this.suite = suite;
        this.samples = new StringBuilder();
    }

    public void addSample(SampleResult result)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("samples");

        sb.append(",herculesrunid=").append(escapeTag(this.herculesrunid));

        sb.append(",suite=").append(escapeTag(this.suite));

        sb.append(",label=").append(escapeTag(result.getSampleLabel()));

        sb.append(",status=");
        if (result.isSuccessful()) {
            sb.append("Success");
        } else {
            sb.append("Failure");
            sb.append(",failureresponse=").append(escapeTag(result.getResponseDataAsString()));
        }
        sb.append(",threadname=").append(escapeTag(result.getThreadName()));

        sb.append(",responsecode=").append(escapeResponceCode(result.getResponseCode()));

        sb.append(",responemessage=").append(escapeTag(result.getResponseMessage()));

        sb.append(" ").append("ath=").append(JMeterContextService.getThreadCounts().activeThreads);

        sb.append(",duration=").append(result.getTime());

        sb.append(",latency=").append(result.getLatency());

        sb.append(",bytes=").append(result.getBytesAsLong());

        sb.append(",idletime=").append(result.getIdleTime());

        sb.append(",connect=").append(result.getConnectTime());

        sb.append(",grpthreads=").append(result.getGroupThreads());

        sb.append(",allthreads=").append(result.getAllThreads());

        sb.append(" ").append(result.getTimeStamp()).append("000000");

        sb.append("\n");

        this.samples.append(sb.toString());
    }

    public String getLineProtocol()
    {
        String linePropertocol = this.samples.toString();
        this.samples.setLength(0);
        return linePropertocol;
    }

    public String escapeTag(String tag)
    {
        tag = tag.replaceAll(",", "\\\\,").replaceAll(" ", "\\\\ ").replaceAll("=", "\\\\=").trim();
        return tag;
    }

    public String escapeResponceCode(String code)
    {
        code = code.trim();
        if (code.length() > 5) {
            code = "0";
        }
        return code;
    }
}
