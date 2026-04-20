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
}
