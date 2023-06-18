package org.example;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataCollectionReceiver {
    public static final String JOB_INPUT = "JobChannel";
    public static final String STATION_USAGE_INPUT = "StationUsageChannel";
    public static final String PDF_OUTPUT = "PDFChannel";

    public static Channel channel1;
    public static Channel channel2;

    public static List<String> inputDataList;

    public static void main(String[] args) throws Exception {
        DataCollectionReceiver receiver = new DataCollectionReceiver();
        receiver.initialize();
        inputDataList = new ArrayList<>();

        DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
            String inputData1 = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Received message: " + inputData1);
        };
        channel1.basicConsume(JOB_INPUT, true, deliverCallback1, consumerTag -> {
        });

        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String inputData2 = new String(delivery.getBody(), StandardCharsets.UTF_8);

            int prefixEndIndex = inputData2.indexOf(": ") + 2;

            String restOfString = inputData2.substring(prefixEndIndex);

            if (restOfString.isEmpty()) {
                System.out.println("No customer available!");
            } else {
                System.out.println("Received collected data: " + inputData2);
                inputDataList.add(inputData2);
            }
            if(inputDataList.size()>0){
                String combinedData = String.join("\n", inputDataList);
                channel1.basicPublish("", PDF_OUTPUT, null, combinedData.getBytes(StandardCharsets.UTF_8));
                inputDataList.clear();
            }
        };
        channel2.basicConsume(STATION_USAGE_INPUT, true, deliverCallback2, consumerTag -> {
        });
    }

    public void initialize() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(30003);
        com.rabbitmq.client.Connection connection = factory.newConnection();
        channel1 = connection.createChannel();
        channel2 = connection.createChannel();

        channel1.queueDeclare(JOB_INPUT, false, false, false, null);
        channel2.queueDeclare(STATION_USAGE_INPUT, false, false, false, null);
        channel1.queueDeclare(PDF_OUTPUT, false, false, false, null);
    }
}
