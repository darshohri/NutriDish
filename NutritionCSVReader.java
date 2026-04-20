import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class NutritionCSVReader {

    private String csvFilePath;

    public NutritionCSVReader(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    public double[] findIngredient(String ingredientName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(csvFilePath));
            String line;

            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");

                if (columns[0].trim().equalsIgnoreCase(ingredientName.trim())) {
                    double proteinPerGram = Double.parseDouble(columns[1].trim());
                    double carbsPerGram   = Double.parseDouble(columns[2].trim());
                    double fatsPerGram    = Double.parseDouble(columns[3].trim());

                    reader.close();
                    return new double[]{proteinPerGram, carbsPerGram, fatsPerGram};
                }
            }

            reader.close();

        } catch (IOException e) {
            System.out.println("Error reading CSV file: " + e.getMessage());
        }

        return null;
    }

    public void addIngredientToCSV(String ingredientName, double proteinPerGram,
                                    double carbsPerGram, double fatsPerGram) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath, true));
            writer.newLine();
            writer.write(ingredientName + "," + proteinPerGram + "," + carbsPerGram + "," + fatsPerGram);
            writer.close();
            System.out.println("  " + ingredientName + " has been saved to the database.");
        } catch (IOException e) {
            System.out.println("Error writing to CSV file: " + e.getMessage());
        }
    }
}
