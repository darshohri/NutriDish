import java.util.ArrayList;

public class Dish {

    private String dishName;
    private ArrayList<Ingredient> ingredients;

    public Dish(String dishName) {
        this.dishName = dishName;
        this.ingredients = new ArrayList<>();
    }

    public String getDishName() {
        return dishName;
    }

    public ArrayList<Ingredient> getIngredients() {
        return ingredients;
    }

    public int getIngredientCount() {
        return ingredients.size();
    }

    public void addIngredient(Ingredient ingredient) {
        ingredients.add(ingredient);
    }

    public double[] calculateTotalMacros() {
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFats = 0;

        for (Ingredient ing : ingredients) {
            totalProtein += ing.getProtein();
            totalCarbs += ing.getCarbohydrates();
            totalFats += ing.getFats();
        }

        return new double[]{totalProtein, totalCarbs, totalFats};
    }

    public void displayDish() {
        System.out.println("\n=== Dish: " + dishName + " ===");
        System.out.println("Ingredients:");
        for (Ingredient ing : ingredients) {
            ing.display();
        }

        double[] macros = calculateTotalMacros();
        System.out.println("-------------------------------");
        System.out.println("  TOTAL Protein      : " + String.format("%.2f", macros[0]) + "g");
        System.out.println("  TOTAL Carbohydrates: " + String.format("%.2f", macros[1]) + "g");
        System.out.println("  TOTAL Fats         : " + String.format("%.2f", macros[2]) + "g");
        System.out.println("===============================");
    }
}
