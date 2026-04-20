import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class WebServer {

    private static final int PORT = 8080;
    
    // Check root and current dir for files
    private static String findFile(String name) {
        if (new File(name).exists()) return name;
        if (new File("../" + name).exists()) return "../" + name;
        return name;
    }

    private static NutritionCSVReader csvReader = new NutritionCSVReader(findFile("nutrition.csv"));
    private static DishLogger dishLogger = new DishLogger(findFile("dishes_log.txt"));

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Handler for ingredient search (autocomplete)
        server.createContext("/api/search", (exchange) -> {
            String queryArg = exchange.getRequestURI().getQuery();
            String q = "";
            if (queryArg != null && queryArg.startsWith("q=")) {
                q = URLDecoder.decode(queryArg.substring(2), "UTF-8");
            }

            List<String> results = csvReader.searchIngredients(q);
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                json.append("\"").append(results.get(i)).append("\"");
                if (i < results.size() - 1) json.append(",");
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString(), "application/json");
        });

        // API Handler for ingredient lookup
        server.createContext("/api/lookup", (exchange) -> {
            String queryArg = exchange.getRequestURI().getQuery();
            String name = "";
            if (queryArg != null && queryArg.startsWith("name=")) {
                name = URLDecoder.decode(queryArg.substring(5), "UTF-8");
            }

            double[] macros = csvReader.findIngredient(name);
            String response;
            if (macros != null) {
                response = String.format("{\"name\":\"%s\",\"protein\":%.2f,\"carbs\":%.2f,\"fats\":%.2f}", 
                                         name, macros[0], macros[1], macros[2]);
                sendResponse(exchange, 200, response, "application/json");
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}", "application/json");
            }
        });

        // API Handler for saving a dish
        server.createContext("/api/save", (exchange) -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String body = br.lines().collect(Collectors.joining());

                    // Manual JSON parsing (brute force but slightly more robust)
                    String dishName = extractJsonValue(body, "dishName");
                    Dish dish = new Dish(dishName, 50);

                    if (body.contains("\"ingredients\":[")) {
                        String ingredientsPart = body.split("\"ingredients\":\\[")[1];
                        ingredientsPart = ingredientsPart.substring(0, ingredientsPart.indexOf("]"));
                        
                        String[] ingredientObjects = ingredientsPart.split("\\},\\{");
                        for (String obj : ingredientObjects) {
                            String name = extractJsonValue(obj, "name");
                            double weight = Double.parseDouble(extractJsonValue(obj, "weight"));
                            double p = Double.parseDouble(extractJsonValue(obj, "proteinPerGram"));
                            double c = Double.parseDouble(extractJsonValue(obj, "carbsPerGram"));
                            double f = Double.parseDouble(extractJsonValue(obj, "fatsPerGram"));
                            
                            dish.addIngredient(new Ingredient(name, weight, p, c, f));
                        }
                    }

                    dishLogger.saveDish(dish);
                    sendResponse(exchange, 200, "{\"status\":\"saved\"}", "application/json");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 400, "{\"error\":\"Invalid data: " + e.getMessage() + "\"}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        });

        // API Handler for adding a new ingredient to database
        server.createContext("/api/add-ingredient", (exchange) -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String body = br.lines().collect(Collectors.joining());

                    String name = extractJsonValue(body, "name");
                    double p = Double.parseDouble(extractJsonValue(body, "proteinPerGram"));
                    double c = Double.parseDouble(extractJsonValue(body, "carbsPerGram"));
                    double f = Double.parseDouble(extractJsonValue(body, "fatsPerGram"));

                    csvReader.addIngredientToCSV(name, p, c, f);
                    sendResponse(exchange, 200, "{\"status\":\"added\"}", "application/json");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        });

        // API Handler for retrieving logged dishes
        server.createContext("/api/logged-dishes", (exchange) -> {
            String json = dishLogger.getLoggedDishesJSON();
            sendResponse(exchange, 200, json, "application/json");
        });

        // Static file handler
        server.createContext("/", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            
            // Try different possible locations for frontend files
            File file = new File("frontend" + path);
            if (!file.exists()) file = new File("../frontend" + path);
            if (!file.exists()) file = new File(path.substring(1)); // Current dir

            if (file.exists() && !file.isDirectory()) {
                String contentType = "text/plain";
                if (path.endsWith(".html")) contentType = "text/html";
                else if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".js")) contentType = "application/javascript";
                
                byte[] bytes = Files.readAllBytes(file.toPath());
                sendResponse(exchange, 200, bytes, contentType);
            } else {
                sendResponse(exchange, 404, "404 Not Found: " + path, "text/plain");
            }
        });

        System.out.println("NutriDish Server started at http://localhost:" + PORT);
        server.setExecutor(null);
        server.start();
    }

    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        
        if (json.charAt(start) == '\"') {
            // String value
            start++;
            int end = json.indexOf('\"', start);
            return json.substring(start, end);
        } else {
            // Number or boolean
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            return json.substring(start, end);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        sendResponse(exchange, statusCode, response.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, byte[] response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        // Fix for CORS (if needed, but local is fine)
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }
}
