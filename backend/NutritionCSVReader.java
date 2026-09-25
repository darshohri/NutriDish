import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NutritionCSVReader {

    private String csvFilePath;

    public NutritionCSVReader(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    public synchronized double[] findIngredient(String ingredientName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length < 4) continue; // skip malformed lines

                if (columns[0].trim().equalsIgnoreCase(ingredientName.trim())) {
                    double proteinPerGram = Double.parseDouble(columns[1].trim());
                    double carbsPerGram   = Double.parseDouble(columns[2].trim());
                    double fatsPerGram    = Double.parseDouble(columns[3].trim());
                    return new double[]{proteinPerGram, carbsPerGram, fatsPerGram};
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading CSV file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Error parsing nutrition data: " + e.getMessage());
        }

        return null;
    }

    public synchronized void addIngredientToCSV(String ingredientName, double proteinPerGram,
                                    double carbsPerGram, double fatsPerGram) {
        // Duplicate guard — don't add if already exists
        if (findIngredient(ingredientName) != null) {
            System.out.println("  " + ingredientName + " already exists in the database. Skipping.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath, true))) {
            writer.newLine();
            writer.write(ingredientName + "," + proteinPerGram + "," + carbsPerGram + "," + fatsPerGram);
            System.out.println("  " + ingredientName + " has been saved to the database.");
        } catch (IOException e) {
            System.out.println("Error writing to CSV file: " + e.getMessage());
        }
    }

    public synchronized List<String> searchIngredients(String query) {
        // Use LinkedHashSet to deduplicate while preserving insertion order
        Set<String> seen = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length < 1) continue;

                String name = columns[0].trim();
                if (!name.isEmpty() && name.toLowerCase().contains(query.toLowerCase())) {
                    seen.add(name); // Set automatically prevents duplicates
                }
            }
        } catch (IOException e) {
            System.out.println("Error searching CSV file: " + e.getMessage());
        }
        return new ArrayList<>(seen);
    }
}
