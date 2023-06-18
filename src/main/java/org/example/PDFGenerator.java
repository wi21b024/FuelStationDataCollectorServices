package org.example;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.Background;
import com.itextpdf.layout.properties.TextAlignment;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PDFGenerator {
    public static final String PDF_INPUT = "PDFChannel";
    public static final String JDBC = "jdbc:postgresql://localhost:30001/customerdb";
    public static final String USER = "postgres";
    public static final String PW = "postgres";

    public static Channel channel;

    public static String customerID;
    public static String stationID;
    public static boolean customerInfoFetched = false;

    public static double price = 0.5;
    public static double totalSum = 0.0;

    public static void main(String[] args) throws Exception {
        PDFGenerator generator = new PDFGenerator();
        generator.initialize();

        String outputPath = "invoice"+ LocalDate.now()+".pdf";

        PdfDocument pdfDocument = new PdfDocument(new PdfWriter(outputPath));

        // Create a Document and set the document size
        Document document = new Document(pdfDocument, PageSize.A4);

        document.setMargins(100, 20, 20, 20);

        document.add(new Paragraph("Rechnung").setFontSize(40).setTextAlignment(TextAlignment.CENTER)).setBold();
        document.add(new Paragraph());
        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph());

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String inputData = new String(delivery.getBody(), StandardCharsets.UTF_8);

            System.out.println("Received input data: " + inputData);

            int customerIndexStart = inputData.indexOf("customerID") + 11;
            int customerIndexEnd = inputData.indexOf(" -");
            String newCustomerID = inputData.substring(customerIndexStart, customerIndexEnd);

            int stationIndexStart = inputData.indexOf("stationID") + 10;
            int stationIndexEnd = inputData.indexOf(":");
            String newStationID = inputData.substring(stationIndexStart, stationIndexEnd);

            if (!customerInfoFetched || !newCustomerID.equals(customerID)) {
                customerID = newCustomerID;

                try {
                    Connection connection1 = DriverManager.getConnection(JDBC, USER, PW);
                    Statement statement = connection1.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM customer WHERE id = '" + customerID + "'");

                    StringBuilder dataBuilder = new StringBuilder();

                    while (resultSet.next()) {
                        String firstName = resultSet.getString("first_name");
                        String lastName = resultSet.getString("last_name");
                        dataBuilder.append(firstName);
                        dataBuilder.append(" ");
                        dataBuilder.append(lastName);
                    }

                    // System.out.println(dataBuilder);

                    // Add content to the document

                    document.add(new Paragraph("Kunde: " + dataBuilder.toString()).setFontSize(15));
                    document.add(new Paragraph("ID des Kunden: " + customerID).setFontSize(15));
                    document.add(new Paragraph("Datum: "+ LocalDate.now()).setFontSize(15));
                    document.add(new Paragraph());
                    document.add(new LineSeparator(new SolidLine()));
                    document.add(new Paragraph());
                    customerInfoFetched = true;

                    resultSet.close();
                    statement.close();
                    connection1.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            document.add(new Paragraph());

            Table table = new Table(4);

            table.setWidth(550);

            table.addCell(new Cell().add(new Paragraph("Station "+newStationID)).setBold());
            table.addCell(new Cell().add(new Paragraph("Verbrauch")).setBold().setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph("Preis/kwH")).setBold().setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph("Summe")).setBold().setTextAlignment(TextAlignment.RIGHT));

            String[] values = inputData.substring(stationIndexEnd + 1).split(", ");
            DecimalFormat decimalFormat = new DecimalFormat("#0.00");


            for (String value : values) {
                double numericValue = Double.parseDouble(value);
                double sum = numericValue * price;
                totalSum += sum;

                table.addCell(new Cell().add(new Paragraph()));
                table.addCell(new Cell().add(new Paragraph(decimalFormat.format(numericValue))).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(decimalFormat.format(price) + " €")).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(decimalFormat.format(sum) + " €")).setTextAlignment(TextAlignment.RIGHT));
            }

            document.add(table);
        };

        channel.basicConsume(PDF_INPUT, true, deliverCallback, consumerTag -> {});

        System.out.println("Waiting for input data...");

        Thread.sleep(8000); // Wait for some time to receive input data

        document.add(new Paragraph());
        document.add(new Paragraph("Gesamtsumme: " + totalSum + " €").setFontSize(15).setTextAlignment(TextAlignment.RIGHT));

        document.close();
        System.out.println("PDF generated successfully. Output file: " + outputPath);
        System.exit(0);
    }

    public void initialize() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(30003);
        com.rabbitmq.client.Connection connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(PDF_INPUT, false, false, false, null);
    }
}
