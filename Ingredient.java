public class Ingredient {

    String name;
    double weightInGrams;
    double protein;
    double carbohydrates;
    double fats;

    public Ingredient(String name, double weightInGrams,
                      double proteinPerGram, double carbsPerGram, double fatsPerGram) {
        this.name = name;
        this.weightInGrams = weightInGrams;
        this.protein = proteinPerGram * weightInGrams;
        this.carbohydrates = carbsPerGram * weightInGrams;
        this.fats = fatsPerGram * weightInGrams;
    }

    public void display() {
        System.out.println("  - " + name + " (" + weightInGrams + "g)"
                + " | Protein: " + String.format("%.2f", protein) + "g"
                + " | Carbs: " + String.format("%.2f", carbohydrates) + "g"
                + " | Fats: " + String.format("%.2f", fats) + "g");
    }
}
