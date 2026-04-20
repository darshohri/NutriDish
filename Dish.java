public class Dish {

    String dishName;
    Ingredient[] ingredients;
    int ingredientCount;

    public Dish(String dishName, int maxIngredients) {
        this.dishName = dishName;
        this.ingredients = new Ingredient[maxIngredients];
        this.ingredientCount = 0;
    }

    public void addIngredient(Ingredient ingredient) {
        if (ingredientCount < ingredients.length) {
            ingredients[ingredientCount] = ingredient;
            ingredientCount++;
        } else {
            System.out.println("Max ingredients reached for this dish.");
        }
    }

    public double[] calculateTotalMacros() {
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFats = 0;

        for (int i = 0; i < ingredientCount; i++) {
            totalProtein += ingredients[i].protein;
            totalCarbs += ingredients[i].carbohydrates;
            totalFats += ingredients[i].fats;
        }

        return new double[]{totalProtein, totalCarbs, totalFats};
    }

    public void displayDish() {
        System.out.println("\n=== Dish: " + dishName + " ===");
        System.out.println("Ingredients:");
        for (int i = 0; i < ingredientCount; i++) {
            ingredients[i].display();
        }

        double[] macros = calculateTotalMacros();
        System.out.println("-------------------------------");
        System.out.println("  TOTAL Protein      : " + String.format("%.2f", macros[0]) + "g");
        System.out.println("  TOTAL Carbohydrates: " + String.format("%.2f", macros[1]) + "g");
        System.out.println("  TOTAL Fats         : " + String.format("%.2f", macros[2]) + "g");
        System.out.println("===============================");
    }
}
