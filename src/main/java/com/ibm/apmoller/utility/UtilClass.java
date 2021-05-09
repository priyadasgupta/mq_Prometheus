package com.ibm.apmoller.utility;

import java.util.List;

import com.ibm.apmoller.constants.ApplicationConstants;
import com.ibm.mq.constants.MQConstants;

public class UtilClass {

    public static String checkQType(int qType) {
        String queueType = ApplicationConstants.LOCAL_QUEUE_TYPE;

        if (qType == MQConstants.MQQT_LOCAL) {
            queueType = ApplicationConstants.LOCAL_QUEUE_TYPE;

        } else if (qType == MQConstants.MQQT_MODEL) {
            queueType = ApplicationConstants.MODEL_QUEUE_TYPE;

        } else if (qType == MQConstants.MQQT_ALIAS) {
            queueType = ApplicationConstants.ALIAS_QUEUE_TYPE;

        } else if (qType == MQConstants.MQQT_REMOTE) {
            queueType = ApplicationConstants.REMOTE_QUEUE_TYPE;

        } else if (qType == MQConstants.MQQT_CLUSTER) {
            queueType = ApplicationConstants.CLUSTER_QUEUE_TYPE;

        } else {
            queueType = ApplicationConstants.LOCAL_QUEUE_TYPE;
        }

        return queueType;
    }

    public static String checkQUsage(int qType, int qUsage) {
        String queueUsage = "";

        if (qType == MQConstants.MQQT_ALIAS) {
            queueUsage = ApplicationConstants.ALIAS_Q_USAGE;
        } else if (qUsage == MQConstants.MQUS_TRANSMISSION) {
            queueUsage = ApplicationConstants.TRANSMISSION_Q_USAGE;
        } else {
            queueUsage = ApplicationConstants.NORMAL_Q_USAGE;
        }

        return queueUsage;
    }

    public static String checkChannelType(int channelType)
    {
        String channelTypeVal = ApplicationConstants.UNKNOWN_CHANNEL_TYPE;

        if (channelType == MQConstants.MQCHT_SVRCONN) { //7
            channelTypeVal = ApplicationConstants.SERVER_CONN_CHANNEL_TYPE;

        } else if(channelType == MQConstants.MQCHT_SENDER) { //1
            channelTypeVal = ApplicationConstants.SENDER_CHANNEL_TYPE;

        } else if (channelType == MQConstants.MQCHT_RECEIVER) { //3
            channelTypeVal = ApplicationConstants.RECEIVER_CHANNEL_TYPE;

        } else if (channelType == MQConstants.MQCHT_CLNTCONN) { //6
            channelTypeVal = ApplicationConstants.CLIENT_CONN_CHANNEL_TYPE;

        } else if (channelType == MQConstants.MQCHT_CLUSRCVR) { //8
            channelTypeVal = ApplicationConstants.CLUSTER_RECEIVER_CHANNEL_TYPE;

        } else if (channelType == MQConstants.MQCHT_CLUSSDR) { //9
            channelTypeVal = ApplicationConstants.CLUSTER_SENDER_CHANNEL_TYPE;

        } else if (channelType == MQConstants.MQCHT_REQUESTER) { //4
            channelTypeVal = ApplicationConstants.REQUESTER_CHANNEL_TYPE;

        //} else if (channelType == 11) {
        //    channelTypeVal = ApplicationConstants.AMQP_CHANNEL_TYPE;

        } else if (channelType == MQConstants.MQCHT_MQTT) { //10
            channelTypeVal = ApplicationConstants.MQTT_CHANNEL_TYPE;

        } else if (channelType == MQConstants.MQCHT_SERVER) { //2
            channelTypeVal = ApplicationConstants.SERVER_CHANNEL_TYPE;

        } else {
            channelTypeVal = ApplicationConstants.UNKNOWN_CHANNEL_TYPE;
        }

        return channelTypeVal;
    }

    public static boolean checkIfAllowed(String name, List<String> excludeList, List<String> includeList) {
        boolean allowed = true;

        for (String excludeEntity : excludeList) {
            if (excludeEntity.trim().equals("*")) {
                break;
            } else if (name.startsWith(excludeEntity.trim())) {
                allowed = false;
            }
        }

        if (allowed) {
            for (String includeEntity : includeList) {
                if (includeEntity.trim().equals("*") ||
                        (name.startsWith(includeEntity.trim()))) {
                    allowed = true;
                } else {
                    allowed = false;
                }
            }
        }

        if (allowed)
            System.out.println("Entity " + name + " is allowed");

        return allowed;
    }
}
