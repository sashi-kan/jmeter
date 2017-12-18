 package org.apache.jmeter.reporters;

 import java.io.PrintStream;
         import java.io.Serializable;
         import java.net.URISyntaxException;
         import java.text.DecimalFormat;
         import java.util.Iterator;
         import java.util.Map;
         import java.util.Map.Entry;
         import java.util.Set;
         import java.util.concurrent.ConcurrentHashMap;
         import org.apache.jmeter.control.TransactionController;
         import org.apache.jmeter.engine.util.NoThreadClone;
         import org.apache.jmeter.samplers.Remoteable;
         import org.apache.jmeter.samplers.SampleEvent;
         import org.apache.jmeter.samplers.SampleListener;
         import org.apache.jmeter.samplers.SampleResult;
         import org.apache.jmeter.testelement.AbstractTestElement;
         import org.apache.jmeter.testelement.TestStateListener;
         import org.apache.jmeter.threads.JMeterContextService;
         import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
         import org.apache.jmeter.util.JMeterUtils;
         import org.apache.jorphan.util.JOrphanUtils;
         import org.slf4j.Logger;
         import org.slf4j.LoggerFactory;

 public class Summariser
           extends AbstractTestElement
           implements Serializable, SampleListener, TestStateListener, NoThreadClone, Remoteable
         {
       private static final long serialVersionUID = 234L;
  private static final Logger log = LoggerFactory.getLogger(Summariser.class);


  private static final long INTERVAL = JMeterUtils.getPropDefault("summariser.interval", 30);


  private static final boolean TOLOG = JMeterUtils.getPropDefault("summariser.log", true);


  private static final boolean TOOUT = JMeterUtils.getPropDefault("summariser.out", true);


  private static final boolean IGNORE_TC_GENERATED_SAMPLERESULT = JMeterUtils.getPropDefault("summariser.ignore_transaction_controller_sample_result", false);

  private static boolean TOINFLUX = JMeterUtils.getPropDefault("summariser.influx.out.enabled", false);
       private static final int INTERVAL_WINDOW = 5;
private static final Object LOCK = new Object();
private static final Map<String, Totals> ACCUMULATORS = new ConcurrentHashMap();
private static int INSTANCE_COUNT;
private transient Totals myTotals = null;
private transient String myName;
 private InfluxMetricSender influxDB = null;
 private InfluxMetricCollector influxSampleCollector = null;

       public Summariser()
       {
       synchronized (LOCK) {
             ACCUMULATORS.clear();
             INSTANCE_COUNT = 0;
                 }
       if (TOINFLUX) {
                   try {
              this.influxDB = new InfluxMetricSender();
              this.influxSampleCollector = new InfluxMetricCollector(this.influxDB.getProject(), this.influxDB.getSuite());
                       } catch (URISyntaxException e) {
                   TOINFLUX = false;
                       }
                 }
           }

       public Summariser(String name)
       {
           this();
           setName(name);
           }

       private static class Totals
               {
        private long last = 0L;

        private final SummariserRunningSample delta = new SummariserRunningSample("DELTA");

        private final SummariserRunningSample total = new SummariserRunningSample("TOTAL");


        
             private void moveDelta()
             {
             this.total.addSample(this.delta);
             this.delta.clear();
                 }
           }

       public void sampleOccurred(SampleEvent e)
       {
       SampleResult s = e.getResult();
       if ((IGNORE_TC_GENERATED_SAMPLERESULT) && (TransactionController.isFromTransactionController(s))) {
             return;
                 }

          long now = System.currentTimeMillis() / 1000L;

          SummariserRunningSample myDelta = null;
          SummariserRunningSample myTotal = null;
          boolean reportNow = false;
          String lineProtocol = null;

       synchronized (this.myTotals) {
             if (s != null) {
           this.myTotals.delta.addSample(s);
           if (TOINFLUX) {
                 this.influxSampleCollector.addSample(s);
                             }
                       }
            
          if ((now > this.myTotals.last + 5L) && (now % INTERVAL <= 5L)) {
                reportNow = true;


                myDelta = new SummariserRunningSample(this.myTotals.delta);
                this.myTotals.moveDelta();
                myTotal = new SummariserRunningSample(this.myTotals.total);

                this.myTotals.last = now;
                if (TOINFLUX) {
                     lineProtocol = this.influxSampleCollector.getLineProtocol();
                             }
                       }
                 }
       if (reportNow) {
             formatAndWriteToLog(this.myName, myDelta, "+");


             if (TOINFLUX) {
              this.influxDB.sendIntervalMetric(myDelta);
                this.influxDB.sendSampleMetric(lineProtocol);
                       }
            
            
           if ((myTotal != null) && (myDelta != null) && (myTotal.getNumSamples() != myDelta.getNumSamples())) {
                 formatAndWriteToLog(this.myName, myTotal, "=");
                 if (TOINFLUX) {
                      this.influxDB.sendTotalMetric(myTotal);
                             }
                       }
                 }
           }
    
       private static StringBuilder longToSb(StringBuilder sb, long l, int len) {
             sb.setLength(0);
             sb.append(l);
             return JOrphanUtils.rightAlign(sb, len);
           }
    
       private static StringBuilder doubleToSb(DecimalFormat dfDouble, StringBuilder sb, double d, int len, int frac) {
            sb.setLength(0);
            dfDouble.setMinimumFractionDigits(frac);
            dfDouble.setMaximumFractionDigits(frac);
            sb.append(dfDouble.format(d));
            return JOrphanUtils.rightAlign(sb, len);
           }

       public void sampleStarted(SampleEvent e) {}
       public void sampleStopped(SampleEvent e) {}
   public void testStarted()
       {
             testStarted("local");
           }
    
    
       public void testEnded()
       {
             testEnded("local");
           }

       public void testStarted(String host)
       {
        synchronized (LOCK) {
              this.myName = getName();
              this.myTotals = ((Totals)ACCUMULATORS.get(this.myName));
              if (this.myTotals == null) {
                this.myTotals = new Totals();
                ACCUMULATORS.put(this.myName, this.myTotals);
         }
              INSTANCE_COUNT += 1;
                 }
           }

       @Override
    public void testEnded(String host) {
        Set<Entry<String, Totals>> totals = null;
        synchronized (LOCK) {
            INSTANCE_COUNT--;
            if (INSTANCE_COUNT <= 0){
                totals = ACCUMULATORS.entrySet();
            }
        }
        if (totals == null) {// We're not done yet
            return;
        }
        for(Map.Entry<String, Totals> entry : totals){
            String name = entry.getKey();
            Totals total = entry.getValue();
            total.delta.setEndTime(); // ensure delta has correct end time
            // Only print final delta if there were some samples in the delta
            // and there has been at least one sample reported previously
            if (total.delta.getNumSamples() > 0 && total.total.getNumSamples() >  0) {
                formatAndWriteToLog(name, total.delta, "+");
            }
            total.moveDelta(); // This will update the total endTime
            formatAndWriteToLog(name, total.total, "=");
            if (TOINFLUX) {
                     this.influxDB.sendTotalMetric(total.total);
                     String lineProtocol = this.influxSampleCollector.getLineProtocol();
                     this.influxDB.sendSampleMetric(lineProtocol);
                     this.influxDB.shutDown();
                       }
        }
    }
    
       private void formatAndWriteToLog(String name, SummariserRunningSample summariserRunningSample, String type) {
       if ((TOOUT) || ((TOLOG) && (log.isInfoEnabled()))) {
             String formattedMessage = format(name, summariserRunningSample, type);
             if (TOLOG) {
                log.info(formattedMessage);
        }
             if (TOOUT) {
                 System.out.println(formattedMessage);
                       }
                 }
           }

       private static String format(String name, SummariserRunningSample summariserRunningSample, String type)
       {
         DecimalFormat dfDouble = new DecimalFormat("#0.0");
         StringBuilder tmp = new StringBuilder(20);
         StringBuilder sb = new StringBuilder(140);
         sb.append(name);
         sb.append(' ');
         sb.append(type);
         sb.append(' ');
         sb.append(longToSb(tmp, summariserRunningSample.getNumSamples(), 6));
         sb.append(" in ");
         long elapsed = summariserRunningSample.getElapsed();
         long elapsedSec = (elapsed + 500L) / 1000L;
         sb.append(JOrphanUtils.formatDuration(elapsedSec));
         sb.append(" = ");
         if (elapsed > 0L) {
               sb.append(doubleToSb(dfDouble, tmp, summariserRunningSample.getRate(), 6, 1));
                 } else {
                 sb.append("******");
                 }
        sb.append("/s Avg: ");
        sb.append(longToSb(tmp, summariserRunningSample.getAverage(), 5));
        sb.append(" Min: ");
        sb.append(longToSb(tmp, summariserRunningSample.getMin(), 5));
        sb.append(" Max: ");
        sb.append(longToSb(tmp, summariserRunningSample.getMax(), 5));
        sb.append(" Err: ");
        sb.append(longToSb(tmp, summariserRunningSample.getErrorCount(), 5));
        sb.append(" (");
        sb.append(summariserRunningSample.getErrorPercentageString());
        sb.append(')');
        if ("+".equals(type)) {
              JMeterContextService.ThreadCounts tc = JMeterContextService.getThreadCounts();
              sb.append(" Active: ");
              sb.append(tc.activeThreads);
              sb.append(" Started: ");
              sb.append(tc.startedThreads);
              sb.append(" Finished: ");
              sb.append(tc.finishedThreads);
                 }
            return sb.toString();
           }
     }
