package ai.quantumsense.tgmonitor.ipc.ui;

import ai.quantumsense.tgmonitor.ipc.api.serializer.Serializer;
import ai.quantumsense.tgmonitor.ipc.api.serializer.pojo.Response;
import ai.quantumsense.tgmonitor.ipc.api.serializer.pojo.ValueResponse;
import ai.quantumsense.tgmonitor.logincodeprompt.LoginCodePrompt;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class LoginCodeRequestHandler {

    private Logger logger = LoggerFactory.getLogger(LoginCodeRequestHandler.class);

    private String requestQueue;
    private Channel channel;
    private LoginCodePrompt loginCodePrompt;
    private Serializer serializer = new Serializer();
    private String consumerTag;

    LoginCodeRequestHandler(String requestQueue, Channel channel, LoginCodePrompt loginCodePrompt) {
        this.requestQueue = requestQueue;
        this.channel = channel;
        this.loginCodePrompt = loginCodePrompt;
        try {
            declareQueue();
            startConsumer();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    void cleanUp() throws  IOException {
        if (channel.consumerCount(requestQueue) > 0)
            channel.basicCancel(consumerTag);
    }

    private void declareQueue() throws IOException {
        logger.debug("Declaring login code request queue " + requestQueue);
        channel.queueDeclare(requestQueue, false, true, true, null);
    }

    private void startConsumer() throws IOException {
        consumerTag = channel.basicConsume(requestQueue, true, this::handleRequest, this::handleConsumerCancel);
        logger.debug("Starting to listen for login code request on queue " + requestQueue + " with consumer " + consumerTag);
    }

    private void handleRequest(String consumerTag, Delivery delivery) throws  IOException {
        logger.debug("Receiving login code request on queue " + requestQueue);
        String loginCode = loginCodePrompt.promptLoginCode();
        sendResponse(new ValueResponse(loginCode), delivery.getProperties().getReplyTo());
        channel.basicCancel(consumerTag);
    }

    private void sendResponse(Response response, String queue) throws IOException {
        logger.debug("Sending back response " + response + " on queue " + queue);
        channel.basicPublish("", queue, null, serializer.serializeResponse(response));
    }

    private void handleConsumerCancel(String consumerTag) {
        logger.debug("Cancelling consumer " + consumerTag);
    }
}
