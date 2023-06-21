package org.example.test;

import org.example.StationDataCollector;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class StationDataCollectorTest{



    @Test
    public void testFetchDataFromStationDatabase() {
        String jdbcUrl = "jdbc:postgresql://localhost:30011/stationdb";
        String user = "postgres";
        String password = "postgres";
        char customerID = '1';
        String stationID = "1";

        String result = StationDataCollector.fetchDataFromStationDatabase(jdbcUrl, user, password, customerID, stationID);

        assertNotNull(result);
        assertEquals("customerID 1 - stationID 1: 10.8, 10.6, 49.7, ", result);
    }

    @Test
    public void testFetchDataFromStationDatabase_InvalidStation() {
        String jdbcUrl = "jdbc:postgresql://localhost:30014/stationdb";
        String user = "postgres";
        String password = "postgres";
        char customerID = '5';
        String stationID = "3";

        String result = StationDataCollector.fetchDataFromStationDatabase(jdbcUrl, user, password, customerID, stationID);

        assertNull(result);
    }}
