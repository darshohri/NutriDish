public class TestParse {
    public static void main(String[] args) {
        String json = "{\"name\":\"Test\",\"ingredients\":[{\"name\":\"Ing1\",\"weight\":100,\"proteinPerGram\":0.1,\"carbsPerGram\":0.1,\"fatsPerGram\":0.1,\"calculated\":{\"p\":10,\"c\":10,\"f\":10}},{\"name\":\"Ing2\",\"weight\":200,\"proteinPerGram\":0.2,\"carbsPerGram\":0.2,\"fatsPerGram\":0.2,\"calculated\":{\"p\":20,\"c\":20,\"f\":20}}],\"totals\":{\"protein\":30,\"carbs\":30,\"fats\":30}}";
        
        String ingredientsPart = json.split("\"ingredients\":\\[")[1];
        ingredientsPart = ingredientsPart.substring(0, ingredientsPart.indexOf("]"));
        
        System.out.println("ingredientsPart: " + ingredientsPart);
        String[] ingredientObjects = ingredientsPart.split("\\},\\{");
        System.out.println("Found " + ingredientObjects.length + " objects.");
        for (int i = 0; i < ingredientObjects.length; i++) {
            System.out.println("Obj " + i + ": " + ingredientObjects[i]);
        }
    }
}
