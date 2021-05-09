package com.ibm.apmoller.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QueueStats {

    private String queueManager;
    private String queueName;
    private String queueType;
    private String queueUsage;
    private String queueCluster;

    private int queueDepth;
    private int maxQueueDepth;

    private int queueOpenInputCount;
    private int queueOpenOutputCount;

    private long queueLastGetDateTime;
    private long queueLastPutDateTime;
    private int queueOldestMsgAge;

    private int deQueueCount;
    private int enQueueCount;

    private List<QueueInquireHandlerStats> queueInquireHandler;
}
