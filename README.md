# mq_Prometheus
Prints MQ custom metrics in prometheus for MQ Channels and Queues.
Certain channels and queues are configured in application.yaml for which the metrics is printed.

# The following Channel related metrics are printed:
	Channel Status - "mq:channelStatus"
	Total message count over channel - "mq:msgsCountOverChannel"
	Total bytes received over channel - "mq:bytesReceivedOverChannel"
	Total bytes sent over channel - "mq:bytesSentOverChannel"
	Max message length - "mq:maxMessageLengthOverChannel"

# The following Queue related metrics are printed:
	Current queue depth - "mq:queueDepth"
	Max queue depth - "mq:maxQueueDepth"
	Open input count - "mq:queueOpenInputCount"
	Open output count - "mq:queueOpenOutputCount"
	Inquire processes - "mq:queueInquireProcesses"
	Last get date time - "mq:lastGetDateTime"
	Last put date time - "mq:lastPutDateTime"
	Oldest message age - "mq:oldestMsgAge"
	Dequeue count - "mq:deQueuedCount"
	Enqueue count - "mq:enQueuedCount"

# Deployment Steps in OpenShift
1) Create a docker file to copy the clientKey to the Openshift cluster. Also the docker file would help in deployment from github to OC.
2) In OC Cluster, create a new project which acts as the namespace.
3) Change the role to Developer if it shows Administrator on the left hand side of the window.
4) Since we have created a docker file to deploy the application, in "Add" menu, select "From Dockerfile"
5) Enter the git url, Dockerfile path, container port and other required details. Select the check box under "Advanced Options" to create a route to the application. This ensures we do not have to manually create the route from services.
6) Once successfully deployed, change the role from "Developer" to "Administrator". Under "Networking", select "Services".
7) Check the status of Pods on selecting the services. It should be in "running" state. If not, then check the Logs and Terminal in Pods.
8) In the YAML section of Services, check for "targetPort". It should be the one on which your application runs. "port" value is the port number of OC host url.
9) Under "Networking" there will be another option "Routes". Select Routes and there should be an entry linked to the Services name, since the checkbox was selected in step 5.
If there are no routes entry, then create a route and link it to the proper Services and target port.
10) In the details section of Routes, the location gives us the host from which we can access the application. Append the required url to the location to hit the rest controller.
In this case it will be: "location url"/actuator/prometheus
