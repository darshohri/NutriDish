import java.io.*;
import java.util.ArrayList;

public class DishLogger {

    private String filePath;

    public DishLogger(String filePath) {
        this.filePath = filePath;
    }

    public boolean dishExists(String dishName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equalsIgnoreCase("DISH:" + dishName.trim())) {
                    return true;
                }
            }
        } catch (IOException e) {
            // File may not exist yet — that's fine, dish doesn't exist
            return false;
        }
        return false;
    }

    public void displaySavedDish(String dishName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean inDish = false;

            while ((line = reader.readLine()) != null) {
                if (line.equalsIgnoreCase("DISH:" + dishName.trim())) {
                    inDish = true;
                }
                if (inDish) {
                    System.out.println(line);
                }
                if (inDish && line.equals("END")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading saved dish: " + e.getMessage());
        }
    }

    public void saveDish(Dish dish) {
        ArrayList<String> allLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean skipping = false;

            while ((line = reader.readLine()) != null) {
                // Case-insensitive match for overwrite
                if (line.equalsIgnoreCase("DISH:" + dish.getDishName().trim())) {
                    skipping = true;
                }
                if (!skipping) {
                    allLines.add(line);
                }
                if (skipping && line.equals("END")) {
                    skipping = false;
                }
            }
        } catch (IOException e) {
            // File may not exist yet — that's okay, we'll create it
            System.out.println("Note: No existing log file found, creating new one.");
        }

        double[] macros = dish.calculateTotalMacros();

        allLines.add("DISH:" + dish.getDishName());
        ArrayList<Ingredient> ingredients = dish.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            allLines.add("  - " + ing.getName() + " (" + ing.getWeightInGrams() + "g)"
                    + " | Protein: " + String.format("%.2f", ing.getProtein()) + "g"
                    + " | Carbs: " + String.format("%.2f", ing.getCarbohydrates()) + "g"
                    + " | Fats: " + String.format("%.2f", ing.getFats()) + "g");
        }
        allLines.add("  TOTAL Protein: " + String.format("%.2f", macros[0]) + "g"
                + " | Carbs: " + String.format("%.2f", macros[1]) + "g"
                + " | Fats: " + String.format("%.2f", macros[2]) + "g");
        allLines.add("END");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            for (String line : allLines) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Dish saved to log.");
        } catch (IOException e) {
            System.out.println("Error saving dish: " + e.getMessage());
        }
    }

    public String getLoggedDishesJSON() {
        StringBuilder json = new StringBuilder("[");
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean first = true;
            String currentDishName = null;
            double totalP = 0, totalC = 0, totalF = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("DISH:")) {
                    currentDishName = line.substring(5).trim();
                } else if (line.startsWith("TOTAL Protein:")) {
                    try {
                        String[] parts = line.split("\\|");
                        totalP = Double.parseDouble(parts[0].split(":")[1].trim().replace("g", ""));
                        totalC = Double.parseDouble(parts[1].split(":")[1].trim().replace("g", ""));
                        totalF = Double.parseDouble(parts[2].split(":")[1].trim().replace("g", ""));
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        System.err.println("Warning: Could not parse totals for dish: " + currentDishName);
                        totalP = 0; totalC = 0; totalF = 0;
                    }
                } else if (line.equals("END")) {
                    if (currentDishName != null) {
                        if (!first) json.append(",");
                        // Escape dish name for JSON safety
                        String safeName = currentDishName
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"");
                        json.append(String.format("{\"name\":\"%s\",\"protein\":%.2f,\"carbs\":%.2f,\"fats\":%.2f}",
                                    safeName, totalP, totalC, totalF));
                        first = false;
                    }
                    // Reset for next dish
                    currentDishName = null;
                    totalP = 0; totalC = 0; totalF = 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading logged dishes: " + e.getMessage());
        }
        json.append("]");
        return json.toString();
    }

    public boolean deleteDish(String dishName) {
        ArrayList<String> allLines = new ArrayList<>();
        boolean dishFound = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean skipping = false;

            while ((line = reader.readLine()) != null) {
                if (line.equalsIgnoreCase("DISH:" + dishName.trim())) {
                    skipping = true;
                    dishFound = true;
                }
                if (!skipping) {
                    allLines.add(line);
                }
                if (skipping && line.equals("END")) {
                    skipping = false;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading log file for deletion: " + e.getMessage());
            return false;
        }

        if (dishFound) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
                for (String line : allLines) {
                    writer.write(line);
                    writer.newLine();
                }
                System.out.println("Dish '" + dishName + "' deleted successfully.");
                return true;
            } catch (IOException e) {
                System.err.println("Error writing to log file during deletion: " + e.getMessage());
                return false;
            }
        } else {
            System.out.println("Dish '" + dishName + "' not found for deletion.");
            return false;
        }
    }
}
