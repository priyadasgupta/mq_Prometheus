package com.ibm.apmoller.constants;

public abstract interface ApplicationConstants //MQPCFConstants
{
    public static final int BASE = 0;
    public static final int PCF_INIT_VALUE = 0;
    public static final int NOTSET = -1;
    public static final int MULTIINSTANCE = 1;
    public static final int NOT_MULTIINSTANCE = 0;
    public static final int MODE_LOCAL = 0;
    public static final int MODE_CLIENT = 1;
    public static final int EXIT_ERROR = 1;
    public static final int NONE = 0;
    public static final int INFO = 1;
    public static final int DEBUG = 2;
    public static final int WARN = 4;
    public static final int ERROR = 8;
    public static final int TRACE = 16;

    public static final String ALIAS_QUEUE_TYPE = "Alias";
    public static final String LOCAL_QUEUE_TYPE = "Local";
    public static final String REMOTE_QUEUE_TYPE = "Remote";
    public static final String MODEL_QUEUE_TYPE = "Model";
    public static final String CLUSTER_QUEUE_TYPE = "Cluster";

    public static final String NORMAL_Q_USAGE = "Normal";
    public static final String TRANSMISSION_Q_USAGE = "Transmission";
    public static final String ALIAS_Q_USAGE = "Alias";

    public static final String UNKNOWN_CHANNEL_TYPE = "Unknown";
    public static final String SERVER_CONN_CHANNEL_TYPE = "ServerConn";
    public static final String SENDER_CHANNEL_TYPE = "Sender";
    public static final String RECEIVER_CHANNEL_TYPE = "Receiver";
    public static final String CLIENT_CONN_CHANNEL_TYPE = "ClientConn";
    public static final String CLUSTER_RECEIVER_CHANNEL_TYPE = "ClusterReceiver";
    public static final String CLUSTER_SENDER_CHANNEL_TYPE = "ClusterSender";
    public static final String REQUESTER_CHANNEL_TYPE = "Requester";
    public static final String AMQP_CHANNEL_TYPE = "AMQP";
    public static final String MQTT_CHANNEL_TYPE = "MQTT";
    public static final String SERVER_CHANNEL_TYPE = "Server";
}
