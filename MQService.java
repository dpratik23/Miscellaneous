import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQGetMessageOptions;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MQService {

    public Map<String, String> checkQueueStatus(String host, int port, String channel, String queueManagerName, String[] queueNames) {
        Map<String, String> queueStatusMap = new HashMap<>();

        MQQueueManager queueManager = null;
        try {
            // Set up connection properties
            java.util.Properties properties = new java.util.Properties();
            properties.put(CMQC.HOST_NAME_PROPERTY, host);
            properties.put(CMQC.PORT_PROPERTY, port);
            properties.put(CMQC.CHANNEL_PROPERTY, channel);
            properties.put(CMQC.USER_ID_PROPERTY, "your_user_id"); // Optional, if authentication is required
            properties.put(CMQC.PASSWORD_PROPERTY, "your_password"); // Optional, if authentication is required

            // Connect to the queue manager
            queueManager = new MQQueueManager(queueManagerName, properties);

            for (String queueName : queueNames) {
                try {
                    // Open the queue for inquiry (no read/write access required)
                    int openOptions = MQConstants.MQOO_INQUIRE;
                    MQQueue queue = queueManager.accessQueue(queueName, openOptions);

                    // If the queue is accessible, it is available
                    queueStatusMap.put(queueName, "AVAILABLE");

                    // Close the queue
                    queue.close();
                } catch (MQException e) {
                    // Handle specific MQ exceptions
                    if (e.getReasonCode() == MQConstants.MQRC_UNKNOWN_OBJECT_NAME) {
                        queueStatusMap.put(queueName, "NOT_AVAILABLE");
                    } else if (e.getReasonCode() == MQConstants.MQRC_NOT_AUTHORIZED) {
                        queueStatusMap.put(queueName, "PERMISSION_DENIED");
                    } else {
                        queueStatusMap.put(queueName, "ERROR: " + e.getMessage());
                    }
                }
            }
        } catch (MQException e) {
            // Handle connection errors
            for (String queueName : queueNames) {
                queueStatusMap.put(queueName, "CONNECTION_ERROR: " + e.getMessage());
            }
        } finally {
            // Disconnect from the queue manager
            if (queueManager != null && queueManager.isConnected()) {
                try {
                    queueManager.disconnect();
                } catch (MQException e) {
                    // Log disconnect error if needed
                }
            }
        }

        return queueStatusMap;
    }
}
