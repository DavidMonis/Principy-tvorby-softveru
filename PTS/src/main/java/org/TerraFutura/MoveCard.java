package org.TerraFutura;

import java.util.*;

// Try to move a card from a Pile to the Grid at the given position
// Returns true if the move succeeded else return false

public class MoveCard {

    public boolean moveCard(Pile pile, GridPosition gridCoordinate, Grid grid, int cardIndex) {

        // Validation
        if (pile == null || gridCoordinate == null || grid == null || cardIndex < 0 || cardIndex > 4) {
            return false;
        }
        if (!grid.canPutCard(gridCoordinate)) {
            return false;
        }

        // Get the card reference from the pile
        Optional<Card> card = pile.getCard(cardIndex);

        // Remove the card from the pile
        pile.takeCard(cardIndex);

        // Put the card on the grid. If there was no card place null
        grid.putCard(gridCoordinate, card.orElse(null));

        return true;
    }
}
