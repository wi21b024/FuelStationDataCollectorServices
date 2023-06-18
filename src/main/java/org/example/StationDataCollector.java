package org.example;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.sql.Connection;

public class StationDataCollector {

    public static final String CHARGING_STATION_INPUT = "ChargingStationChannel";
    public static final String STATION_USAGE_OUTPUT = "StationUsageChannel";

    public static final String JDBC1 = "jdbc:postgresql://localhost:30011/stationdb";
    public static final String JDBC2 = "jdbc:postgresql://localhost:30012/stationdb";
    public static final String JDBC3 = "jdbc:postgresql://localhost:30013/stationdb";
    public static final String USER = "postgres";
    public static final String PW = "postgres";

    public static Channel channel;

    public static void main(String[] args) throws Exception {
        StationDataCollector collector = new StationDataCollector();
        collector.initialize();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String inputData = new String(delivery.getBody(), StandardCharsets.UTF_8);

            System.out.println("Received input data: " + inputData);

            int startIndex = inputData.indexOf("station: ") + "station: ".length();
            int endIndex = inputData.indexOf(",", startIndex);
            String stationID = inputData.substring(startIndex, endIndex).trim();

            char customerID = inputData.charAt(inputData.length() - 1);

            switch (stationID) {
                case "1":
                    String collectedData1 = fetchDataFromStationDatabase(JDBC1, USER, PW, customerID, stationID);
                    sendOutputData(collectedData1);
                    break;
                case "2":
                    String collectedData2 = fetchDataFromStationDatabase(JDBC2, USER, PW, customerID, stationID);
                    sendOutputData(collectedData2);
                    break;
                case "3":
                    String collectedData3 = fetchDataFromStationDatabase(JDBC3, USER, PW, customerID, stationID);
                    sendOutputData(collectedData3);
                    break;
            }
        };
        channel.basicConsume(CHARGING_STATION_INPUT, true, deliverCallback, consumerTag -> {
        });
    }

    private void initialize() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(30003);
        com.rabbitmq.client.Connection connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(CHARGING_STATION_INPUT, false, false, false, null);
        channel.queueDeclare(STATION_USAGE_OUTPUT, false, false, false, null);
    }

    private static String fetchDataFromStationDatabase(String jdbcUrl, String user, String password, char customerID, String stationID) {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM charge WHERE customer_id = '" + customerID + "'");

            StringBuilder dataBuilder = new StringBuilder();

            while (resultSet.next()) {
                String data = resultSet.getString("kwh");
                dataBuilder.append(data);
                dataBuilder.append(", ");
            }

            dataBuilder.insert(0, "customerID " + customerID + " - stationID " + stationID + ": ");

            System.out.println(dataBuilder);

            resultSet.close();
            statement.close();
            connection.close();

            return dataBuilder.toString();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void sendOutputData(String outputData) {
        try {
            channel.basicPublish("", STATION_USAGE_OUTPUT, null, outputData.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
