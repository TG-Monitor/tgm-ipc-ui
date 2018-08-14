package ai.quantumsense.tgmonitor.ipc.ui;

import ai.quantumsense.tgmonitor.ipc.api.HeaderKeys;
import ai.quantumsense.tgmonitor.ipc.api.Queues;
import ai.quantumsense.tgmonitor.ipc.api.serializer.Serializer;
import ai.quantumsense.tgmonitor.ipc.api.serializer.pojo.Request;
import ai.quantumsense.tgmonitor.ipc.api.serializer.pojo.Response;
import ai.quantumsense.tgmonitor.logincodeprompt.LoginCodePrompt;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static ai.quantumsense.tgmonitor.ipc.api.Queues.REQUEST_QUEUE;

class RequestSender {

    private Logger logger = LoggerFactory.getLogger(RequestSender.class);

    private Channel channel;
    private Serializer serializer = new Serializer();
    private String correlationId;
    private final String RESPONSE_QUEUE = "responses_to_ui-" + makeId();
    private final BlockingQueue<Response> responseHolder = new ArrayBlockingQueue<>(1);

    RequestSender(Channel channel) {
        this.channel = channel;
        declareResponseQueue();
    }

    private void declareResponseQueue() {
        logger.debug("Declaring response queue " + RESPONSE_QUEUE);
        try {
            channel.queueDeclare(RESPONSE_QUEUE, false, true, false, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Response sendLogin(Request request, LoginCodePrompt loginCodePrompt) {
        Response response = null;
        try {
            String loginCodeRequestQueue = "login_code_request-" + makeId();
            LoginCodeRequestHandler loginCodeRequestHandler = new LoginCodeRequestHandler(loginCodeRequestQueue, channel, loginCodePrompt);
            sendLoginRequest(request, loginCodeRequestQueue);
            startResponseConsumer();
            response = waitForResponse();
            loginCodeRequestHandler.cleanUp();
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }

    Response send(Request request) {
        Response response = null;
        try {
            sendNormalRequest(request);
            startResponseConsumer();
            response = waitForResponse();
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }

    private void sendNormalRequest(Request request) throws IOException {
        correlationId = makeId();
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .replyTo(RESPONSE_QUEUE)
                .build();
        byte[] body = serializer.serializeRequest(request);
        logger.debug("Sending request " + request + " with correlation ID " + correlationId + " on queue " + REQUEST_QUEUE);
        channel.basicPublish("", REQUEST_QUEUE, props, body);
    }

    private void sendLoginRequest(Request request, String loginCodeRequestQueue) throws IOException {
        correlationId = makeId();
        Map<String, Object> header = new HashMap<>();
        header.put(HeaderKeys.LOGIN_CODE_REQUEST_QUEUE, loginCodeRequestQueue);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .replyTo(RESPONSE_QUEUE)
                .headers(header)
                .build();
        byte[] body = serializer.serializeRequest(request);
        logger.debug("Sending request " + request + " with correlation ID " + correlationId + " on queue " + REQUEST_QUEUE);
        channel.basicPublish("", REQUEST_QUEUE, props, body);
    }

    private void startResponseConsumer() throws IOException {
        String consumerTag = channel.basicConsume(RESPONSE_QUEUE, true, this::handleResponse, this::handleConsumerCancel);
        logger.debug("Starting listening for response on queue " + RESPONSE_QUEUE + " with consumer " + consumerTag);
    }

    private void handleResponse(String consumerTag, Delivery delivery) throws IOException {
        if (isErrorResponse(delivery))
            throw new RuntimeException(getErrorResponseMessage(delivery));
        if (!delivery.getProperties().getCorrelationId().equals(correlationId))
            throw new RuntimeException("Received message with unexpected correlation ID: expected " + correlationId + ", but received " + delivery.getProperties().getCorrelationId());
        Response response = serializer.deserializeResponse(delivery.getBody());
        logger.debug("Received response " + response + " with matching correlation ID " + correlationId);
        responseHolder.offer(response);
        channel.basicCancel(consumerTag);
    }

    private boolean isErrorResponse(Delivery delivery) {
        return delivery.getProperties().getHeaders() != null
                && delivery.getProperties().getHeaders().containsKey(HeaderKeys.ERROR);
    }

    private String getErrorResponseMessage(Delivery delivery) {
        return (String) delivery.getProperties().getHeaders().get(HeaderKeys.ERROR);
    }

    private void handleConsumerCancel(String consumerTag) {
        logger.debug("Cancelling consumer " + consumerTag);
    }

    private Response waitForResponse() throws InterruptedException {
        return responseHolder.take();
    }

    private String makeId() {
        return UUID.randomUUID().toString();
    }
}
