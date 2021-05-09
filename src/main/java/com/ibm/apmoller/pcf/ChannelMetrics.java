package com.ibm.apmoller.pcf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ibm.apmoller.model.ChannelStats;
import com.ibm.apmoller.mq.ConnectionPool;
import com.ibm.apmoller.utility.UtilClass;
import com.ibm.mq.MQException;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;

@Component
public class ChannelMetrics {

    @Autowired
    private ConnectionPool mqConnection;

    @Value("#{'${ibm.mq.objects.channels.exclude}'.split(',', -1)}")
    private List<String> excludeChannels;

    @Value("#{'${ibm.mq.objects.channels.include}'.split(',', -1)}")
    private List<String> includeChannels;

    List<ChannelStats> channelStatsList;

    public List<ChannelStats> getAllChannelStats() {
        return this.channelStatsList;
    }

    //@PostConstruct
    public void checkActiveChannels() {
        try {
            this.channelStatsList = new ArrayList<>();

            int[] pcfAttr = { MQConstants.MQIACF_ALL };
            PCFMessage request = new PCFMessage (MQConstants.MQCMD_INQUIRE_CHANNEL);
            request.addParameter (MQConstants.MQCACH_CHANNEL_NAME, "*");
            request.addParameter (MQConstants.MQIACF_CHANNEL_ATTRS, pcfAttr);

            System.out.println("Trying to connect to Channel stats");
            PCFMessage[] responses = mqConnection.getAgent().send(request);

            for(PCFMessage response : responses) {
                String channelName = response.getStringParameterValue(MQConstants.MQCACH_CHANNEL_NAME).trim();
                //System.out.println("Channel Name === " + channelName);
                ChannelStats channelStats = new ChannelStats();

                if (UtilClass.checkIfAllowed(channelName, excludeChannels, includeChannels)) {
                    channelStats = setChannelStats(response, channelName);
                    this.channelStatsList.add(channelStats);
                }
            }
        } catch(MQException | IOException | ArrayIndexOutOfBoundsException ex) {
            System.err.println("checkActiveChannels() ==== " + ex);
        }
    }

    private ChannelStats setChannelStats(PCFMessage response, String channelName) {
        ChannelStats channelStats = new ChannelStats();

        try {
            channelStats.setQueueManager(mqConnection.getQManager());
            channelStats.setChannelName(channelName);

            int channelType = response.getIntParameterValue(MQConstants.MQIACH_CHANNEL_TYPE);
            channelStats.setChannelType(UtilClass.checkChannelType(channelType));

            if ((channelType == MQConstants.MQCHT_CLUSRCVR) || (channelType == MQConstants.MQCHT_CLUSSDR)) {
                channelStats.setChannelCluster(response.getStringParameterValue(MQConstants.MQCA_CLUSTER_NAME).trim());
            } else {
                channelStats.setChannelCluster("");
            }

            channelStats.setMaxMsgLength(response.getIntParameterValue(MQConstants.MQIACH_MAX_MSG_LENGTH));

            PCFMessage[] pcfChannelStatusResList = getChannelStatusSpecifics(channelName);

            channelStats.setChannelStatus(pcfChannelStatusResList[0].getIntParameterValue(MQConstants.MQIACH_CHANNEL_STATUS));

            long totalMsgsOverChannel = 0L;
            long bytesReceived = 0L;
            long bytesSent = 0L;

            for (PCFMessage pcfRes : pcfChannelStatusResList) {
                totalMsgsOverChannel += pcfRes.getIntParameterValue(MQConstants.MQIACH_MSGS);
                bytesReceived += pcfRes.getIntParameterValue(MQConstants.MQIACH_BYTES_RCVD);
                bytesSent += pcfRes.getIntParameterValue(MQConstants.MQIACH_BYTES_SENT);
            }
            channelStats.setMsgsCountOverChannel(totalMsgsOverChannel);
            channelStats.setBytesReceivedOverChannel(bytesReceived);
            channelStats.setBytesSentOverChannel(bytesSent);

            System.out.println("count over channel = " + channelStats.getMsgsCountOverChannel() + ", bytes received = " + channelStats.getBytesReceivedOverChannel()
                    + ", bytes sent = " + channelStats.getBytesSentOverChannel());
            //System.out.println("channel name == " + channelStats.getChannelName() + ", channel type = " + channelStats.getChannelType());
        } catch (PCFException e) {
            System.err.println("setChannelStats() ======= " + e);
            if (e.reasonCode == MQConstants.MQRCCF_CHL_STATUS_NOT_FOUND) {
                channelStats.setChannelStatus(0);
            }
        } catch (Exception e) {
            System.err.println("setChannelStats(), exception ------ " + e);
            channelStats.setChannelStatus(0);
        }
        return channelStats;
    }

    private PCFMessage[] getChannelStatusSpecifics(String channelName) throws Exception {
        PCFMessage[] pcfResponseList = null;

        PCFMessage pcfRequest = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);
        pcfRequest.addParameter(MQConstants.MQCACH_CHANNEL_NAME, channelName);
        pcfRequest.addParameter(MQConstants.MQIACH_CHANNEL_INSTANCE_TYPE, MQConstants.MQOT_CURRENT_CHANNEL);
        int[] attrs = {MQConstants.MQIACF_ALL};
        pcfRequest.addParameter(MQConstants.MQIACH_CHANNEL_INSTANCE_ATTRS, attrs);

        pcfResponseList = mqConnection.getAgent().send(pcfRequest);

        return pcfResponseList;
    }
}
