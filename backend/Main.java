import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        NutritionCSVReader csvReader = new NutritionCSVReader("nutrition.csv");
        DishLogger dishLogger = new DishLogger("dishes_log.txt");
        ArrayList<Dish> savedDishes = new ArrayList<>();
        String userChoice;

        System.out.println("========================================");
        System.out.println("       Welcome to NutriDish Simple      ");
        System.out.println("========================================");

        do {
            System.out.print("\nEnter dish name: ");
            String dishName = scanner.nextLine();

            boolean skipDish = false;

            if (dishLogger.dishExists(dishName)) {
                System.out.println("  '" + dishName + "' already exists in your log.");
                System.out.print("  Do you want to overwrite it? (yes/no): ");
                String overwrite = scanner.nextLine();

                if (!overwrite.equalsIgnoreCase("yes")) {
                    System.out.println("\nShowing saved dish:");
                    dishLogger.displaySavedDish(dishName);
                    skipDish = true;
                }
            }

            if (!skipDish) {
                Dish currentDish = new Dish(dishName);

                String addMore;
                do {
                    System.out.print("Enter ingredient name: ");
                    String ingredientName = scanner.nextLine();

                    double[] macrosPerGram = csvReader.findIngredient(ingredientName);

                    if (macrosPerGram != null) {
                        System.out.print("Enter weight in grams: ");
                        double weight = Double.parseDouble(scanner.nextLine());

                        Ingredient ingredient = new Ingredient(
                                ingredientName, weight,
                                macrosPerGram[0], macrosPerGram[1], macrosPerGram[2]
                        );

                        currentDish.addIngredient(ingredient);
                        System.out.println("  Added " + ingredientName + ".");

                    } else {
                        System.out.println("  '" + ingredientName + "' not found in database.");
                        System.out.println("  Please enter its per gram values:");

                        System.out.print("  Protein per gram: ");
                        double p = Double.parseDouble(scanner.nextLine());

                        System.out.print("  Carbohydrates per gram: ");
                        double c = Double.parseDouble(scanner.nextLine());

                        System.out.print("  Fats per gram: ");
                        double f = Double.parseDouble(scanner.nextLine());

                        csvReader.addIngredientToCSV(ingredientName, p, c, f);

                        System.out.print("Enter weight in grams: ");
                        double weight = Double.parseDouble(scanner.nextLine());

                        Ingredient ingredient = new Ingredient(ingredientName, weight, p, c, f);
                        currentDish.addIngredient(ingredient);
                        System.out.println("  Added " + ingredientName + ".");
                    }

                    System.out.print("Add another ingredient to this dish? (yes/no): ");
                    addMore = scanner.nextLine();

                } while (addMore.equalsIgnoreCase("yes"));

                currentDish.displayDish();
                savedDishes.add(currentDish);

                System.out.print("Do you want to save this dish to the log? (yes/no): ");
                String saveChoice = scanner.nextLine();
                if (saveChoice.equalsIgnoreCase("yes")) {
                    dishLogger.saveDish(currentDish);
                }
            }

            System.out.print("\nDo you want to add another dish? (yes/no): ");
            userChoice = scanner.nextLine();

        } while (userChoice.equalsIgnoreCase("yes"));

        System.out.println("\n\n========================================");
        System.out.println("         YOUR SESSION SUMMARY           ");
        System.out.println("========================================");

        for (int i = 0; i < savedDishes.size(); i++) {
            savedDishes.get(i).displayDish();
        }

        System.out.println("\nThank you for using NutriDish! Goodbye.");
        scanner.close();
    }
}
