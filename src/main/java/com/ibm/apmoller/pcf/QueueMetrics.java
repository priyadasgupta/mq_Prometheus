package com.ibm.apmoller.pcf;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ibm.apmoller.constants.ApplicationConstants;
import com.ibm.apmoller.model.QueueInquireHandlerStats;
import com.ibm.apmoller.model.QueueStats;
import com.ibm.apmoller.mq.ConnectionPool;
import com.ibm.apmoller.utility.UtilClass;
import com.ibm.mq.MQException;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;

import io.micrometer.core.instrument.util.StringUtils;


@Component
public class QueueMetrics {

    @Autowired
    private ConnectionPool mqConnection;

    @Value("#{'${ibm.mq.objects.queues.exclude}'.split(',', -1)}")
    private List<String> excludeQueueList;

    @Value("#{'${ibm.mq.objects.queues.include}'.split(',', -1)}")
    private List<String> includeQueueList;

    private List<QueueStats> queueNameStatsList;
    private QueueStats qStats;

    public List<QueueStats> getAllQueueStats() {
        return this.queueNameStatsList;
    }

    //@PostConstruct
    public void checkActiveQueues() {
        try {
            this.queueNameStatsList = new ArrayList<>();

            PCFMessage request = new PCFMessage (MQConstants.MQCMD_INQUIRE_Q);
            request.addParameter(MQConstants.MQCA_Q_NAME, "*");
            request.addParameter(MQConstants.MQIA_Q_TYPE, MQConstants.MQQT_ALL);

            PCFMessage[] responses = mqConnection.getAgent().send(request);

            for(PCFMessage response : responses) {
                String queueName = response.getStringParameterValue(MQConstants.MQCA_Q_NAME).trim();
                //System.out.println("Queue name ===== " + queueName);

                if (UtilClass.checkIfAllowed(queueName, excludeQueueList, includeQueueList)) {
                    this.qStats = new QueueStats();
                    setQueueStats(response, queueName);
                }
            }

        } catch(MQException | IOException | ArrayIndexOutOfBoundsException ex) {
            System.err.println("checkActiveQueues  -------- " + ex);
        }
    }

    private void setQueueStats(PCFMessage response, String queueName) {
        try {
            int qType = response.getIntParameterValue(MQConstants.MQIA_Q_TYPE);

            this.qStats.setQueueManager(mqConnection.getQManager());
            this.qStats.setQueueName(queueName);
            this.qStats.setQueueType(UtilClass.checkQType(qType));

            if (qType == MQConstants.MQQT_ALIAS) {
                this.qStats.setQueueUsage(ApplicationConstants.ALIAS_Q_USAGE);
            } else {
                this.qStats.setQueueUsage(UtilClass.checkQUsage(qType, response.getIntParameterValue(MQConstants.MQIA_USAGE)));
            }

            this.qStats.setQueueCluster(response.getStringParameterValue(MQConstants.MQCA_CLUSTER_NAME).trim());

            if (qType != MQConstants.MQQT_ALIAS) {
                this.qStats.setQueueDepth(response.getIntParameterValue(MQConstants.MQIA_CURRENT_Q_DEPTH));
                this.qStats.setMaxQueueDepth(response.getIntParameterValue(MQConstants.MQIA_MAX_Q_DEPTH));
                this.qStats.setQueueOpenInputCount(response.getIntParameterValue(MQConstants.MQIA_OPEN_INPUT_COUNT));
                this.qStats.setQueueOpenOutputCount(response.getIntParameterValue(MQConstants.MQIA_OPEN_OUTPUT_COUNT));

                if ((this.qStats.getQueueOpenInputCount() > 0) || (this.qStats.getQueueOpenOutputCount() > 0)) {
                    List<QueueInquireHandlerStats> qHandlerStats = checkQueueHandlers(queueName);
                    this.qStats.setQueueInquireHandler(qHandlerStats);
                }
                checkQueueMonitoring(queueName);
            } else {
                this.qStats.setQueueDepth(0);
                this.qStats.setMaxQueueDepth(0);
                this.qStats.setQueueOpenInputCount(0);
                this.qStats.setQueueOpenOutputCount(0);
            }
            this.queueNameStatsList.add(this.qStats);
            System.out.println("Queue = " + queueName + ", depth = " + this.qStats.getQueueDepth()
                        + ", type = " + this.qStats.getQueueType());

        } catch (PCFException ex) {
            System.err.println("setQueueStats() ====== " + ex);
        }
    }

    private PCFMessage getPCFInquireQueueStatus(String qName) {
        PCFMessage pcfInquireQueueStatus = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q_STATUS);
        pcfInquireQueueStatus.addParameter(MQConstants.MQCA_Q_NAME, qName);

        return pcfInquireQueueStatus;
    }

    private List<QueueInquireHandlerStats> checkQueueHandlers(String qName) {
        List<QueueInquireHandlerStats> qInqHandlerStatsList = new ArrayList<>();

        try {
            PCFMessage pcfInquireQueueStatus = getPCFInquireQueueStatus(qName);
            pcfInquireQueueStatus.addParameter(MQConstants.MQIACF_Q_STATUS_TYPE, MQConstants.MQIACF_Q_HANDLE);

            PCFMessage[] pcfResponses = mqConnection.getAgent().send(pcfInquireQueueStatus);
            int seq = 0;
            for(PCFMessage response : pcfResponses) {
                QueueInquireHandlerStats qHandlerStat = new QueueInquireHandlerStats();

                qHandlerStat.setQueueState(response.getIntParameterValue(MQConstants.MQIACF_HANDLE_STATE));
                qHandlerStat.setApplicationTag(response.getStringParameterValue(MQConstants.MQCACF_APPL_TAG).trim());
                qHandlerStat.setUserId(response.getStringParameterValue(MQConstants.MQCACF_USER_IDENTIFIER).trim());
                qHandlerStat.setSequence(seq);
                seq++;

                qInqHandlerStatsList.add(qHandlerStat);
            }
        } catch (MQException | IOException ex) {
            System.err.println("checkQueueHandlers() ======== " + ex);
        }

        return qInqHandlerStatsList;
    }

    private void checkQueueMonitoring(String qName) {
        try {
            PCFMessage pcfInquireQueueStatus = getPCFInquireQueueStatus(qName);
            pcfInquireQueueStatus.addParameter(MQConstants.MQIACF_Q_STATUS_TYPE, MQConstants.MQIACF_Q_STATUS);

            PCFMessage[] pcfResponses = mqConnection.getAgent().send(pcfInquireQueueStatus);
            setQueueStatusStats(pcfResponses[0]);

        } catch(MQException | IOException ex) {
            System.err.println("checkQueueMonitoring ===== " + ex);
        }
    }

    private void setQueueStatusStats(PCFMessage pcfRes) {
        try {
            int queueMonitor = getQueueMonitorVal();

            if ((queueMonitor != 0) && (queueMonitor != -1)) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

                String lastGetDate = pcfRes.getStringParameterValue(MQConstants.MQCACF_LAST_GET_DATE);
                String lastGetTime = pcfRes.getStringParameterValue(MQConstants.MQCACF_LAST_GET_TIME);

                String lastPutDate = pcfRes.getStringParameterValue(MQConstants.MQCACF_LAST_PUT_DATE);
                String lastPutTime = pcfRes.getStringParameterValue(MQConstants.MQCACF_LAST_PUT_TIME);

                if (StringUtils.isNotBlank(lastGetDate) || StringUtils.isNotBlank(lastGetTime))
                {
                    Date dt = formatter.parse(lastGetDate + " " + lastGetTime);
                    this.qStats.setQueueLastGetDateTime(dt.getTime());
                }

                if (StringUtils.isNotBlank(lastPutDate) || StringUtils.isNotBlank(lastPutTime))
                {
                    Date dt = formatter.parse(lastPutDate + " " + lastPutTime);
                    this.qStats.setQueueLastPutDateTime(dt.getTime());
                }

                this.qStats.setQueueOldestMsgAge(pcfRes.getIntParameterValue(MQConstants.MQIACF_OLDEST_MSG_AGE));
            }

            if (! this.qStats.getQueueType().equals(ApplicationConstants.LOCAL_QUEUE_TYPE)) {
                this.qStats.setDeQueueCount(pcfRes.getIntParameterValue(MQConstants.MQIA_MSG_DEQ_COUNT));
                this.qStats.setEnQueueCount(pcfRes.getIntParameterValue(MQConstants.MQIA_MSG_ENQ_COUNT));
            }

        } catch (MQException | IOException | ParseException ex) {
            System.err.println("setQueueStatusStats === " + ex);
        }
    }

    private int getQueueMonitorVal() throws MQException, IOException {
        PCFMessage pcfReq = new PCFMessage(MQConstants.MQTT_EVERY);
        int[] pcfAttr = { MQConstants.MQIACF_ALL };
        pcfReq.addParameter(MQConstants.MQNT_ALL, pcfAttr);

        PCFMessage[] pcfResponse = mqConnection.getAgent().send(pcfReq);

        int queueMonitor = pcfResponse[0].getIntParameterValue(MQConstants.MQIA_MONITORING_Q);
        return queueMonitor;
    }
}
