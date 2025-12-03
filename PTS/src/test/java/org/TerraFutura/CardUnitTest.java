package org.TerraFutura;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Card class.
 */
public class CardUnitTest {

    @Test
    void canPutAndGetBasicResources() {
        Card card = new Card(null, null, 0);

        List<Resource> gained = List.of(Resource.Green, Resource.Red, Resource.Yellow);
        assertTrue(card.canPutResources(gained));

        card.putResources(gained);
        assertEquals(3, card.getResourcesSnapshot().size());
        assertTrue(card.getResourcesSnapshot().containsAll(gained));

        List<Resource> toTake = List.of(Resource.Green, Resource.Yellow);
        assertTrue(card.canGetResources(toTake));

        card.getResources(toTake);
        List<Resource> remaining = card.getResourcesSnapshot();
        assertEquals(1, remaining.size());
        assertEquals(Resource.Red, remaining.getFirst());
    }

    @Test
    void canGetResources_returnsFalseWhenBlocked() {
        // 1 safe pollution slot + 1 center
        Card card = new Card(null, null, 1);
        card.putResources(List.of(Resource.Green));

        // First pollution → safe
        card.putResources(List.of(Resource.Pollution));
        // Second pollution → center → blocks card
        card.putResources(List.of(Resource.Pollution));

        assertTrue(card.isBlockedByPollution());
        assertFalse(card.canGetResources(List.of(Resource.Green)));
    }

    @Test
    void canPutResources_respectsPollutionCapacityAndBlocksCard() {
        Card card = new Card(null, null, 1); // 1 safe pollution slot + 1 center

        // First pollution goes to the safe slot
        assertTrue(card.canPutResources(List.of(Resource.Pollution)));
        card.putResources(List.of(Resource.Pollution));

        assertEquals(1, card.getPollutionOnCard());
        assertFalse(card.isBlockedByPollution());

        // Second pollution goes to center and blocks the card
        assertTrue(card.canPutResources(List.of(Resource.Pollution)));
        card.putResources(List.of(Resource.Pollution));

        assertEquals(2, card.getPollutionOnCard());
        assertTrue(card.isBlockedByPollution());

        // No more pollution can be placed
        assertFalse(card.canPutResources(List.of(Resource.Pollution)));

        // Also cannot put non-pollution resources when blocked
        assertFalse(card.canPutResources(List.of(Resource.Green)));
    }

    @Test
    void canGetResources_doesNotAllowPayingWithPollution() {
        Card card = new Card(null, null, 0);
        card.putResources(List.of(Resource.Pollution));

        // Even if pollution is physically on the card, it cannot be requested as payment
        assertFalse(card.canGetResources(List.of(Resource.Pollution)));
    }

    @Test
    void getResources_throwsWhenNotEnoughResources() {
        Card card = new Card(null, null, 0);
        card.putResources(List.of(Resource.Green));

        assertFalse(card.canGetResources(List.of(Resource.Green, Resource.Red)));

        assertThrows(IllegalArgumentException.class,
                () -> card.getResources(List.of(Resource.Green, Resource.Red)));
    }

    @Test
    void check_delegatesToUpperEffect_usingComposite() {
        // Leaf effect: exactly one Green -> one Red, no pollution
        TransformationFixed leaf = new TransformationFixed(
                List.of(Resource.Green),
                List.of(Resource.Red),
                0
        );

        // Composite effect – EffectOr
        EffectOr composite = new EffectOr();
        composite.addEffect(leaf);

        // Card with composite as upperEffect
        Card card = new Card(composite, null, 0);

        // Valid transformation
        assertTrue(card.check(
                List.of(Resource.Green),
                List.of(Resource.Red),
                0));

        // Invalid transformation (wrong output)
        assertFalse(card.check(
                List.of(Resource.Green),
                List.of(Resource.Green),
                0));
    }

    @Test
    void checkLower_delegatesToLowerEffect_usingComposite() {
        // Leaf effect: one Money -> one Green, with 1 pollution
        TransformationFixed leaf = new TransformationFixed(
                List.of(Resource.Money),
                List.of(Resource.Green),
                1
        );

        // Composite for lower effect
        EffectOr composite = new EffectOr();
        composite.addEffect(leaf);

        // Card with composite as lowerEffect
        Card card = new Card(null, composite, 1);

        // Valid lower effect
        assertTrue(card.checkLower(
                List.of(Resource.Money),
                List.of(Resource.Green),
                1));

        // Invalid lower effect (missing Money, still pollution 1)
        assertFalse(card.checkLower(
                List.of(Resource.Green),
                List.of(Resource.Green),
                1));
    }
    @Test
    void card_withCompositeUpperAndLowerEffects_generatesAndTransformsCorrectly() {
        // Upper effect: generates one Red from nothing, no pollution
        EffectOr upperComposite = new EffectOr();
        upperComposite.addEffect(new TransformationFixed(
                List.of(),                      // no input
                List.of(Resource.Red),          // output Red
                0                               // no pollution
        ));

        // Lower effect 1: Money -> Red + 1 Pollution
        TransformationFixed lower1 = new TransformationFixed(
                List.of(Resource.Money),
                List.of(Resource.Red),
                1
        );

        // Lower effect 2: Red + Green -> Bulb
        TransformationFixed lower2 = new TransformationFixed(
                List.of(Resource.Red, Resource.Green),
                List.of(Resource.Bulb),
                0
        );

        // Lower effect 3: Bulb -> Gear
        TransformationFixed lower3 = new TransformationFixed(
                List.of(Resource.Bulb),
                List.of(Resource.Gear),
                0
        );

        EffectOr lowerComposite = new EffectOr();
        lowerComposite.addEffect(lower1);
        lowerComposite.addEffect(lower2);
        lowerComposite.addEffect(lower3);

        // Card with both upper and lower composites, initial resources: Red, Green, Money
        Card card = new Card(upperComposite, lowerComposite, 1);
        card.putResources(List.of(Resource.Red, Resource.Green, Resource.Money));

        // --- Upper effect: generate Red from nothing (no input, only output Red, no pollution) ---
        assertTrue(card.check(
                null,                          // no input
                List.of(Resource.Red),         // output Red
                0));

        // Upper effect should NOT match a pattern that belongs to lower effects
        assertFalse(card.check(
                List.of(Resource.Money),
                List.of(Resource.Red),
                1));

        // --- Lower effect 1: Money -> Red + 1 Pollution ---
        assertTrue(card.checkLower(
                List.of(Resource.Money),
                List.of(Resource.Red),
                1));

        // --- Lower effect 2: Red + Green -> Bulb ---
        assertTrue(card.checkLower(
                List.of(Resource.Red, Resource.Green),
                List.of(Resource.Bulb),
                0));

        // --- Lower effect 3: Bulb -> Gear ---
        assertTrue(card.checkLower(
                List.of(Resource.Bulb),
                List.of(Resource.Gear),
                0));

        // Invalid lower usage – no leaf matches this
        assertFalse(card.checkLower(
                List.of(Resource.Green),
                List.of(Resource.Gear),
                0));
    }

}
