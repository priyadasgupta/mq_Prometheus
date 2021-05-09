package com.ibm.apmoller.mq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ibm.apmoller.constants.ApplicationConstants;
import com.ibm.apmoller.model.ChannelStats;
import com.ibm.apmoller.model.QueueInquireHandlerStats;
import com.ibm.apmoller.model.QueueStats;
import com.ibm.apmoller.pcf.ChannelMetrics;
import com.ibm.apmoller.pcf.QueueMetrics;

@Component
public class MetricsMonitor
{
    @Autowired
    QueueMetrics queueMetricsService;

    @Autowired
    ChannelMetrics channelMetricsService; 

    @Autowired
    private MeterRegistry meterRegistry;

    private static final String queueDepthTag = "mq:queueDepth";
    private static final String queueOpenInCountTag = "mq:queueOpenInputCount";
    private static final String queueOpenOutCountTag = "mq:queueOpenOutputCount";
    private static final String queueInquireTag = "mq:queueInquireProcesses";
    private static final String queueMaxDepthTag = "mq:maxQueueDepth";
    private static final String queueLastGetDateTimeTag = "mq:lastGetDateTime";
    private static final String queueLastPutDateTimeTag = "mq:lastPutDateTime";
    private static final String queueOldestMsgAgeTag = "mq:oldestMsgAge";
    private static final String queueDequeuedCountTag = "mq:deQueuedCount";
    private static final String queueEnqueuedCountTag = "mq:enQueuedCount";

    private static final String channelStatusTag = "mq:channelStatus";
    private static final String channelTotalMsgsTag = "mq:msgsCountOverChannel";
    private static final String channelBytesReceivedTag = "mq:bytesReceivedOverChannel";
    private static final String channelBytesSentTag = "mq:bytesSentOverChannel";
    private static final String channelMaxMessageLengthTag = "mq:maxMessageLengthOverChannel";

    //private Map<String, AtomicInteger> queueDepthMap;
    private Map<String, AtomicInteger> queueIntegerMap;
    private Map<String, AtomicLong> queueLongMap;

    //private Map<String, AtomicInteger> channelStatusMap;
    private Map<String, AtomicInteger> channelIntegerMap;
    private Map<String, AtomicLong> channelLongMap;

    private List<QueueStats> queueStatsList;
    private List<ChannelStats> channelStatsList;

    //@PostConstruct
    public void checkMetrics() {
        System.out.println("in Check Metrics");
        queueStatsList = queueMetricsService.getAllQueueStats();
        channelStatsList = channelMetricsService.getAllChannelStats();

        //queueDepthMap = new HashMap<>();
        queueIntegerMap = new HashMap<>();
        queueLongMap = new HashMap<>();

        //channelStatusMap = new HashMap<>();
        channelIntegerMap = new HashMap<>();
        channelLongMap = new HashMap<>();

        for (QueueStats qStats : this.queueStatsList) {
            System.out.println("qStats ==== deQ " + qStats.getDeQueueCount() + ", enQ "
                    + qStats.getEnQueueCount() + ", maxQdepth "
                    + qStats.getMaxQueueDepth() + ", qCluster "
                    + qStats.getQueueCluster() + ", qDepth "
                    + qStats.getQueueDepth() + ", qLastGetDate "
                    + qStats.getQueueLastGetDateTime() + ", qLastPutdate "
                    + qStats.getQueueLastPutDateTime() + ", qManager "
                    + qStats.getQueueManager() + ", qName "
                    + qStats.getQueueName());
            Tags qTag = getQueueTag(qStats);

            checkQueueDepth(qStats, qTag);
            checkIOCount(qStats, qTag);
            checkLastDateTime(qStats, qTag);
            checkOldestMsgAge(qStats, qTag);
            checkDeQueueEnQueueCount(qStats, qTag);
        }

        for (ChannelStats channelStat : this.channelStatsList) {
            System.out.println("channelStat ========== " + channelStat.getBytesReceivedOverChannel() + ", "
                    + channelStat.getBytesSentOverChannel() + ", "
                    + channelStat.getChannelCluster() + ", "
                    + channelStat.getChannelName() + ", "
                    + channelStat.getChannelStatus() + ", "
                    + channelStat.getChannelType() + ", "
                    + channelStat.getMaxMsgLength() + ", "
                    + channelStat.getMsgsCountOverChannel() + ", "
                    + channelStat.getQueueManager());
            Tags cTag = getChannelTag(channelStat);

            checkChannelStatus(channelStat, cTag);
            checkChannelMsgCount(channelStat, cTag);
            checkBytesReceived(channelStat, cTag);
            checkBytesSent(channelStat, cTag);
            checkMaxMsgLength(channelStat, cTag);
        }
    }

    private Tags getQueueTag(QueueStats qStats) {
        return (Tags.of(new String[] { "queueManagerName", qStats.getQueueManager(), "queueName", qStats.getQueueName(),
                "queueType", qStats.getQueueType(), "usage", qStats.getQueueUsage(), "cluster", qStats.getQueueCluster()}));
    }

    private Tags getInquireQueueTag(QueueInquireHandlerStats qInquireHandler, String qManager, String qName, String qCluster) {
        return (Tags.of(new String[] { "queueManagerName", qManager, "queueName", qName, "cluster", qCluster,
                "applicationTag", qInquireHandler.getApplicationTag(), "userId", qInquireHandler.getUserId()}));
    }

    private Tags getChannelTag(ChannelStats cStats) {
        return (Tags.of(new String[] { "queueManagerName", cStats.getQueueManager(), "channelName", cStats.getChannelName(),
                "channelType", cStats.getChannelType(), "cluster", cStats.getChannelCluster()}));
    }

    private void checkQueueDepth(QueueStats qStats, Tags tag) {
        String searchQDepthTag = queueDepthTag.concat("-") + qStats.getQueueName();
        AtomicInteger qDepth = (AtomicInteger)this.queueIntegerMap.get(searchQDepthTag);

        if (qDepth == null) {
            System.out.println("when qDepth is null and queueDepth in stats - " + qStats.getQueueDepth());
            this.queueIntegerMap.put(searchQDepthTag, (AtomicInteger)this.meterRegistry.gauge(queueDepthTag,
                  tag, new AtomicInteger(qStats.getQueueDepth())));
        }

        if (!qStats.getQueueType().equals(ApplicationConstants.ALIAS_QUEUE_TYPE)) {
            String searchMaxDepthTag = queueMaxDepthTag.concat("-") + qStats.getQueueName();
            AtomicInteger maxQueueDepth = (AtomicInteger)this.queueIntegerMap.get(searchMaxDepthTag);

            if (maxQueueDepth == null) {
                System.out.println("when max q depth map is null");
                this.queueIntegerMap.put(searchMaxDepthTag, (AtomicInteger)this.meterRegistry.gauge(queueMaxDepthTag,
                      tag, new AtomicInteger(qStats.getMaxQueueDepth())));
            }
        }
    }

    private void checkIOCount(QueueStats qStats, Tags tag) {
        if (!qStats.getQueueType().equals(ApplicationConstants.ALIAS_QUEUE_TYPE)) {
            String searchInputCountTag = queueOpenInCountTag.concat("-") + qStats.getQueueName();
            AtomicInteger queueInCount = (AtomicInteger)this.queueIntegerMap.get(searchInputCountTag);

            if (queueInCount == null) {
                this.queueIntegerMap.put(searchInputCountTag, (AtomicInteger)this.meterRegistry.gauge(queueOpenInCountTag,
                      tag, new AtomicInteger(qStats.getQueueOpenInputCount())));
            }

            String searchOutputCountTag = queueOpenOutCountTag.concat("-") + qStats.getQueueName();
            AtomicInteger queueOutCount = (AtomicInteger)this.queueIntegerMap.get(searchOutputCountTag);

            if (queueOutCount == null) {
                this.queueIntegerMap.put(searchOutputCountTag, (AtomicInteger)this.meterRegistry.gauge(queueOpenOutCountTag,
                      tag, new AtomicInteger(qStats.getQueueOpenOutputCount())));
            }
        }

        if (!CollectionUtils.isEmpty(qStats.getQueueInquireHandler())) {
            checkQueueHandlers(qStats);
        }
    }

    private void checkQueueHandlers(QueueStats qStats) {
        for (QueueInquireHandlerStats qHandlerStats : qStats.getQueueInquireHandler()) {
            String searchInquireTag = queueInquireTag.concat("-") + qHandlerStats.getApplicationTag() + "_" + qHandlerStats.getSequence();
            AtomicInteger qInquireProc = (AtomicInteger)this.queueIntegerMap.get(searchInquireTag);

            Tags tag = getInquireQueueTag(qHandlerStats, qStats.getQueueManager(), qStats.getQueueName(), qStats.getQueueCluster());

            if (qInquireProc == null) {
                this.queueIntegerMap.put(searchInquireTag, (AtomicInteger)this.meterRegistry.gauge(queueInquireTag, tag,
                        new AtomicInteger(qHandlerStats.getQueueState())));
            }
        }
    }

    private void checkLastDateTime(QueueStats qStats, Tags tag) {
        if (qStats.getQueueLastGetDateTime() != 0) {
            String searchGetDateTag = queueLastGetDateTimeTag.concat("-") + qStats.getQueueName();
            AtomicLong queueGetDate = (AtomicLong)this.queueLongMap.get(searchGetDateTag);

            if (queueGetDate == null) {
                this.queueLongMap.put(searchGetDateTag, (AtomicLong)this.meterRegistry.gauge(queueLastGetDateTimeTag, tag,
                        new AtomicLong(qStats.getQueueLastGetDateTime())));
            }
        }

        if (qStats.getQueueLastPutDateTime() != 0) {
            String searchPutDateTag = queueLastPutDateTimeTag.concat("-") + qStats.getQueueName();
            AtomicLong queuePutDate = (AtomicLong)this.queueLongMap.get(searchPutDateTag);

            if (queuePutDate == null) {
                this.queueLongMap.put(searchPutDateTag, (AtomicLong)this.meterRegistry.gauge(queueLastPutDateTimeTag, tag,
                        new AtomicLong(qStats.getQueueLastPutDateTime())));
            }
        }
    }

    private void checkOldestMsgAge(QueueStats qStats, Tags tag) {
        String searchOldMsgAgeTag = queueOldestMsgAgeTag.concat("-") + qStats.getQueueName();
        AtomicInteger queueOldMsgAge = (AtomicInteger)this.queueIntegerMap.get(searchOldMsgAgeTag);

        if (queueOldMsgAge == null) {
            this.queueIntegerMap.put(searchOldMsgAgeTag, (AtomicInteger)this.meterRegistry.gauge(queueOldestMsgAgeTag, tag,
                    new AtomicInteger(qStats.getQueueOldestMsgAge())));
        }
    }

    private void checkDeQueueEnQueueCount(QueueStats qStats, Tags tag) {
        String searchDeQueueCountTag = queueDequeuedCountTag.concat("-") + qStats.getQueueName();
        AtomicInteger queueDeQCount = (AtomicInteger)this.queueIntegerMap.get(searchDeQueueCountTag);

        if (queueDeQCount == null) {
            this.queueIntegerMap.put(searchDeQueueCountTag, (AtomicInteger)this.meterRegistry.gauge(queueDequeuedCountTag, tag,
                    new AtomicInteger(qStats.getDeQueueCount())));
        }

        String searchEnQueueCountTag = queueEnqueuedCountTag.concat("-") + qStats.getQueueName();
        AtomicInteger queueEnQCount = (AtomicInteger)this.queueIntegerMap.get(searchEnQueueCountTag);

        if (queueEnQCount == null) {
            this.queueIntegerMap.put(searchEnQueueCountTag, (AtomicInteger)this.meterRegistry.gauge(queueEnqueuedCountTag, tag,
                    new AtomicInteger(qStats.getEnQueueCount())));
        }
    }

    private void checkChannelStatus(ChannelStats cStats, Tags tag) {
        String searchChannelStatusTag = channelStatusTag.concat("-") + cStats.getChannelName();
        AtomicInteger channelStatus = (AtomicInteger)this.channelIntegerMap.get(searchChannelStatusTag);

        if (channelStatus == null) {
            this.channelIntegerMap.put(searchChannelStatusTag, (AtomicInteger)this.meterRegistry.gauge(channelStatusTag, tag,
                    new AtomicInteger(cStats.getChannelStatus())));
        }
    }

    private void checkChannelMsgCount(ChannelStats cStats, Tags tag) {
        String searchChannelMsgCountTag = channelTotalMsgsTag.concat("-") + cStats.getChannelName();
        AtomicLong channelMsgCount = (AtomicLong)this.channelLongMap.get(searchChannelMsgCountTag);

        if (channelMsgCount == null) {
            this.channelLongMap.put(searchChannelMsgCountTag, (AtomicLong)this.meterRegistry.gauge(channelTotalMsgsTag, tag,
                    new AtomicLong(cStats.getMsgsCountOverChannel())));
        }
    }

    private void checkBytesReceived(ChannelStats cStats, Tags tag) {
        String searchChannelBytesReceivedTag = channelBytesReceivedTag.concat("-") + cStats.getChannelName();
        AtomicLong channelBytesReceived = (AtomicLong)this.channelLongMap.get(searchChannelBytesReceivedTag);

        if (channelBytesReceived == null) {
            this.channelLongMap.put(searchChannelBytesReceivedTag, (AtomicLong)this.meterRegistry.gauge(channelBytesReceivedTag, tag,
                    new AtomicLong(cStats.getBytesReceivedOverChannel())));
        }
    }

    private void checkBytesSent(ChannelStats cStats, Tags tag) {
        String searchChannelBytesSentTag = channelBytesSentTag.concat("-") + cStats.getChannelName();
        AtomicLong channelBytesSent = (AtomicLong)this.channelLongMap.get(searchChannelBytesSentTag);

        if (channelBytesSent == null) {
            this.channelLongMap.put(searchChannelBytesSentTag, (AtomicLong)this.meterRegistry.gauge(channelBytesSentTag, tag,
                    new AtomicLong(cStats.getBytesSentOverChannel())));
        }
    }

    private void checkMaxMsgLength(ChannelStats cStats, Tags tag) {
        String searchChannelMaxMsgLenTag = channelMaxMessageLengthTag.concat("-") + cStats.getChannelName();
        AtomicInteger channelMaxLength = (AtomicInteger)this.channelIntegerMap.get(searchChannelMaxMsgLenTag);

        if (channelMaxLength == null) {
            this.channelIntegerMap.put(searchChannelMaxMsgLenTag, (AtomicInteger)this.meterRegistry.gauge(channelMaxMessageLengthTag, tag,
                    new AtomicInteger(cStats.getMaxMsgLength())));
        }
    }

    private void clear() {
        removeMetric(queueDepthTag);
        removeMetric(queueOpenInCountTag);
        removeMetric(queueOpenOutCountTag);
        removeMetric(queueInquireTag);
        removeMetric(queueMaxDepthTag);
        removeMetric(queueLastGetDateTimeTag);
        removeMetric(queueLastPutDateTimeTag);
        removeMetric(queueOldestMsgAgeTag);
        removeMetric(queueDequeuedCountTag);
        removeMetric(queueEnqueuedCountTag);

        removeMetric(channelStatusTag);
        removeMetric(channelTotalMsgsTag);
        removeMetric(channelBytesReceivedTag);
        removeMetric(channelBytesSentTag);
        removeMetric(channelMaxMessageLengthTag);

        this.queueIntegerMap.clear();
        this.queueLongMap.clear();

        this.channelIntegerMap.clear();
        this.channelLongMap.clear();
    }

    private void removeMetric(String lookup) {
        List<Meter> matchingMeterList = this.meterRegistry.getMeters().stream()
                                            .filter(meter -> lookup.equals(meter.getId().getName()))
                                            .collect(Collectors.toList());

        for (Meter meter : matchingMeterList) {
            this.meterRegistry.remove(meter);
        }
    }
}
