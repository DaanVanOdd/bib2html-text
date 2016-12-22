package client.controller;

import client.model.ClientFileModel;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import global.controller.IConnectionPoint;
import global.logging.Log;
import global.logging.LogLevel;
import global.model.DefaultClientRequest;
import global.model.IClientRequest;
import global.model.IResult;
import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * @author daan
 *         created on 11/30/16.
 */
public class Client implements IConnectionPoint, Runnable, Consumer {

    private final String clientID, callbackQueueName;
    private final String CLIENT_REQUEST_QUEUE_NAME = "clientRequestQueue";
    private String outputDirectory, hostIP;

    private Connection connection;
    private Channel channel;
    private final BasicProperties replyProps;

    private ClientFileModel clientFileModel;


    public Client() throws IOException {
        this("localhost");
    }

    public Client(String hostIP) throws IOException {
        this(hostIP, UUID.randomUUID().toString());
    }

    public Client(String hostIP, String clientID) throws IOException {
        connectToHost(hostIP);
        this.clientID = clientID;
        this.callbackQueueName = clientID;
        this.clientFileModel = new ClientFileModel(this.clientID);
        this.replyProps = new BasicProperties
                .Builder()
                .correlationId(clientID)
                .replyTo(callbackQueueName)
                .build();
        this.outputDirectory = "";
        initConnectionPoint();
    }

    @Override
    public void run() {
        try {
            consumeIncomingQueues();
        } catch (IOException e) {
            Log.log("failed to consume incoming queues in client.run()", e);
        }
    }

    @Override
    public void consumeIncomingQueues() throws IOException {
        channel.basicConsume(callbackQueueName, false, this);
    }

    private long timeStart = 0;

    void sendClientRequest() throws IOException {
        //time measuring starts before request creation
        //timeStart = System.currentTimeMillis();
        channel.basicPublish("", CLIENT_REQUEST_QUEUE_NAME, replyProps, SerializationUtils.serialize(this.createClientRequest()));
        //time measuring starts after request creation
        timeStart = System.currentTimeMillis();
        Log.log("Client with ID: " + this.clientID + " sent a ClientRequest. (@" + timeStart + ")", LogLevel.INFO);
    }

    @Override
    public void handleConsumeOk(String s) {

    }

    @Override
    public void handleCancelOk(String s) {

    }

    @Override
    public void handleCancel(String s) throws IOException {

    }

    @Override
    public void handleShutdownSignal(String s, ShutdownSignalException e) {

    }

    @Override
    public void handleRecoverOk(String s) {

    }

    @Override
    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        long timeEnd = System.currentTimeMillis();
        Log.log("Client with ID: " + this.clientID + " received a message on queue: " + this.callbackQueueName + "(@" + timeEnd + ")", LogLevel.LOW);
        Log.log("TIME TAKEN : " + ((timeEnd - timeStart) / 1000.0) + " sec");
        Object deliveredObject = SerializationUtils.deserialize(bytes);
        if (deliveredObject instanceof IResult) {
            //TODO: handle Result
            Log.log("Message is instance of IResult.", LogLevel.INFO);
            handleResult((IResult) deliveredObject);
        } else {
            Log.log("SERVER: " + new String(bytes), LogLevel.SEVERE);
        }
    }

    private void handleResult(IResult result) {
        StringBuilder builder = new StringBuilder();
        result.getFileContents().forEach(part -> builder.append(part));
        outputDirectory = "";
        File outDir = new File(outputDirectory);
        String filename = result.getClientID(); //TODO.
        if (outDir == null || !outDir.exists())
            Log.log("Output-directory doesn't exist!", LogLevel.SEVERE);
        else
            try {
                Files.write(new File(outDir, filename).toPath(), builder.toString().getBytes());
            } catch (IOException e) {
                Log.log("Failed to write output file", e);
            }
    }

    @Override
    public void closeConnection() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    @Override
    public void initConnectionPoint() throws IOException {
        declareQueues();
        run();
    }

    @Override
    public void declareQueues() throws IOException {
        //outgoing queues
        channel.queueDeclare(CLIENT_REQUEST_QUEUE_NAME, false, false, false, null);
        //incoming queues
        channel.queueDeclare(callbackQueueName, false, false, false, null);
    }

    @Override
    public String getHostIP() {
        return hostIP;
    }

    boolean connectToHost(String hostIP) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostIP);
        try {
            this.connection = factory.newConnection();
        } catch (Exception e) {
            Log.log("Invalid Host IP " + hostIP);
            return false;
        }
        try {
            this.channel = connection.createChannel();
        } catch (IOException e) {
            Log.log("Could not create a channel for the connection to host ip " + hostIP);
            return false;
        }
        this.hostIP = hostIP;
        return true;
    }

    @Override
    public String getID() {
        return clientID;
    }

    ClientFileModel getClientFileModel() {
        return clientFileModel;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    private IClientRequest createClientRequest() throws IOException {
        return new DefaultClientRequest(clientID, BibTeXFileSplitter.INSTANCE.createIEntryListFromClientFileModel(clientFileModel));
    }
}
