package org.example.test;

import org.example.StationDataCollector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.sql.*;



public class StationDataCollectorTest {



    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @Test
    public void testFetchDataFromStationDatabase() throws SQLException {

        // Test input data
        String jdbcUrl = "jdbc:postgresql://localhost:30011/stationdb";
        String user = "postgres";
        String password = "postgres";
        char customerID = '1';
        String stationID = "1";

        // Mock the necessary dependencies
        MockitoAnnotations.openMocks(this);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(true, true, false);
        Mockito.when(resultSet.getString("kwh")).thenReturn("10.5", "8.2");

        // Mock the DriverManager.getConnection() method
        Mockito.when(DriverManager.getConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(connection);

        // Invoke the method to be tested
        String result = StationDataCollector.fetchDataFromStationDatabase(jdbcUrl, user, password, customerID, stationID);

        // Verify the expected interactions
        Mockito.verify(connection).close();
        Mockito.verify(statement).close();
        Mockito.verify(resultSet).close();

        // Assert the expected result
        String expected = "customerID 1 - stationID 1: 10.8, 10.6,49.7 ";
        Assertions.assertEquals(expected, result);
    }
}
