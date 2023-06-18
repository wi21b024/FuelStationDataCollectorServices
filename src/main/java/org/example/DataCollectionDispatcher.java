package org.example;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class DataCollectionDispatcher {
    public static final String CUSTOMER_ID_INPUT = "CustomerIDChannel";
    public static final String CHARGING_STATION_OUTPUT = "ChargingStationChannel";
    public static final String JOB_OUTPUT = "JobChannel";

    public static final String JDBC = "jdbc:postgresql://localhost:30002/stationdb";
    public static final String USER = "postgres";
    public static final String PW = "postgres";

    public static Channel channel;

    public static void main(String[] args) throws Exception {
        DataCollectionDispatcher dispatcher = new DataCollectionDispatcher();
        dispatcher.initialize();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String customerID = new String(delivery.getBody(), StandardCharsets.UTF_8);

            try {
                Connection connection1 = DriverManager.getConnection(JDBC, USER, PW);
                Statement statement = connection1.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM station");

                while (resultSet.next()) {
                    int stationId = resultSet.getInt("id");

                    //message to StationDataCollector
                    String messageToCollector = "Job started for station: " + stationId + ", customerID: " + customerID;
                    channel.basicPublish("", CHARGING_STATION_OUTPUT, null, messageToCollector.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Sent message to station data collector: " + messageToCollector);
                }

                //message to DataCollectionReceiver
                String messageToReceiver = "New data collection job started for customerID: " + customerID;
                channel.basicPublish("", JOB_OUTPUT, null, messageToReceiver.getBytes(StandardCharsets.UTF_8));
                System.out.println("Sent message to data collection receiver: " + messageToReceiver);

                resultSet.close();
                statement.close();
                connection1.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        channel.basicConsume(CUSTOMER_ID_INPUT, true, deliverCallback, consumerTag -> {
        });
    }

    public void initialize() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(30003);
        com.rabbitmq.client.Connection connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(CUSTOMER_ID_INPUT, false, false, false, null);
        channel.queueDeclare(CHARGING_STATION_OUTPUT, false, false, false, null);
        channel.queueDeclare(JOB_OUTPUT, false, false, false, null);
    }
}