package org.example.test;

import org.junit.Assert;
import org.junit.Test;

public class PDFGeneratorTest {

    @Test
    public void testLoop() {
        String[] values = {"1.0", "2.5", "3.7"};
        double price = 2.0;
        double totalSum = 0.0;

        for (String value : values) {
            double numericValue = Double.parseDouble(value);
            double sum = numericValue * price;
            totalSum += sum;
        }

        // Test of the expected TotalSum
        Assert.assertEquals(14.4, totalSum, 0.001);
    }
}
