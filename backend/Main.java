import java.sql.*;
import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class Main {

    static Connection con;

    public static void main(String[] args) {

        try {

            String dbUrl = (args.length > 0 && args[0] != null && !args[0].trim().isEmpty())
                ? args[0].trim()
                : System.getenv("DATABASE_URL");

            if (dbUrl == null || dbUrl.trim().isEmpty()) {
                File propFile = new File("db.properties");
                if (!propFile.exists()) propFile = new File("backend/db.properties");
                if (propFile.exists()) {
                    Properties props = new Properties();
                    try (FileInputStream fis = new FileInputStream(propFile)) {
                        props.load(fis);
                        dbUrl = props.getProperty("DATABASE_URL");
                    } catch (Exception ignored) {}
                }
            }

            if (dbUrl != null && !dbUrl.trim().isEmpty()) {
                System.out.println("Connecting to Cloud Database...");
                if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
                    URI uri = new URI(dbUrl.replace("postgres://", "http://").replace("postgresql://", "http://"));
                    String userInfo = uri.getUserInfo();
                    String user = null;
                    String pass = null;
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        user = parts[0];
                        pass = parts[1];
                    }
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();
                    String query = uri.getQuery();
                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null ? "?" + query : "?sslmode=require");
                    Class.forName("org.postgresql.Driver");
                    if (user != null && pass != null) {
                        con = DriverManager.getConnection(jdbcUrl, user, pass);
                    } else {
                        con = DriverManager.getConnection(jdbcUrl);
                    }
                } else if (dbUrl.startsWith("jdbc:postgresql:")) {
                    Class.forName("org.postgresql.Driver");
                    con = DriverManager.getConnection(dbUrl);
                } else {
                    con = DriverManager.getConnection(dbUrl);
                }
            } else {
                System.out.println("No DATABASE_URL supplied, falling back to local Oracle...");
                Class.forName("oracle.jdbc.OracleDriver");
                con = DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521:XE",
                    "system",
                    "1"
                );
            }

            System.out.println("DATABASE CONNECTED SUCCESSFULLY!");

            boolean isPostgres = false;
            try {
                isPostgres = con.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
            } catch (Exception ignored) {}

            try (Statement initStmt = con.createStatement()) {
                if (isPostgres) {
                    initStmt.execute("CREATE TABLE IF NOT EXISTS PROJECTS (" +
                        "PROJECT_ID VARCHAR(10) PRIMARY KEY, " +
                        "PROJECT_NAME VARCHAR(100) NOT NULL, " +
                        "LOCATION VARCHAR(50) NOT NULL, " +
                        "MANAGER VARCHAR(50) NOT NULL, " +
                        "PROGRESS INT CHECK (PROGRESS BETWEEN 0 AND 100), " +
                        "STATUS VARCHAR(20) CHECK (STATUS IN ('ACTIVE', 'COMPLETED', 'PLANNING')))");

                    initStmt.execute("CREATE TABLE IF NOT EXISTS PROJECT_ESTIMATIONS (" +
                        "EST_ID VARCHAR(20) PRIMARY KEY, " +
                        "PROJECT_NAME VARCHAR(100) NOT NULL, " +
                        "PROJECT_TYPE VARCHAR(30) NOT NULL, " +
                        "QUALITY_GRADE VARCHAR(20) NOT NULL, " +
                        "AREA_SQFT NUMERIC(10,2) NOT NULL, " +
                        "ALLOCATED_BUDGET NUMERIC(14,2) NOT NULL, " +
                        "ESTIMATED_COST NUMERIC(14,2) NOT NULL, " +
                        "REQUIRED_WORKERS INT, " +
                        "STEEL_TONS NUMERIC(8,2), " +
                        "CEMENT_BAGS INT, " +
                        "FEASIBILITY_STATUS VARCHAR(20), " +
                        "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

                    ResultSet rsCount = initStmt.executeQuery("SELECT COUNT(*) FROM PROJECTS");
                    if (rsCount.next() && rsCount.getInt(1) == 0) {
                        initStmt.execute("INSERT INTO PROJECTS VALUES " +
                            "('P001', 'Academic Block', 'Chennai', 'R. Kumar', 80, 'ACTIVE'), " +
                            "('P002', 'Hostel Complex', 'Chennai', 'S. Arun', 62, 'ACTIVE'), " +
                            "('P003', 'Central Library', 'Vellore', 'A. Prakash', 100, 'COMPLETED'), " +
                            "('P004', 'Research Centre', 'Chennai', 'V. Raj', 35, 'PLANNING')");
                        System.out.println("POSTGRESQL SEED DATA AUTO-INSERTED (4 projects)!");
                    }
                    rsCount.close();
                    System.out.println("POSTGRES TABLES VERIFIED & READY!");
                } else {
                    try {
                        initStmt.execute("CREATE TABLE PROJECT_ESTIMATIONS (" +
                            "EST_ID VARCHAR2(20) PRIMARY KEY, " +
                            "PROJECT_NAME VARCHAR2(100) NOT NULL, " +
                            "PROJECT_TYPE VARCHAR2(30) NOT NULL, " +
                            "QUALITY_GRADE VARCHAR2(20) NOT NULL, " +
                            "AREA_SQFT NUMBER(10,2) NOT NULL, " +
                            "ALLOCATED_BUDGET NUMBER(14,2) NOT NULL, " +
                            "ESTIMATED_COST NUMBER(14,2) NOT NULL, " +
                            "REQUIRED_WORKERS NUMBER(5), " +
                            "STEEL_TONS NUMBER(8,2), " +
                            "CEMENT_BAGS NUMBER(8), " +
                            "FEASIBILITY_STATUS VARCHAR2(20), " +
                            "CREATED_AT DATE DEFAULT SYSDATE)");
                        System.out.println("PROJECT_ESTIMATIONS TABLE CREATED!");
                    } catch (SQLException sqle) {
                        // Table already exists
                    }
                }
            } catch (SQLException sqle) {
                System.err.println("Database init note: " + sqle.getMessage());
            }

            int listenPort = 8081;
            String portEnv = System.getenv("PORT");
            if (portEnv != null && !portEnv.trim().isEmpty()) {
                try { listenPort = Integer.parseInt(portEnv.trim()); } catch (Exception ignored) {}
            }

            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", listenPort), 0);

            server.createContext("/projects", Main::getProjects);
            server.createContext("/insert", Main::insertProject);
            server.createContext("/update", Main::updateProject);
            server.createContext("/delete", Main::deleteProject);
            server.createContext("/evaluate", Main::evaluateProject);
            server.createContext("/estimate", Main::processEstimation);
            server.createContext("/estimations", Main::getEstimations);

            server.start();

            System.out.println("SERVER RUNNING AT http://localhost:" + listenPort);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ================= GET PROJECTS =================

    static void getProjects(HttpExchange exchange) {

        try {

            if (handleCors(exchange)) return;

            Statement stmt = con.createStatement();

            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM PROJECTS"
            );

            StringBuilder json = new StringBuilder("[");

            while (rs.next()) {

                json.append("{");

                json.append("\"id\":\"")
                    .append(rs.getString("PROJECT_ID"))
                    .append("\",");

                json.append("\"name\":\"")
                    .append(rs.getString("PROJECT_NAME"))
                    .append("\",");

                json.append("\"location\":\"")
                    .append(rs.getString("LOCATION"))
                    .append("\",");

                json.append("\"manager\":\"")
                    .append(rs.getString("MANAGER"))
                    .append("\",");

                json.append("\"progress\":")
                    .append(rs.getInt("PROGRESS"))
                    .append(",");

                json.append("\"status\":\"")
                    .append(rs.getString("STATUS"))
                    .append("\"");

                json.append("},");
            }

            if (json.length() > 1)
                json.deleteCharAt(json.length() - 1);

            json.append("]");

            send(exchange, 200, json.toString());

            rs.close();
            stmt.close();

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "ERROR");
        }
    }


    // ================= INSERT =================

    static void insertProject(HttpExchange exchange) {

        try {

            if (handleCors(exchange)) return;

            Map<String, String> p =
                getParams(exchange.getRequestURI().getQuery());

            PreparedStatement stmt = con.prepareStatement(
                "INSERT INTO PROJECTS VALUES (?, ?, ?, ?, ?, ?)"
            );

            stmt.setString(1, p.get("id"));
            stmt.setString(2, p.get("name"));
            stmt.setString(3, p.get("location"));
            stmt.setString(4, p.get("manager"));
            stmt.setInt(5, Integer.parseInt(p.get("progress")));
            stmt.setString(6, p.get("status"));

            stmt.executeUpdate();

            stmt.close();

            send(exchange, 200, "INSERT SUCCESSFUL");

        } catch (Exception e) {

            e.printStackTrace();

            send(exchange, 500, "INSERT FAILED");
        }
    }


    // ================= UPDATE =================

    static void updateProject(HttpExchange exchange) {

        try {

            if (handleCors(exchange)) return;

            Map<String, String> p =
                getParams(exchange.getRequestURI().getQuery());

            PreparedStatement stmt = con.prepareStatement(
                "UPDATE PROJECTS SET PROJECT_NAME=?, LOCATION=?, MANAGER=?, PROGRESS=?, STATUS=? WHERE PROJECT_ID=?"
            );

            stmt.setString(1, p.get("name"));
            stmt.setString(2, p.get("location"));
            stmt.setString(3, p.get("manager"));
            stmt.setInt(4, Integer.parseInt(p.get("progress")));
            stmt.setString(5, p.get("status"));
            stmt.setString(6, p.get("id"));

            int rows = stmt.executeUpdate();

            stmt.close();

            if (rows > 0)
                send(exchange, 200, "UPDATE SUCCESSFUL");
            else
                send(exchange, 404, "PROJECT NOT FOUND");

        } catch (Exception e) {

            e.printStackTrace();

            send(exchange, 500, "UPDATE FAILED");
        }
    }


    // ================= DELETE =================

    static void deleteProject(HttpExchange exchange) {

        try {

            if (handleCors(exchange)) return;

            Map<String, String> p =
                getParams(exchange.getRequestURI().getQuery());

            PreparedStatement stmt = con.prepareStatement(
                "DELETE FROM PROJECTS WHERE PROJECT_ID=?"
            );

            stmt.setString(1, p.get("id"));

            int rows = stmt.executeUpdate();

            stmt.close();

            if (rows > 0)
                send(exchange, 200, "DELETE SUCCESSFUL");
            else
                send(exchange, 404, "PROJECT NOT FOUND");

        } catch (Exception e) {

            e.printStackTrace();

            send(exchange, 500, "DELETE FAILED");
        }
    }


    // ================= EVALUATE PROJECT SCHEDULE & HEALTH =================

    static void evaluateProject(HttpExchange exchange) {
        try {
            if (handleCors(exchange)) return;
            Map<String, String> p = getParams(exchange.getRequestURI().getQuery());
            String id = p.getOrDefault("id", "").trim();
            int currentProgress = Integer.parseInt(p.getOrDefault("progress", "0"));
            int daysElapsed = Integer.parseInt(p.getOrDefault("daysElapsed", "30"));
            int targetDays = Integer.parseInt(p.getOrDefault("targetDays", "90"));
            boolean autoUpdate = "true".equalsIgnoreCase(p.getOrDefault("autoUpdate", "false"));

            if (currentProgress < 0) currentProgress = 0;
            if (currentProgress > 100) currentProgress = 100;

            PreparedStatement pstmt = con.prepareStatement(
                "SELECT PROJECT_NAME, LOCATION, MANAGER FROM PROJECTS WHERE PROJECT_ID = ?"
            );
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            String name = "Project " + id;
            String location = "N/A";
            String manager = "N/A";

            if (rs.next()) {
                name = rs.getString("PROJECT_NAME");
                location = rs.getString("LOCATION");
                manager = rs.getString("MANAGER");
            }
            rs.close();
            pstmt.close();

            int remainingWork = 100 - currentProgress;
            int projectedTotalDays;
            int variance;
            String health;
            String newStatus;
            String recommendation;

            if (currentProgress == 100) {
                newStatus = "COMPLETED";
                health = "COMPLETED";
                projectedTotalDays = daysElapsed;
                variance = projectedTotalDays - targetDays;
                recommendation = "Project successfully delivered. All site deliverables completed.";
            } else if (currentProgress == 0) {
                newStatus = "PLANNING";
                health = "PLANNING PHASE";
                projectedTotalDays = targetDays;
                variance = 0;
                recommendation = "Project awaiting site mobilization and team allocation.";
            } else {
                newStatus = "ACTIVE";
                double dailyPace = (double) currentProgress / Math.max(1, daysElapsed);
                projectedTotalDays = (int) Math.round(100.0 / dailyPace);
                variance = projectedTotalDays - targetDays;

                if (variance <= 0) {
                    health = "ON TRACK";
                    recommendation = "Velocity is healthy. Project on schedule to finish on time.";
                } else if (variance <= 25) {
                    health = "DELAY RISK";
                    recommendation = "Behind target pace. Recommend allocating additional manpower to recover " + variance + " days.";
                } else {
                    health = "CRITICAL DELAY";
                    recommendation = "Severe milestone delay (" + variance + " days behind). Escalation and emergency intervention required.";
                }
            }

            if (autoUpdate) {
                PreparedStatement updateStmt = con.prepareStatement(
                    "UPDATE PROJECTS SET PROGRESS = ?, STATUS = ? WHERE PROJECT_ID = ?"
                );
                updateStmt.setInt(1, currentProgress);
                updateStmt.setString(2, newStatus);
                updateStmt.setString(3, id);
                updateStmt.executeUpdate();
                updateStmt.close();
            }

            StringBuilder json = new StringBuilder("{");
            json.append("\"status\":\"SUCCESS\",");
            json.append("\"id\":\"").append(id).append("\",");
            json.append("\"name\":\"").append(name.replace("\"", "\\\"")).append("\",");
            json.append("\"location\":\"").append(location).append("\",");
            json.append("\"manager\":\"").append(manager).append("\",");
            json.append("\"progress\":").append(currentProgress).append(",");
            json.append("\"remainingWork\":").append(remainingWork).append(",");
            json.append("\"daysElapsed\":").append(daysElapsed).append(",");
            json.append("\"targetDays\":").append(targetDays).append(",");
            json.append("\"projectedDays\":").append(projectedTotalDays).append(",");
            json.append("\"variance\":").append(variance).append(",");
            json.append("\"health\":\"").append(health).append("\",");
            json.append("\"newStatus\":\"").append(newStatus).append("\",");
            json.append("\"recommendation\":\"").append(recommendation).append("\",");
            json.append("\"dbUpdated\":").append(autoUpdate);
            json.append("}");

            send(exchange, 200, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }


    // ================= ESTIMATION & FEASIBILITY =================

    static void processEstimation(HttpExchange exchange) {
        try {
            if (handleCors(exchange)) return;
            Map<String, String> p = getParams(exchange.getRequestURI().getQuery());
            String name = p.getOrDefault("name", "New Project");
            String type = p.getOrDefault("type", "Residential");
            String grade = p.getOrDefault("grade", "Standard");
            double area = Double.parseDouble(p.getOrDefault("area", "1000"));
            double budget = Double.parseDouble(p.getOrDefault("budget", "0"));
            boolean save = "true".equalsIgnoreCase(p.getOrDefault("save", "false"));

            double baseRate = 1800.0;
            if ("Premium".equalsIgnoreCase(grade)) baseRate = 2400.0;
            else if ("Luxury".equalsIgnoreCase(grade)) baseRate = 3200.0;

            double typeMultiplier = 1.0;
            if ("Commercial".equalsIgnoreCase(type)) typeMultiplier = 1.15;
            else if ("Industrial".equalsIgnoreCase(type)) typeMultiplier = 1.25;

            double estimatedCost = Math.round(area * baseRate * typeMultiplier);
            int workers = Math.max(5, (int) Math.round(area / 150.0));
            double steelTons = Math.round(((area * 3.8) / 1000.0) * 100.0) / 100.0;
            int cementBags = (int) Math.round(area * 0.4);

            double variance = budget - estimatedCost;
            String feasibilityStatus = (variance >= 0) ? "FEASIBLE" : "BUDGET DEFICIT";
            String estId = "EST" + (System.currentTimeMillis() % 1000000);

            if (save) {
                PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO PROJECT_ESTIMATIONS (EST_ID, PROJECT_NAME, PROJECT_TYPE, QUALITY_GRADE, AREA_SQFT, ALLOCATED_BUDGET, ESTIMATED_COST, REQUIRED_WORKERS, STEEL_TONS, CEMENT_BAGS, FEASIBILITY_STATUS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                );
                stmt.setString(1, estId);
                stmt.setString(2, name);
                stmt.setString(3, type);
                stmt.setString(4, grade);
                stmt.setDouble(5, area);
                stmt.setDouble(6, budget);
                stmt.setDouble(7, estimatedCost);
                stmt.setInt(8, workers);
                stmt.setDouble(9, steelTons);
                stmt.setInt(10, cementBags);
                stmt.setString(11, feasibilityStatus);
                stmt.executeUpdate();
                stmt.close();
            }

            StringBuilder json = new StringBuilder("{");
            json.append("\"status\":\"SUCCESS\",");
            json.append("\"estId\":\"").append(estId).append("\",");
            json.append("\"projectName\":\"").append(name.replace("\"", "\\\"")).append("\",");
            json.append("\"projectType\":\"").append(type).append("\",");
            json.append("\"qualityGrade\":\"").append(grade).append("\",");
            json.append("\"area\":").append(area).append(",");
            json.append("\"budget\":").append(budget).append(",");
            json.append("\"estimatedCost\":").append(estimatedCost).append(",");
            json.append("\"variance\":").append(variance).append(",");
            json.append("\"feasibilityStatus\":\"").append(feasibilityStatus).append("\",");
            json.append("\"requiredWorkers\":").append(workers).append(",");
            json.append("\"steelTons\":").append(steelTons).append(",");
            json.append("\"cementBags\":").append(cementBags).append(",");
            json.append("\"saved\":").append(save);
            json.append("}");

            send(exchange, 200, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    static void getEstimations(HttpExchange exchange) {
        try {
            if (handleCors(exchange)) return;
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM PROJECT_ESTIMATIONS ORDER BY CREATED_AT DESC");
            StringBuilder json = new StringBuilder("[");

            while (rs.next()) {
                json.append("{");
                json.append("\"id\":\"").append(rs.getString("EST_ID")).append("\",");
                json.append("\"name\":\"").append(rs.getString("PROJECT_NAME")).append("\",");
                json.append("\"type\":\"").append(rs.getString("PROJECT_TYPE")).append("\",");
                json.append("\"grade\":\"").append(rs.getString("QUALITY_GRADE")).append("\",");
                json.append("\"area\":").append(rs.getDouble("AREA_SQFT")).append(",");
                json.append("\"budget\":").append(rs.getDouble("ALLOCATED_BUDGET")).append(",");
                json.append("\"estimatedCost\":").append(rs.getDouble("ESTIMATED_COST")).append(",");
                json.append("\"workers\":").append(rs.getInt("REQUIRED_WORKERS")).append(",");
                json.append("\"steel\":").append(rs.getDouble("STEEL_TONS")).append(",");
                json.append("\"cement\":").append(rs.getInt("CEMENT_BAGS")).append(",");
                json.append("\"status\":\"").append(rs.getString("FEASIBILITY_STATUS")).append("\"");
                json.append("},");
            }

            if (json.length() > 1)
                json.deleteCharAt(json.length() - 1);

            json.append("]");

            send(exchange, 200, json.toString());

            rs.close();
            stmt.close();

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "[]");
        }
    }


    // ================= PARAMETERS =================

    static Map<String, String> getParams(String query) {

        Map<String, String> map = new HashMap<>();

        if (query == null)
            return map;

        for (String part : query.split("&")) {

            String[] pair = part.split("=", 2);

            if (pair.length == 2) {

                try {

                    map.put(
                        URLDecoder.decode(pair[0], "UTF-8"),
                        URLDecoder.decode(pair[1], "UTF-8")
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return map;
    }


    // ================= RESPONSE & CORS =================

    static boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, *");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    static void send(
        HttpExchange exchange,
        int code,
        String response
    ) {

        try {

            exchange.getResponseHeaders()
                .set("Content-Type", "application/json; charset=UTF-8");

            exchange.getResponseHeaders()
                .set("Access-Control-Allow-Origin", "*");

            exchange.getResponseHeaders()
                .set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

            exchange.getResponseHeaders()
                .set("Access-Control-Allow-Headers", "Content-Type, Authorization, *");

            byte[] data = response.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(
                code,
                data.length
            );

            OutputStream output =
                exchange.getResponseBody();

            output.write(data);
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}