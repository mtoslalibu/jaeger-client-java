/*
 * Copyright (c) 2016, Uber Technologies, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.jaegertracing.internal.samplers;

import io.jaegertracing.internal.Constants;
import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.exceptions.SamplingStrategyErrorException;
import io.jaegertracing.internal.metrics.InMemoryMetricsFactory;
import io.jaegertracing.internal.metrics.Metrics;
import io.jaegertracing.internal.samplers.http.OperationSamplingParameters;
import io.jaegertracing.internal.samplers.http.ProbabilisticSamplingStrategy;
import io.jaegertracing.internal.samplers.http.RateLimitingSamplingStrategy;
import io.jaegertracing.internal.samplers.http.SamplingStrategyResponse;
import io.jaegertracing.spi.Sampler;
import io.jaegertracing.spi.SamplingManager;
import java.util.Timer;
import java.util.TimerTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import java.util.Random;
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.Scanner; // Import the Scanner class to read text files

import java.util.HashMap;
import java.util.Map;
import java.util.*;

@SuppressWarnings("EqualsHashCode")
@ToString
@Slf4j
public class AstraeaSampler implements Sampler {
  public static final String TYPE = "astraea";
  private static final int DEFAULT_POLLING_INTERVAL_MS = 5000; // every 5 seconds

  private final int maxOperations = 2000;

  // initialized in constructor and updated from a single (poll timer) thread
  // volatile to guarantee immediate visibility of the updated sampler to other threads (remove if not a requirement)
  @Getter(AccessLevel.PACKAGE) // visible for testing
  private volatile Sampler sampler;

  private Map<String, Object> tags;


/// MERT : span decisions
  @Getter(AccessLevel.PACKAGE) // visible for testing
  // private volatile Map<String, String> map spanDecisions;
  private volatile Set<String>  spanDecisions = new HashSet<String>();

  // most of the time, toString here is called from the JaegerTracer, which holds this as well
  @ToString.Exclude private final String serviceName;

  @ToString.Exclude private final Timer pollTimer;
  

  private AstraeaSampler(Builder builder) {
    System.out.println("** astraea geliyor");
    tags = new HashMap<String, Object>();
    tags.put(Constants.SAMPLER_TYPE_TAG_KEY, TYPE);

    this.serviceName = builder.serviceName;
    // this.manager = builder.samplingManager;
    // this.metrics = builder.metrics;

    // if (builder.initialSampler != null) {
    //   this.sampler = builder.initialSampler;
    // } else {
    //   this.sampler = new ProbabilisticSampler(ProbabilisticSampler.DEFAULT_SAMPLING_PROBABILITY);
    // }

    pollTimer = new Timer(true); // true makes this a daemon thread
    pollTimer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            try {
              System.out.println("********* deneme1");
            //   updateSampler();
            updateSpanSamplingDecisions();
            } catch (Exception e) { // keep the timer thread alive
            //   log.error("Failed to update sampler", e);
            System.out.print("Error1");
            }
          }
        },
        0,
        builder.pollingIntervalMs);
  }

  private void updateSpanSamplingDecisions() {
      System.out.println("********* reading from a file");

      /// read from file
      try {
        // File myObj = new File("/Users/merttoslali/Desktop/fall21/IBM/tech/jaeger-spring/opentracing-microservices-example/span-decision.txt");
        File myObj = new File("/span-decision.txt");
        Scanner myReader = new Scanner(myObj);
        this.spanDecisions.clear();
        while (myReader.hasNextLine()) {
          String data = myReader.nextLine();
          System.out.println(data);
          this.spanDecisions.add(data);
        }
        myReader.close();
      } catch (FileNotFoundException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
      }
   
    // String aToZ="ABCD1234"; // 36 letter.
    // Random rand=new Random();
    // StringBuilder res=new StringBuilder();
    // for (int i = 0; i < 17; i++) {
    //    int randIndex=rand.nextInt(aToZ.length()); 
    //    res.append(aToZ.charAt(randIndex));            
    // }

    // this.spanDecisions = res.toString();;
    // System.out.println("********* deneme13");

    System.out.println("Span decision now " + this.spanDecisions);

  }


  @Override
  public SamplingStatus sample(String operation, long id) {

    return SamplingStatus.of(true, tags);

    // System.out.println("********* deneme-sampling decisions" + this.spanDecisions);
    // System.out.println("span now: " +  serviceName + " " + operation);

    // if (this.spanDecisions.contains(serviceName + ":" + operation)){
    //   System.out.println("999 containing so disabling");
    //   return SamplingStatus.of(false, tags);
    // }
    // System.out.println("11 not containing so enabling");
    // return SamplingStatus.of(true, tags);
    
    // if (operation.equalsIgnoreCase("GET")){
    //   System.out.println("********* false donuyor2");
    //   SamplingStatus.of(false, tags);
    // }
    // System.out.println("********* true donuyor2");
    // return SamplingStatus.of(true, tags);
    // return sampler.sample(operation, id);
  }

  public boolean sampleReport(JaegerSpan span){
    System.out.println("////// Reporter is calling sampling: "+ span.getServiceName() + ":" + span.getOperationName());
    if (this.spanDecisions.contains(span.getServiceName()  + ":" + span.getOperationName())){
        System.out.println("999 containing so disabling");
        return false;
      }
      System.out.println("not containing so enabling");
      return true;
  }

  @Override
  public boolean equals(Object sampler) {
    if (this == sampler) {
      return true;
    }
    if (sampler instanceof AstraeaSampler) {
      AstraeaSampler remoteSampler = ((AstraeaSampler) sampler);
      return this.sampler.equals(remoteSampler.sampler);
    }
    return false;
  }

  @Override
  public void close() {
    pollTimer.cancel();
  }

  public static class Builder {
    private final String serviceName;
    // private SamplingManager samplingManager;
    // private Sampler initialSampler;
    // private Metrics metrics;
    private int pollingIntervalMs = DEFAULT_POLLING_INTERVAL_MS;

    public Builder(String serviceName) {
      this.serviceName = serviceName;
    }

    // public Builder withSamplingManager(SamplingManager samplingManager) {
    //   this.samplingManager = samplingManager;
    //   return this;
    // }

    // public Builder withInitialSampler(Sampler initialSampler) {
    //   this.initialSampler = initialSampler;
    //   return this;
    // }

    // public Builder withMetrics(Metrics metrics) {
    //   this.metrics = metrics;
    //   return this;
    // }

    public Builder withPollingInterval(int pollingIntervalMs) {
      this.pollingIntervalMs = pollingIntervalMs;
      return this;
    }

    public AstraeaSampler build() {
  
    //   if (initialSampler == null) {
    //     initialSampler = new ProbabilisticSampler(1);
    //   }
      
      return new AstraeaSampler(this);
    }
  }
}
