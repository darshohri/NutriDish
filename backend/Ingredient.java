public class Ingredient {

    private String name;
    private double weightInGrams;
    private double protein;
    private double carbohydrates;
    private double fats;

    public Ingredient(String name, double weightInGrams,
                      double proteinPerGram, double carbsPerGram, double fatsPerGram) {
        this.name = name;
        this.weightInGrams = weightInGrams;
        this.protein = proteinPerGram * weightInGrams;
        this.carbohydrates = carbsPerGram * weightInGrams;
        this.fats = fatsPerGram * weightInGrams;
    }

    public String getName() { return name; }
    public double getWeightInGrams() { return weightInGrams; }
    public double getProtein() { return protein; }
    public double getCarbohydrates() { return carbohydrates; }
    public double getFats() { return fats; }

    public void display() {
        System.out.println("  - " + name + " (" + weightInGrams + "g)"
                + " | Protein: " + String.format("%.2f", protein) + "g"
                + " | Carbs: " + String.format("%.2f", carbohydrates) + "g"
                + " | Fats: " + String.format("%.2f", fats) + "g");
    }
}
