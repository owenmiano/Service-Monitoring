package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

class ServiceConfig {
    String serviceId;
    String serviceName;
    String host;
    int port;
    String uri;
    String method;
    String expectedTelnetResponse;
    String expectedRequestResponse;
    int monitoringInterval;
    String monitoringIntervalTimeUnit;

    public ServiceConfig(String serviceId, String serviceName, String host, int port, String uri, String method,
                         String expectedTelnetResponse, String expectedRequestResponse, int monitoringInterval, String monitoringIntervalTimeUnit) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.uri = uri;
        this.method = method;
        this.expectedTelnetResponse = expectedTelnetResponse;
        this.expectedRequestResponse = expectedRequestResponse;
        this.monitoringInterval = monitoringInterval;
        this.monitoringIntervalTimeUnit = monitoringIntervalTimeUnit;
    }
}

public class Main {

    public static void main(String[] args) {

        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("services.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
            // Skip the header line
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                if (data.length < 11) {
                    System.err.println("Invalid data format: " + line);
                    continue;
                }
                try{
                ServiceConfig serviceConfig = new ServiceConfig(
                        data[1],           // serviceId
                        data[2],           // serviceName
                        data[3],           // host
                        Integer.parseInt(data[4]),  // port
                        data[5],           // uri
                        data[6],           // method
                        data[7],           // expectedTelnetResponse
                        data[8],           // expectedRequestResponse
                        Integer.parseInt(data[9]),  // monitoringInterval
                        data[10]           // monitoringIntervalTimeUnit
                );
                    new Thread(() -> monitorService(serviceConfig)).start();
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing integer value in data: " + line);
                } catch (IllegalArgumentException e) {
                    System.err.println("Error parsing enum value in data: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void monitorService(ServiceConfig serviceConfig) {
        try {
            while (true) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                // Check Telnet
                boolean currentTelnetStatus = checkTelnet(serviceConfig.host, serviceConfig.port);
                String telnetStatus = currentTelnetStatus ?
                        "Connected to server " :
                        "Unable to connect to server";


                // Check HTTP
                String httpStatus = checkHttp(serviceConfig.host, serviceConfig.port, serviceConfig.uri, serviceConfig.method);

                System.out.println(timestamp + " - "+ serviceConfig.serviceId + " "  + serviceConfig.serviceName + " - Telnet: " + telnetStatus + " - Http: " + httpStatus);


                // Determine the correct TimeUnit for the current interval
                TimeUnit intervalUnit = serviceConfig.monitoringIntervalTimeUnit.equals("Minutes") ? TimeUnit.MINUTES : TimeUnit.SECONDS;

                // Wait for the next monitoring interval
                long intervalMillis = serviceConfig.monitoringInterval * intervalUnit.toMillis(1);
                TimeUnit.MILLISECONDS.sleep(intervalMillis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean checkTelnet(String host, int port) {
//        check server
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String checkHttp(String host, int port, String uri, String method) {
//        check application
        try {
            URL url = new URL("https", host, port, uri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return "Application is UP";
            } else {
                return "Application is DOWN";
            }
        } catch (IOException e) {
            return "Error" + ":" + e.getMessage();
        }
    }
}
//