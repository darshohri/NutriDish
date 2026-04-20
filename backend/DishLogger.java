import java.io.*;
import java.util.ArrayList;

public class DishLogger {

    private String filePath;

    public DishLogger(String filePath) {
        this.filePath = filePath;
    }

    public boolean dishExists(String dishName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("DISH:" + dishName.trim())) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public void displaySavedDish(String dishName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            boolean inDish = false;

            while ((line = reader.readLine()) != null) {
                if (line.equals("DISH:" + dishName.trim())) {
                    inDish = true;
                }
                if (inDish) {
                    System.out.println(line);
                }
                if (inDish && line.equals("END")) {
                    break;
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading saved dish: " + e.getMessage());
        }
    }

    public void saveDish(Dish dish) {
        ArrayList<String> allLines = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            boolean skipping = false;

            while ((line = reader.readLine()) != null) {
                if (line.equals("DISH:" + dish.dishName.trim())) {
                    skipping = true;
                }
                if (!skipping) {
                    allLines.add(line);
                }
                if (skipping && line.equals("END")) {
                    skipping = false;
                }
            }
            reader.close();
        } catch (IOException e) {
        }

        double[] macros = dish.calculateTotalMacros();

        allLines.add("DISH:" + dish.dishName);
        for (int i = 0; i < dish.ingredientCount; i++) {
            Ingredient ing = dish.ingredients[i];
            allLines.add("  - " + ing.name + " (" + ing.weightInGrams + "g)"
                    + " | Protein: " + String.format("%.2f", ing.protein) + "g"
                    + " | Carbs: " + String.format("%.2f", ing.carbohydrates) + "g"
                    + " | Fats: " + String.format("%.2f", ing.fats) + "g");
        }
        allLines.add("  TOTAL Protein: " + String.format("%.2f", macros[0]) + "g"
                + " | Carbs: " + String.format("%.2f", macros[1]) + "g"
                + " | Fats: " + String.format("%.2f", macros[2]) + "g");
        allLines.add("END");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false));
            for (String line : allLines) {
                writer.write(line);
                writer.newLine();
            }
            writer.close();
            System.out.println("Dish saved to log.");
        } catch (IOException e) {
            System.out.println("Error saving dish: " + e.getMessage());
        }
    }

    public String getLoggedDishesJSON() {
        StringBuilder json = new StringBuilder("[");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            boolean first = true;
            String currentDishName = null;
            double totalP = 0, totalC = 0, totalF = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("DISH:")) {
                    currentDishName = line.substring(5).trim();
                } else if (line.startsWith("TOTAL Protein:")) {
                    // Example: TOTAL Protein: 30.00g | Carbs: 60.00g | Fats: 15.00g
                    String[] parts = line.split("\\|");
                    totalP = Double.parseDouble(parts[0].split(":")[1].trim().replace("g", ""));
                    totalC = Double.parseDouble(parts[1].split(":")[1].trim().replace("g", ""));
                    totalF = Double.parseDouble(parts[2].split(":")[1].trim().replace("g", ""));
                } else if (line.equals("END")) {
                    if (currentDishName != null) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"name\":\"%s\",\"protein\":%.2f,\"carbs\":%.2f,\"fats\":%.2f}", 
                                    currentDishName, totalP, totalC, totalF));
                        first = false;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Error reading logged dishes: " + e.getMessage());
        }
        json.append("]");
        return json.toString();
    }
}
