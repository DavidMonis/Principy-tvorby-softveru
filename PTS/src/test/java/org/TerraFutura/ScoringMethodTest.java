import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.TerraFutura.*;

public class ScoringMethodTest {

    // Test 1: Check if passing null throws an exception
    @Test
    public void testNullInput() {
        try {
            ScoringMethod method = new ScoringMethod(null);
            fail("Should have thrown an exception for null input");
        } catch (Exception e) {
            // Exception caught, test passed
        }
    }

    // Test 2: Calculate basic points without any bonus combination
    @Test
    public void testBasicPoints() {
        List<Resource> emptyList = new ArrayList<>();
        Pair<List<Resource>, Integer> rule = new Pair<>(emptyList, 0);

        ScoringMethod method = new ScoringMethod(rule);

        // Add resources Green + Car (1 + 6)
        List<Resource> inventory = new ArrayList<>();
        inventory.add(Resource.Green);
        inventory.add(Resource.Car);

        method.setAllResources(inventory);
        method.selectThisMethodAndCalculate();

        // 1 + 6 = 7
        assertEquals(7, method.getTotal());
    }

    // Test 3: Calculate points with a bonus combination
    @Test
    public void testBonusPoints() {
        // Green resources give 10 bonus points
        List<Resource> required = new ArrayList<>();
        required.add(Resource.Green);
        required.add(Resource.Green);

        Pair<List<Resource>, Integer> rule = new Pair<>(required, 10);
        ScoringMethod method = new ScoringMethod(rule);

        // 2 Greens + 1 Red
        List<Resource> inventory = new ArrayList<>();
        inventory.add(Resource.Green); // 1 pt
        inventory.add(Resource.Green); // 1 pt
        inventory.add(Resource.Red);   // 1 pt

        method.setAllResources(inventory);
        method.selectThisMethodAndCalculate();

        // 3 + 10 = 13
        assertEquals(13, method.getTotal());
    }

    // Test 4: Verify that Pollution reduces the score
    @Test
    public void testPollutionMinusPoints() {
        List<Resource> emptyList = new ArrayList<>();
        Pair<List<Resource>, Integer> rule = new Pair<>(emptyList, 0);
        ScoringMethod method = new ScoringMethod(rule);

        // Inventory Car + Pollution (6 + (-1))
        List<Resource> inventory = new ArrayList<>();
        inventory.add(Resource.Car);
        inventory.add(Resource.Pollution);

        method.setAllResources(inventory);
        method.selectThisMethodAndCalculate();

        // 6 - 1 = 5
        assertEquals(5, method.getTotal());
    }

    // Test 5: Check correct string format output
    @Test
    public void testStateString() {
        List<Resource> list = new ArrayList<>();
        Pair<List<Resource>, Integer> rule = new Pair<>(list, 5);

        ScoringMethod method = new ScoringMethod(rule);

        method.selectThisMethodAndCalculate();

        String state = method.state();

        // Simple string check
        if (!state.contains("Total points = 0")) {
            fail("State string format is incorrect: " + state);
        }
    }
}