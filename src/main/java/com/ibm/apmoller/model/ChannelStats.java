package com.ibm.apmoller.model;

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
public class ChannelStats {

    private String queueManager;

    private String channelName;
    private String channelType;
    private String channelCluster;

    private int channelStatus;

    private long msgsCountOverChannel;

    private long bytesReceivedOverChannel;
    private long bytesSentOverChannel;

    private int maxMsgLength;

}
