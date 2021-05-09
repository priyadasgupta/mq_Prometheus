package com.ibm.apmoller.mq;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ibm.apmoller.pcf.ChannelMetrics;
import com.ibm.apmoller.pcf.QueueMetrics;

@Component
public class PrometheusStats {

    @Autowired
    ConnectionPool mqConnection;

    @Autowired
    ChannelMetrics channelMetricsService;

    @Autowired
    QueueMetrics queueMetricsService;

    @Autowired
    MetricsMonitor metricsMonitorService;

    @PostConstruct
    public void callInit() {
        try {
        mqConnection.createMQConnection();
        channelMetricsService.checkActiveChannels();
        queueMetricsService.checkActiveQueues();

        System.out.println("Disconnecting PCFMessageAgent");
        mqConnection.getAgent().disconnect();
        System.out.println("Done !!");
        } catch (Exception e) {
            System.err.println("Error disconnecting the PCFMessageAgent");
        }

        metricsMonitorService.checkMetrics();
    }
}
