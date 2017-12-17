package org.apache.jmeter.reporters;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;

class InfluxMetricCollector
{
    private final StringBuilder samples;
    private final String suite;
    private final String application;

    public InfluxMetricCollector(String project, String suite)
    {
        this.application = project;
        this.suite = suite;
        this.samples = new StringBuilder();
    }

    public void addSample(SampleResult result)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("samples");

        sb.append(",application=").append(escapeTag(this.application));

        sb.append(",suite=").append(escapeTag(this.suite));

        sb.append(",label=").append(escapeTag(result.getSampleLabel()));

        sb.append(",status=");
        if (result.isSuccessful()) {
            sb.append("Success");
        } else {
            sb.append("Failure");
        }
        sb.append(",threadname=").append(escapeTag(result.getThreadName()));

        sb.append(",responsecode=").append(escapeResponceCode(result.getResponseCode()));

        sb.append(" ").append("ath=").append(JMeterContextService.getThreadCounts().activeThreads);

        sb.append(",duration=").append(result.getTime());

        sb.append(",latency=").append(result.getLatency());

        sb.append(",bytes=").append(result.getBytesAsLong());

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
