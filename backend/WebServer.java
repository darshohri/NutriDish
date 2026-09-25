import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer {

    private static final int PORT = 8080;
    private static final Map<String, Long> rateLimits = new ConcurrentHashMap<>();
    
    private static boolean checkRateLimit(HttpExchange exchange) {
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        long now = System.currentTimeMillis();
        long last = rateLimits.getOrDefault(ip, 0L);
        if (now - last < 300) { // 300ms limit
            return false;
        }
        rateLimits.put(ip, now);
        return true;
    }
    
    private static String sanitizeHTML(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }
    
    // Check root and current dir for files
    private static String findFile(String name) {
        if (new File(name).exists()) return name;
        if (new File("../" + name).exists()) return "../" + name;
        return name;
    }

    private static NutritionCSVReader csvReader = new NutritionCSVReader(findFile("nutrition.csv"));
    private static DishLogger dishLogger = new DishLogger(findFile("dishes_log.txt"));

    // Resolve the absolute path to the frontend directory for path traversal protection
    private static File frontendDir;
    static {
        File f = new File("frontend");
        if (!f.exists()) f = new File("../frontend");
        try {
            frontendDir = f.getCanonicalFile();
        } catch (IOException e) {
            frontendDir = f.getAbsoluteFile();
        }
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Handler for ingredient search (autocomplete)
        server.createContext("/api/search", (exchange) -> {
            if (!checkRateLimit(exchange)) { sendResponse(exchange, 429, "{\"error\":\"Too Many Requests\"}", "application/json"); return; }
            String queryArg = exchange.getRequestURI().getQuery();
            String q = "";
            if (queryArg != null && queryArg.startsWith("q=")) {
                q = sanitizeHTML(URLDecoder.decode(queryArg.substring(2), "UTF-8"));
            }

            List<String> results = csvReader.searchIngredients(q);
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                // Escape ingredient name for JSON safety
                String safeName = results.get(i)
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");
                json.append("\"").append(safeName).append("\"");
                if (i < results.size() - 1) json.append(",");
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString(), "application/json");
        });

        // API Handler for ingredient lookup
        server.createContext("/api/lookup", (exchange) -> {
            if (!checkRateLimit(exchange)) { sendResponse(exchange, 429, "{\"error\":\"Too Many Requests\"}", "application/json"); return; }
            String queryArg = exchange.getRequestURI().getQuery();
            String name = "";
            if (queryArg != null && queryArg.startsWith("name=")) {
                name = sanitizeHTML(URLDecoder.decode(queryArg.substring(5), "UTF-8"));
            }

            double[] macros = csvReader.findIngredient(name);
            String response;
            if (macros != null) {
                String safeName = name.replace("\\", "\\\\").replace("\"", "\\\"");
                response = String.format("{\"name\":\"%s\",\"protein\":%.4f,\"carbs\":%.4f,\"fats\":%.4f}", 
                                         safeName, macros[0], macros[1], macros[2]);
                sendResponse(exchange, 200, response, "application/json");
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}", "application/json");
            }
        });

        // API Handler for saving a dish
        server.createContext("/api/save", (exchange) -> {
            if (!checkRateLimit(exchange)) { sendResponse(exchange, 429, "{\"error\":\"Too Many Requests\"}", "application/json"); return; }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String body = br.lines().collect(Collectors.joining());

                    // Manual JSON parsing (brute force but slightly more robust)
                    String dishName = sanitizeHTML(extractJsonValue(body, "name"));
                    if (dishName == null || dishName.trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"error\":\"Dish name is required\"}", "application/json");
                        return;
                    }

                    Dish dish = new Dish(dishName);

                    if (body.contains("\"ingredients\":[")) {
                        String ingredientsPart = body.split("\"ingredients\":\\[")[1];
                        ingredientsPart = ingredientsPart.substring(0, ingredientsPart.indexOf("]"));
                        
                        if (!ingredientsPart.trim().isEmpty()) {
                            String[] ingredientObjects = ingredientsPart.split("\\},\\{");
                            for (String obj : ingredientObjects) {
                                String ingName = sanitizeHTML(extractJsonValue(obj, "name"));
                                double weight = Double.parseDouble(extractJsonValue(obj, "weight"));
                                double p = Double.parseDouble(extractJsonValue(obj, "proteinPerGram"));
                                double c = Double.parseDouble(extractJsonValue(obj, "carbsPerGram"));
                                double f = Double.parseDouble(extractJsonValue(obj, "fatsPerGram"));
                                
                                dish.addIngredient(new Ingredient(ingName, weight, p, c, f));
                            }
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
            if (!checkRateLimit(exchange)) { sendResponse(exchange, 429, "{\"error\":\"Too Many Requests\"}", "application/json"); return; }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String body = br.lines().collect(Collectors.joining());

                    String name = sanitizeHTML(extractJsonValue(body, "name"));
                    if (name == null || name.trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"error\":\"Ingredient name is required\"}", "application/json");
                        return;
                    }

                    double p = Double.parseDouble(extractJsonValue(body, "proteinPerGram"));
                    double c = Double.parseDouble(extractJsonValue(body, "carbsPerGram"));
                    double f = Double.parseDouble(extractJsonValue(body, "fatsPerGram"));

                    // Check for negative values
                    if (p < 0 || c < 0 || f < 0) {
                        sendResponse(exchange, 400, "{\"error\":\"Nutritional values cannot be negative\"}", "application/json");
                        return;
                    }

                    csvReader.addIngredientToCSV(name, p, c, f);
                    sendResponse(exchange, 200, "{\"status\":\"added\"}", "application/json");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        });

        // API Handler for deleting a logged dish
        server.createContext("/api/delete-dish", (exchange) -> {
            if (!checkRateLimit(exchange)) { sendResponse(exchange, 429, "{\"error\":\"Too Many Requests\"}", "application/json"); return; }
            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                String queryArg = exchange.getRequestURI().getQuery();
                String name = "";
                if (queryArg != null && queryArg.startsWith("name=")) {
                    name = sanitizeHTML(URLDecoder.decode(queryArg.substring(5), "UTF-8"));
                }

                if (name == null || name.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Dish name is required\"}", "application/json");
                    return;
                }

                boolean deleted = dishLogger.deleteDish(name);
                if (deleted) {
                    sendResponse(exchange, 200, "{\"status\":\"deleted\"}", "application/json");
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Dish not found\"}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        });

        // API Handler for retrieving logged dishes
        server.createContext("/api/logged-dishes", (exchange) -> {
            if (!checkRateLimit(exchange)) { sendResponse(exchange, 429, "{\"error\":\"Too Many Requests\"}", "application/json"); return; }
            String json = dishLogger.getLoggedDishesJSON();
            sendResponse(exchange, 200, json, "application/json");
        });

        // Static file handler with path traversal protection
        server.createContext("/", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/landing.html";
            
            // Try different possible locations for frontend files
            File file = new File(frontendDir, path);

            try {
                // Path traversal protection: ensure resolved path is inside frontendDir
                File canonicalFile = file.getCanonicalFile();
                if (!canonicalFile.getPath().startsWith(frontendDir.getPath())) {
                    sendResponse(exchange, 403, "403 Forbidden", "text/plain");
                    return;
                }
                file = canonicalFile;
            } catch (IOException e) {
                sendResponse(exchange, 400, "400 Bad Request", "text/plain");
                return;
            }

            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
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

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".woff") || path.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
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
        // Security Headers (CORS, XSS, Frame Options)
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("X-XSS-Protection", "1; mode=block");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.gstatic.com https://cdnjs.cloudflare.com https://unpkg.com https://cdn.jsdelivr.net; img-src 'self' data:;");
        // Removed Access-Control-Allow-Origin: * to prevent cross-origin API access
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        
        exchange.sendResponseHeaders(statusCode, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }
}



