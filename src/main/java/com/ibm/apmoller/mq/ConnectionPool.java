package com.ibm.apmoller.mq;

import java.util.Hashtable;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.pcf.PCFMessageAgent;

@Component
public class ConnectionPool {

    @Value("${ibm.mq.queueManager}")
    private String queueManager;

    @Value("${ibm.mq.connName}")
    private String connName;

    @Value("${ibm.mq.mqHostName}")
    private String mqHostName;

    @Value("${ibm.mq.mqPort}")
    private String mqPort;

    @Value("${ibm.mq.channel}")
    private String channelName;

    @Value("${ibm.mq.user}")
    private String userId;

    @Value("${ibm.mq.password}")
    private String password;

    @Value("${ibm.mq.useSSL:false}")
    private boolean useSSL;

    @Value("${ibm.mq.security.truststore}")
    private String truststore;

    @Value("${ibm.mq.security.truststore-password}")
    private String truststorepass;

    @Value("${ibm.mq.security.keystore}")
    private String keystore;

    @Value("${ibm.mq.security.keystore-password}")
    private String keystorepass;

    @Value("${ibm.mq.multiInstance:false}")
    private boolean multiInstance;

    @Value("${ibm.mq.local:false}")
    private boolean local;

    @Value("${ibm.mq.pcf.browse:false}")
    private boolean pcfBrowse;

    @Value("${info.app.version}")
    private String appversion;

    @Value("${ibm.mq.sslCipherSpec}")
    private String sslCipher;

    private Hashtable<String, Comparable> envVar = new Hashtable<>();

    private MQQueueManager qmgr;

    public MQQueueManager getQueueManager() {
        return this.qmgr;
    }

    public String getQManager() {
        return this.queueManager;
    }

    private void setEnvVar() {
        envVar.put("hostname", this.mqHostName);
        envVar.put("channel", this.channelName);
        envVar.put("port", Integer.valueOf(this.mqPort));
        envVar.put("userID", this.userId);
        if (!StringUtils.isEmpty(this.password)) {
            envVar.put("password", this.password);
        }
        envVar.put("transport", "MQSeries");

        if (useSSL) {
            System.setProperty("javax.net.ssl.trustStore", this.truststore);
            System.setProperty("javax.net.ssl.trustStorePassword", this.truststorepass);
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");
            System.setProperty("javax.net.ssl.keyStore", this.keystore);
            System.setProperty("javax.net.ssl.keyStorePassword", this.keystorepass);
            System.setProperty("javax.net.ssl.keyStoreType", "JKS");
            envVar.put("SSL Cipher Suite", this.sslCipher);
        }
    }

    //@PostConstruct
    public void createMQConnection() {
        setEnvVar();
        System.out.println("Connecting to local queue manager " + queueManager);
        try {
            this.qmgr = new MQQueueManager(this.queueManager, this.envVar);
        } catch (MQException ex) {
            System.err.println("createMQConnection : " + ex);
        }
    }

    public PCFMessageAgent getAgent() {
        PCFMessageAgent agent = null;
        try {
            agent = new PCFMessageAgent(getQueueManager());
        } catch(MQException ex) {
            System.err.println(ex);
        }
        return agent;
    }
}
