package org.example;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.Reader;
import java.io.*;
import java.util.zip.*;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.util.Calendar;
import org.ini4j.Ini;
import java.util.ArrayList;
import java.util.Map;
import java.text.ParseException;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.util.HashMap;
import java.nio.file.StandardOpenOption;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import org.yaml.snakeyaml.Yaml;
import java.util.LinkedHashMap;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import org.yaml.snakeyaml.DumperOptions;

class ServiceConfig {
    String serviceId;
    String serviceName;
    String host;
    int port;
    String uri;
    String method;
    String expectedTelnetResponse;
    String expectedRequestResponse;
    long monitoringInterval;
    String monitoringIntervalTimeUnit;
    String enableFileLogging;
    String fileLoggingInterval;
    String lastLogTime;
    String enableLogsArchiving;
    String logArchivingInterval;
    String lastArchivingTime;


    public ServiceConfig(String serviceId, String serviceName, String host, int port, String uri, String method,
                         String expectedTelnetResponse, String expectedRequestResponse, long monitoringInterval,
                         String monitoringIntervalTimeUnit,String enableFileLogging,String fileLoggingInterval,String lastLogTime,String enableLogsArchiving,String logArchivingInterval,String lastArchivingTime) {
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
        this.enableFileLogging = enableFileLogging;
        this.fileLoggingInterval = fileLoggingInterval;
        this.lastLogTime= lastLogTime;
        this.enableLogsArchiving = enableLogsArchiving;
        this.logArchivingInterval = logArchivingInterval;
        this.lastArchivingTime=lastArchivingTime;
    }

    public String getServiceId() {
        return serviceId;
    }

    // Optionally, you may also include other getters (and setters if needed) for the remaining fields.
    // For example:
    public String getServiceName() {
        return serviceName;
    }
    public String getServiceHost() { return host; }

    public int getServicePort() { return port; }
    public String getServiceResourceURI() { return uri; }
    public String getServiceMethod() { return method; }
    public String getExpectedTelnetResponse() { return expectedTelnetResponse; }
    public String getExpectedRequestResponse() { return expectedRequestResponse; }
    public long getMonitoringInterval() { return monitoringInterval; }

    public String getMonitoringIntervalTimeUnit() { return monitoringIntervalTimeUnit; }

    public String getFileLoggingStatus() { return enableFileLogging; }

    public String getFileLoggingInterval() { return fileLoggingInterval; }

    public String getLastLogTime() { return lastLogTime; }

    public String getLogArchivingStatus() { return enableLogsArchiving; }

    public String getlogArchivingInterval() { return logArchivingInterval; }

    public String getLastArchivingTime() { return lastArchivingTime; }


//    setter methods
public void setLastLogTime(String lastLogTime) {
    this.lastLogTime = lastLogTime;
}

    public void setLastArchivingTime(String lastArchivingTime) {
        this.lastArchivingTime = lastArchivingTime;
    }
}

public class Main {
    private static boolean isApplicationRunning = false;
    private static List<ServiceConfig> serviceConfigs;
    private static Map<String, Thread> monitoringThreads = new HashMap<>();
    private static Map<String, Thread> archivingThreads = new HashMap<>();

    // Define an array to store last log times for each service and log type
    private static final String LOG_FILE_NAME_PATTERN = "yyyy-MM-dd_HH-mm-ss";
    private static final SimpleDateFormat logFileNameDateFormat = new SimpleDateFormat(LOG_FILE_NAME_PATTERN);
    private static final SimpleDateFormat zipFileNameDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");


    public static void main(String[] args) {
        File configFile = getConfigFile();
        String fileExtension = getFileExtension(configFile);

        switch (fileExtension) {
            case "xml":
                serviceConfigs = readXmlConfiguration(configFile);
                break;
            case "json":
                serviceConfigs = readJsonConfiguration(configFile);
                break;
            case "yaml":
                serviceConfigs = readYamlConfiguration(configFile);
                break;
            case "ini":
                serviceConfigs = readIniConfiguration(configFile);
                break;
            case "csv":
                serviceConfigs = readCsvConfiguration(configFile);
                break;
            default:
                System.out.println("Unsupported file type.");
                return;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println(serviceConfigs);
        System.out.println("Enter 'sky-monitor start' command  to start the sky-monitor application ");
        while (true) {
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "sky-monitor start":
                    if (!isApplicationRunning) {
                        startApplication();
                    } else {
                        System.out.println("The application is already running.");
                    }
                    break;
                case "sky-monitor stop":
                    if (isApplicationRunning) {
                        stopApplication();
                    } else {
                        System.out.println("The application is not running.");
                    }
                    break;
                case "sky-monitor exit":
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid command. Please enter a valid one.");
            }
        }
    }
    private static File getConfigFile() {
        Path resourcesPath = Paths.get("src", "main", "resources");
        try {
            if (!Files.exists(resourcesPath)) {
                Files.createDirectories(resourcesPath);
            }
        } catch (IOException e) {
            System.err.println("Error creating resources directory: " + e.getMessage());
            return null;
        }

        File resourcesDirectory = resourcesPath.toFile();
        FilenameFilter filter = (dir, name) -> name.startsWith("config");
        File[] files = resourcesDirectory.listFiles(filter);

        if (files == null || files.length == 0) {
            System.out.println("No 'config' files found in the resources directory");
            return null;
        }
        if (files.length > 1) {
            System.out.println("More than one 'config' file found. Please ensure only one is present.");
            return null;
        }

        return files[0];
    }

    private static void startApplication() {


    isApplicationRunning = true;
    Scanner scanner = new Scanner(System.in);

        for (ServiceConfig service : serviceConfigs) {
            String serviceId = service.getServiceId();

            // Start a monitoring thread for each service
            Thread monitoringThread = new Thread(() -> monitorService(service));
            monitoringThread.start();
            monitoringThreads.put(serviceId, monitoringThread);

            // Start an archiving thread for each service
            Thread archivingThread = new Thread(() -> archiveService(service));
            archivingThread.start();
            archivingThreads.put(serviceId, archivingThread);
        }



    // Handle user commands (you can implement this function)
    handleUserCommands();
}


    private static void handleUserCommands() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter one of the following commands:");
        System.out.println("'sky-monitor server status [Service ID]' to check the status of the server hosting the service.");
        System.out.println("'sky-monitor application status [Service ID]' to check the status of the service's application.");
        System.out.println("'sky-monitor service list' to list all the services.");
        while (isApplicationRunning) {
            String command = scanner.nextLine().trim().toLowerCase();
            processCommand(command, scanner);
        }
        scanner.close();
    }

    private static void processCommand(String command, Scanner scanner) {
        String[] commandParts = command.split(" ");

        if ("sky-monitor".equals(commandParts[0])) {
            if (commandParts.length < 2) {
                System.out.println("Invalid command format.");
                return;
            }

            switch (commandParts[1]) {
                case "application":
                    if (commandParts.length > 3 && "status".equalsIgnoreCase(commandParts[2])) {
                        String serviceId = commandParts[3].trim();
                        ServiceConfig service = findServiceById(serviceId);
                        if (service != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                            while (true) {
                                String currentDateTime = dateFormat.format(new Date());

                                // Convert monitoring interval to milliseconds
                                long intervalMillis = TimeUnit.valueOf(service.getMonitoringIntervalTimeUnit().toUpperCase()).toMillis(service.getMonitoringInterval());

                                String appStatus = checkApplicationStatus(service);
                                System.out.println(currentDateTime + " - " + serviceId + " - " + service.getServiceName() + " - Application Status: " + appStatus);

                                try {
                                    // Wait for the next interval
                                    Thread.sleep(intervalMillis);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    System.out.println("Monitoring interrupted for service " + serviceId);
                                    break;
                                }
                            }
                        } else {
                            System.out.println("Service with ID " + serviceId + " not found.");
                        }
                    } else {
                        System.out.println("Invalid command format. Usage: sky-monitor application status <serviceId>");
                    }
                    break;

                case "server":
                    if (commandParts.length > 3 && "status".equalsIgnoreCase(commandParts[2])) {
                        String serviceId = commandParts[3].trim();
                        ServiceConfig service = findServiceById(serviceId);
                        if (service != null) {
                            // Start monitoring thread for the server
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                            while (true) {
                                String currentDateTime = dateFormat.format(new Date());

                                // Convert monitoring interval to milliseconds
                                long intervalMillis = TimeUnit.valueOf(service.getMonitoringIntervalTimeUnit().toUpperCase()).toMillis(service.getMonitoringInterval());

                                String serverStatus = checkServerStatus(service);
                                System.out.println(currentDateTime + " - " + serviceId + " - " + service.getServiceName() + " - Server Status: " + serverStatus);

                                try {
                                    // Wait for the next interval
                                    Thread.sleep(intervalMillis);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    System.out.println("Monitoring interrupted for server " + serviceId);
                                    break;
                                }
                            }
                        } else {
                            System.out.println("Server with ID " + serviceId + " not found.");
                        }
                    } else {
                        System.out.println("Invalid command format. Usage: sky-monitor server status <serverId>");
                    }
                    break;


                case "service":
                    if (commandParts.length == 3 && "list".equals(commandParts[2])) {
                        listServices();
                    }
                    break;

                case "exit":
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid command.");
            }
        }
    }


    //    stop the application
    private static void stopApplication() {
        System.out.println("Stopping the sky-monitor application...");
        isApplicationRunning = false;

        // Stop all monitoring threads
        for (String serviceId : monitoringThreads.keySet()) {
            Thread thread = monitoringThreads.get(serviceId);
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
        monitoringThreads.clear();
    }
    private static void monitorService(ServiceConfig service) {
        // Define intervals for checking status

        long intervalMillis = TimeUnit.valueOf(service.getMonitoringIntervalTimeUnit().toUpperCase()).toMillis(service.getMonitoringInterval());


        while (isApplicationRunning) {

            try {
                // Perform server status check
                String serverStatus = checkServerStatus(service);
                String serverLogEntry = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " - Server Status: " + serverStatus;

                // Perform application status check
                String appStatus = checkApplicationStatus(service);
                String appLogEntry = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " - Application Status: " + appStatus;

                // Log server and application status if file logging is enabled
                if ("yes".equalsIgnoreCase(service.getFileLoggingStatus())) {
                    // Write log for server status
                    writeLog(service.getServiceId(), service.getServiceName(), service.getFileLoggingInterval(),
                            service.getLastLogTime(), "server", serverLogEntry, service);

                    // Write log for application status
                    writeLog(service.getServiceId(), service.getServiceName(), service.getFileLoggingInterval(),
                            service.getLastLogTime(), "application", appLogEntry, service);
                }

                // Handle archiving if enabled
//                   archiveServiceStatus(service);

                // Wait for the next interval
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Monitoring interrupted for service " + service.getServiceId());
                break;
            }
        }
    }
    private static Date getCurrentTimeStamp() {
        return new Date();
    }

    private static void archiveService(ServiceConfig service) {
        // Check if log archiving is enabled
        if ("yes".equalsIgnoreCase(service.getLogArchivingStatus())) {
            // Define intervals for archiving using the calculateArchivingIntervalMillis method
            long archivingIntervalMillis = calculateArchivingIntervalMillis(service.getlogArchivingInterval());

            while (isApplicationRunning) {
                try {
                    // Archiving operations
                    performArchiving(service.getServiceId(), service.getServiceName(), service.getLastArchivingTime(),
                            service.getlogArchivingInterval(), service);

                    // Wait for the next archiving interval
                    Thread.sleep(archivingIntervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Archiving interrupted for service " + service.getServiceId());
                    break;
                }
            }
        }
    }

    private static void performArchiving(String serviceId, String serviceName, String lastArchivingTime, String fileArchivingInterval, ServiceConfig service) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Path logsDirectoryPath = Paths.get("src", "main", "resources", "logs", serviceName);
            Files.createDirectories(logsDirectoryPath);

            Date timeStamp;
            if (lastArchivingTime == null || lastArchivingTime.trim().isEmpty()) {
                timeStamp = new Date(); // Current time
                String formattedTimeStamp = sdf.format(timeStamp);
                updateConfig(serviceId, formattedTimeStamp, "lastArchivingTime");
                service.setLastArchivingTime(formattedTimeStamp);
            } else {
                timeStamp = sdf.parse(lastArchivingTime);
            }

            long archivingIntervalMillis = calculateArchivingIntervalMillis(fileArchivingInterval);

                if (Files.isDirectory(logsDirectoryPath)) {
                    List<Path> filesToArchive = new ArrayList<>();
                    Date currentDate = new Date();

                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDirectoryPath)) {
                        for (Path file : stream) {
                            String fileName = file.getFileName().toString();
                            if (fileName.endsWith(".log")) {
                                Date logFileDate = extractDateFromLogFileName(fileName); // Implement this method
                                if (logFileDate != null) {
                                    long elapsedTimeMillis = currentDate.getTime() - logFileDate.getTime();
                                    if (elapsedTimeMillis > archivingIntervalMillis) {
                                        filesToArchive.add(file);
                                    }
                                }
                            }
                        }
                    }

                    if (!filesToArchive.isEmpty()) {
                        String zipFileName = zipFileNameDateFormat.format(currentDate) + "_archive.zip"; // Format zip file name
                        Path zipFilePath = logsDirectoryPath.resolve(zipFileName);
                        archiveLogFiles(zipFilePath, filesToArchive); // Implement this method

                        // Update lastArchivingTime after successful archiving
                        String formattedTimeStamp = sdf.format(new Date());
                        updateConfig(serviceId, formattedTimeStamp, "lastArchivingTime");
                        service.setLastArchivingTime(formattedTimeStamp);
                    }
                }

        } catch (Exception e) {
            System.err.println("Error occurred during archiving for service: " + service.getServiceId());
            e.printStackTrace();
        }
    }


    private static long calculateArchivingIntervalMillis(String archivingInterval) {
        switch (archivingInterval.toLowerCase()) {
            case "hourly":
                return 60 * 60 * 1000; // 1 hour
            case "daily":
                return 24 * 60 * 60 * 1000; // 24 hours
            case "weekly":
                return 7 * 24 * 60 * 60 * 1000; // 7 days
            case "monthly":
                return 30 * 24 * 60 * 60 * 1000; // 30 days (approximate)
            default:
                System.out.println("Unsupported log archiving interval: " + archivingInterval + ". Using default: weekly");
                return 7 * 24 * 60 * 60 * 1000; // Default to weekly (7 days) if unsupported interval is specified
        }
    }

    private static Date extractDateFromLogFileName(String fileName) {
        try {
            int firstUnderscoreIndex = fileName.indexOf('_');
            int secondUnderscoreIndex = fileName.indexOf('_', firstUnderscoreIndex + 1);
            if (secondUnderscoreIndex != -1) {
                String datetimePart = fileName.substring(0, secondUnderscoreIndex);
                return logFileNameDateFormat.parse(datetimePart);
            }
        } catch (ParseException e) {
            System.err.println("Error parsing date from file name: " + fileName + " - " + e.getMessage());
        }
        return null;
    }

    private static void archiveLogFiles(Path zipFilePath, List<Path> filesToArchive) throws IOException {
        // Ensure the directory exists
        Files.createDirectories(zipFilePath.getParent());

        // Zip the files
        try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (Path file : filesToArchive) {
                zos.putNextEntry(new ZipEntry(file.getFileName().toString()));
                Files.copy(file, zos);
                zos.closeEntry();
                Files.delete(file); // Optionally delete the file after adding to zip
            }
        }
    }


    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1).toLowerCase();
        }
        return "";
    }
    //Json configuration method
    private static List<ServiceConfig> readJsonConfiguration(File configFile) {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(configFile)) {
            Type listType = new TypeToken<List<ServiceConfig>>(){}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    //YAML configuration method
    private static List<ServiceConfig> readYamlConfiguration(File configFile) {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            List<LinkedHashMap<String, Object>> data = yaml.loadAs(inputStream, List.class);

            if (data != null) {
                for (LinkedHashMap<String, Object> item : data) {
                    ServiceConfig config = mapToServiceConfig(item);
                    serviceConfigs.add(config);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serviceConfigs;
    }

    private static ServiceConfig mapToServiceConfig(LinkedHashMap<String, Object> map) {
        // Initialize the ServiceConfig object with values from the map
        ServiceConfig serviceConfig = new ServiceConfig(
                getString(map, "serviceId"),
                getString(map, "serviceName"),
                getString(map, "host"),
                getInt(map, "port"),
                getString(map, "uri"),
                getString(map, "method"),
                getString(map, "expectedTelnetResponse"),
                getString(map, "expectedRequestResponse"),
                getLong(map, "monitoringInterval"),
                getString(map, "monitoringIntervalTimeUnit"),
                getString(map, "enableFileLogging"),
                getString(map, "fileLoggingInterval"),
                getString(map, "lastLogTime"),
                getString(map, "enableLogsArchiving"),
                getString(map, "logArchivingInterval"),
                getString(map, "lastArchivingTime")
        );

        return serviceConfig;
    }

    // Helper method to get a String value from the map
    private static String getString(LinkedHashMap<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }


    // Helper method to get an int value from the map
    private static int getInt(LinkedHashMap<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            throw new IllegalArgumentException("Value for key '" + key + "' is not a Number.");
        }
    }

    // Helper method to get a long value from the map
    private static long getLong(LinkedHashMap<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            throw new IllegalArgumentException("Value for key '" + key + "' is not a Number.");
        }
    }



    //ini configuration method
    private static List<ServiceConfig> readIniConfiguration(File configFile) {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        try {
            Ini ini = new Ini(new FileReader(configFile));
            for (String sectionName : ini.keySet()) {
                Ini.Section section = ini.get(sectionName);
                ServiceConfig config = new ServiceConfig(
                        section.get("serviceId"),
                        section.get("serviceName"),
                        section.get("host"),
                        Integer.parseInt(section.get("port")),
                        section.get("uri"),
                        section.get("method"),
                        section.get("expectedTelnetResponse"),
                        section.get("expectedRequestResponse"),
                        Integer.parseInt(section.get("monitoringInterval")),
                        section.get("monitoringIntervalTimeUnit"),
                        section.get("enableFileLogging"),
                        section.get("fileLoggingInterval"),
                        section.get("lastLogTime"),
                        section.get("enableLogsArchiving"),
                        section.get("logArchivingInterval"),
                        section.get("lastArchivingTime")

                );
                serviceConfigs.add(config);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serviceConfigs;
    }
    //Xml configuration method
    private static List<ServiceConfig> readXmlConfiguration(File configFile) {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(configFile);
            document.getDocumentElement().normalize();

            NodeList nodeList = document.getElementsByTagName("serviceConfig");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    ServiceConfig serviceConfig = new ServiceConfig(
                            element.getElementsByTagName("serviceId").item(0).getTextContent(),
                            element.getElementsByTagName("serviceName").item(0).getTextContent(),
                            element.getElementsByTagName("host").item(0).getTextContent(),
                            Integer.parseInt(element.getElementsByTagName("port").item(0).getTextContent()),
                            element.getElementsByTagName("uri").item(0).getTextContent(),
                            element.getElementsByTagName("method").item(0).getTextContent(),
                            element.getElementsByTagName("expectedTelnetResponse").item(0).getTextContent(),
                            element.getElementsByTagName("expectedRequestResponse").item(0).getTextContent(),
                            Long.parseLong(element.getElementsByTagName("monitoringInterval").item(0).getTextContent()),
                            element.getElementsByTagName("monitoringIntervalTimeUnit").item(0).getTextContent(),
                            element.getElementsByTagName("enableFileLogging").item(0).getTextContent(),
                            element.getElementsByTagName("fileLoggingInterval").item(0).getTextContent(),
                            safeGetTextContent(element, "lastLogTime"),
                            element.getElementsByTagName("enableLogsArchiving").item(0).getTextContent(),
                            element.getElementsByTagName("logArchivingInterval").item(0).getTextContent(),
                            safeGetTextContent(element, "lastArchivingTime")
                    );
                    serviceConfigs.add(serviceConfig);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serviceConfigs;
    }
    private static String safeGetTextContent(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0) != null) {
            return nodes.item(0).getTextContent();
        } else {
            return null; // or return a default value if applicable
        }
    }
    //Csv configuration method
    private static List<ServiceConfig> readCsvConfiguration(File configFile) {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            // Skip the header line
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                // Trim the line and check if it's empty
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                String[] data = line.split(",", -1); // -1 to include trailing empty strings

                if (data.length != 17) {
                    System.err.println("Invalid data format: " + line);
                    continue;
                }

                ServiceConfig serviceConfig = new ServiceConfig(
                        data[1].trim(),  // ID
                        data[2].trim(),  // Service Name
                        data[3].trim(),  // Service Host
                        data[4].trim().isEmpty() ? 0 : Integer.parseInt(data[4].trim()),  // Service Port
                        data[5].trim(),  // Service Resource URI
                        data[6].trim(),  // Service Method
                        data[7].trim(),  // Expected Telnet Response
                        data[8].trim(),  // Expected Request Response
                        data[9].trim().isEmpty() ? 0 : Integer.parseInt(data[9].trim()),  // Monitoring Intervals
                        data[10].trim(), // Monitoring Intervals Time Unit
                        data[11].trim(), // Enable File Logging
                        data[12].trim(), // File Logging Intervals
                        data[13].trim(), // Last Log Time
                        data[14].trim(), // Enable logs Archiving
                        data[15].trim(), // Log Archiving Interval
                        data[16].trim()  // Last Archiving Time
                );

                serviceConfigs.add(serviceConfig);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return serviceConfigs;
    }


    private static ServiceConfig findServiceById(String serviceId) {
        if (serviceId == null) {
            // Log that no service ID was provided
            System.out.println("No service ID provided to findServiceById method.");
            return null; // Return null or handle as required
        }

        for (ServiceConfig config : serviceConfigs) {
            if (config.getServiceId() != null && config.getServiceId().equals(serviceId)) {
                return config; // Found a matching service ID
            }
        }

        // Log if service ID was not found in the list
        System.out.println("Service with ID " + serviceId + " not found in serviceConfigs.");
        return null;
    }

    private static String checkServerStatus(ServiceConfig service) {
//        check server
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(service.getServiceHost(), service.getServicePort()), 5000);
            return "Online";
        } catch (IOException e) {
            return "Offline";
        }
    }
    private static void configureSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    private static String checkApplicationStatus(ServiceConfig service) {
        try {
            configureSSLContext();
            URL url = new URL("https", service.getServiceHost(), service.getServiceResourceURI());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(service.getServiceMethod());
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 300) ? "Up" : "Down";
        } catch (IOException e) {
            return "Down";
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            return "SSL Configuration Error";
        }
    }
    private static void listServices() {
        String rowFormat = "| %-20s | %-10s | %-35s | %-15s | %-15s |%n";

        // Calculate the length of a single row and create the border line
        int rowLength = String.format(rowFormat, "", "", "", "", "").length();
        String border = "+" + new String(new char[rowLength - 2]).replace("\0", "-") + "+";

        // Print the top border
        System.out.println(border);

        // Print the header of the table
        System.out.printf(rowFormat, "Timestamp", "Service ID", "Service Name", "Server Status", "Application Status");

        // Print the separator line
        System.out.println(border);

        // Iterate through serviceConfigs and print each service's information
        for (ServiceConfig serviceConfig : serviceConfigs) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String telnetStatus = checkServerStatus(serviceConfig);
            String httpStatus = checkApplicationStatus(serviceConfig);

            System.out.printf(
                    rowFormat,
                    timestamp,
                    serviceConfig.getServiceId(),
                    serviceConfig.getServiceName(),
                    telnetStatus,
                    httpStatus
            );
        }

        // Print the bottom border
        System.out.println(border);
    }

    private static void writeLog(String serviceId, String serviceName, String fileLoggingInterval, String lastLogTime, String logType, String logEntry, ServiceConfig service) {
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Path logsDirectoryPath = Paths.get("src", "main", "resources", "logs", serviceName);
            Files.createDirectories(logsDirectoryPath);

            Date timeStamp;
            if (lastLogTime == null || lastLogTime.trim().isEmpty()) {
                timeStamp = getCurrentTimeStamp();
                String formattedTimeStamp = sdf.format(timeStamp);
                updateConfig(service.getServiceId(), formattedTimeStamp, "lastLogTime");
                service.setLastLogTime(formattedTimeStamp);
            } else {
                timeStamp = sdf.parse(lastLogTime);
            }


            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timeStamp); // Set the calendar
            Date currentTime = new Date();
            //Determine the next log time based on the fileLoggingInterval
            switch (fileLoggingInterval.toLowerCase()) {
                case "hourly":
                    calendar.add(Calendar.HOUR, 1);
                    break;
                case "daily":
                    calendar.add(Calendar.DATE, 1);
                    break;
                case "weekly":
                    calendar.add(Calendar.DATE, 7);
                    break;
                case "monthly":
                    calendar.add(Calendar.MONTH, 1);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown fileLoggingInterval: " + fileLoggingInterval);
            }

            String currentLogFileName;
            boolean createNewLogFile = false;

            if (currentTime.after(calendar.getTime())) {
                // If current time is past the next log time, create a new log file
                timeStamp = currentTime; // Update logDate to current time
                createNewLogFile = true;
                String formattedTimeStamp = sdf.format(timeStamp);
                updateConfig(serviceId, formattedTimeStamp,"lastLogTime");
                service.setLastLogTime(formattedTimeStamp);
                currentLogFileName = logFileNameDateFormat.format(currentTime) + "_" + logType + ".log";
            } else {
                // If not creating a new log file, use the last log date for the log file name
                currentLogFileName = logFileNameDateFormat.format(timeStamp) + "_" + logType + ".log";
            }
            Path currentLogFilePath = logsDirectoryPath.resolve(currentLogFileName);

            if (createNewLogFile && !Files.exists(currentLogFilePath)) {
                Files.createFile(currentLogFilePath);
            }

            String formattedLogEntry = logEntry + System.lineSeparator();
            Files.write(currentLogFilePath, formattedLogEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }catch (IOException | ParseException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private static void updateConfig(String serviceId, String newDate,String updateType) {
        try {
            File configFile = getConfigFile();
            String fileExtension = getFileExtension(configFile);
            switch (fileExtension) {
                case "xml":
                    updateXml(configFile,serviceId,newDate,updateType);
                    break;
                case "ini":
                    updateIni(configFile,serviceId,newDate,updateType);
                    break;
                case "csv":
                    updateCsv(configFile,serviceId,newDate,updateType);
                    break;
                case "yaml":
                    updateYaml(configFile,serviceId,newDate,updateType);
                    break;
                case "json":
                    updateJson(configFile,serviceId,newDate,updateType);
                    break;
                    default:
                        System.out.println("Unsupported update type.");
                        break;

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error in updateConfig method");
        }
    }

    //updateXml config method
    public synchronized static void updateXml(File configFile, String serviceId, String newDate, String updateType) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(configFile);
            document.getDocumentElement().normalize();

            NodeList serviceList = document.getElementsByTagName("serviceConfig");
            boolean serviceFound = false;

            for (int i = 0; i < serviceList.getLength(); i++) {
                Element service = (Element) serviceList.item(i);
                if (service.getElementsByTagName("serviceId").item(0).getTextContent().equals(serviceId)) {
                    if (updateType.equals("lastLogTime")) {
                        service.getElementsByTagName("lastLogTime").item(0).setTextContent(newDate);
                    } else if (updateType.equals("lastArchivingTime")) {
                        service.getElementsByTagName("lastArchivingTime").item(0).setTextContent(newDate);
                    }
                    serviceFound = true;
                    break;
                }
            }

            if (!serviceFound) {
                throw new IllegalArgumentException("Service ID not found in XML file");
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(configFile);
            transformer.transform(source, result);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in updateXml: " + e.getMessage());
        }
    }

          //UpdateIni config method
    public synchronized static void updateIni(File configFile, String serviceId, String newDate, String updateType) {
        try {
            // Load the INI file
            List<String> fileContent = new ArrayList<>(Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8));

            boolean serviceFound = false;
            for (int i = 0; i < fileContent.size(); i++) {
                String line = fileContent.get(i);

                if (line.trim().startsWith("[Service") && line.contains(serviceId)) {
                    serviceFound = true;
                }
                if("lastLogTime".equalsIgnoreCase(updateType)){
                    if (serviceFound && line.startsWith("lastLogTime")) {
                        fileContent.set(i, "lastLogTime = " + newDate);
                        break;
                    }
                } else if ("lastArchivingTime".equalsIgnoreCase(updateType)) {
                    if (serviceFound && line.startsWith("lastArchivingTime")) {
                        fileContent.set(i, "lastArchivingTime = " + newDate);
                        break;
                    }
                }
            }


            if (!serviceFound) {
                throw new IllegalArgumentException("Service ID not found in ini file");
            }

            // Write updated content back to the INI file
            Files.write(configFile.toPath(), fileContent, StandardCharsets.UTF_8);

        } catch (IOException e) {
            System.err.println("I/O Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //updateCsv config method
    public synchronized static void updateCsv(File configFile, String serviceId, String newDate, String updateType) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            List<String[]> records = new ArrayList<>();
            String line;

            // Read existing CSV records
            while ((line = reader.readLine()) != null) {
                String[] recordData = line.split(",", -1); // -1 to include trailing empty strings
                records.add(recordData);
            }

            // Identify the column index to update
            int timeIndex;
            if ("lastLogTime".equalsIgnoreCase(updateType)) {
                timeIndex = 13; // Assuming index 13 is Last Log Time
            } else if ("lastArchivingTime".equalsIgnoreCase(updateType)) {
                timeIndex = 16; // Assuming index 16 is Last Archive Time
            } else {
                // Handle unsupported update types
                System.out.println("Unsupported update type.");
                return;
            }

            // Update the record with the new date based on serviceId
            for (int i = 1; i < records.size(); i++) {
                String[] recordData = records.get(i);
                if (recordData[1].equals(serviceId)) { // Assuming index 1 is serviceId
                    recordData[timeIndex] = newDate;
                    break; // Assuming there is only one record per serviceId
                }
            }

            // Write back the updated records to the CSV file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
                for (String[] recordData : records) {
                    String lineToWrite = String.join(",", recordData);
                    writer.write(lineToWrite);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error updating CSV: " + e.getMessage());
        }
    }

    //updateYaml config method
    public synchronized static void updateYaml(File configFile, String serviceId, String newDate, String updateType) {
        Yaml yaml = new Yaml();
        InputStream inputStream = null;
        FileWriter writer = null;
        try {
            // Read the YAML file
            inputStream = new FileInputStream(configFile);
            List<Map<String, Object>> services = yaml.load(inputStream);

            boolean serviceFound = false;
            for (Map<String, Object> service : services) {
                if (serviceId.equals(service.get("serviceId").toString())) {
                    if ("lastLogTime".equals(updateType) || "lastArchivingTime".equals(updateType)) {
                        // Handle optional fields that might be empty
                        service.put(updateType, newDate);
                    } else {
                        // Handle other update types if needed
                        System.out.println("Unsupported update type.");
                        return;
                    }
                    serviceFound = true;
                    break; // Assuming unique serviceId
                }
            }

            if (!serviceFound) {
                throw new IllegalArgumentException("Service ID not found in YAML file");
            }

            // Write the updated YAML back to file
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            yaml = new Yaml(options);

            writer = new FileWriter(configFile);
            yaml.dump(services, writer);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in updateYaml: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

// updateJson method
    public synchronized static void updateJson(File configFile, String serviceId, String newDate, String updateType) {
        Gson gson = new Gson();

        try (Reader reader = new FileReader(configFile)) {
            Type listType = new TypeToken<List<ServiceConfig>>() {}.getType();
            List<ServiceConfig> serviceConfigs = gson.fromJson(reader, listType);

            if (serviceConfigs != null) {
                boolean serviceFound = false;

                for (ServiceConfig config : serviceConfigs) {
                    if (serviceId.equals(config.getServiceId())) {
                        if ("lastLogTime".equalsIgnoreCase(updateType)) {
                            config.setLastLogTime(newDate);
                        } else if ("lastArchivingTime".equalsIgnoreCase(updateType)) {
                            config.setLastArchivingTime(newDate);
                        } else {
                            System.out.println("Unsupported update type: " + updateType);
                        }
                        serviceFound = true;
                        break;
                    }
                }

                if (!serviceFound) {
                    System.out.println("Service ID not found in JSON file");
                    return;
                }

                try (Writer writer = new FileWriter(configFile)) {
                    gson.toJson(serviceConfigs, writer);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}




