package org.TerraFutura;

import org.json.JSONObject;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

public class Game implements TerraFuturaInterface {
    private GameState state;
    private final Player[] players;
    private int onTurn;
    private int startingPlayer;
    private int turnNumber;

    private final Pile pileI;
    private final Pile pileII;
    private final MoveCard moveCard;
    private final ProcessAction processAction;
    private final GameObserver gameObserver;

    public Game(int numberOfPlayers,Pile pileI, Pile pileII, GameObserver gameObserver, List<Pair<ActivationPattern,ActivationPattern>> activationPatterns ,List<Pair<ScoringMethod,ScoringMethod>> methods) {
        if (numberOfPlayers < 2 || numberOfPlayers > 5) {
            throw new IllegalArgumentException("Number of players must be between 2 and 5");
        }

        this.players = new Player[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            if(activationPatterns.get(i) == null) {
                throw new IllegalArgumentException("Invalid activation pattern list");
            }
            if(methods.get(i) == null){
                throw new IllegalArgumentException("Invalid method list");
            }
            players[i] = new Player(i,new Grid(),activationPatterns.get(i),methods.get(i));
        }

        this.pileI = pileI;
        this.pileII = pileII;
        this.moveCard = new MoveCard();
        this.processAction = new ProcessAction();
        this.gameObserver = gameObserver;
        this.startingPlayer = 0;
        this.onTurn = 0;
        this.turnNumber = 0;
        this.state = GameState.TakeCardNoCardDiscarded;
    }

    public GameState getState() {
        return state;
    }

    public Player[] getPlayers() {
        return players.clone();
    }

    public int getOnTurn() {
        return onTurn;
    }

    public int getStartingPlayer() {
        return startingPlayer;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setStartingPlayer() {
        this.startingPlayer = 0;
        this.onTurn = startingPlayer;
        this.turnNumber = 0;
        this.state = GameState.TakeCardNoCardDiscarded;
        notifyObservers();
    }

    private String buildState(Player player) {
        JSONObject result = new JSONObject();

        result.put("state", state.toString());
        result.put("player", player.getId());
        result.put("onTurn", onTurn);
        result.put("turnNumber", turnNumber);
        result.put("myGrid", new JSONObject(player.getGrid().state()));


/*        JSONArray others = new JSONArray();
        for(Player otherPlayer : players) {
            if(otherPlayer.getId() != player.getId()) {
                JSONObject other = new JSONObject();
                other.put("playerId", otherPlayer.getId());
                other.put("gridOverview", new JSONObject(otherPlayer.getGrid().state()));
                others.put(other);
            }
        }
        result.put("otherPlayers", others);*/
        result.put("pileI", new JSONObject(pileI.state()));
        result.put("pileII", new JSONObject(pileII.state()));
        return result.toString();
    }

    private void notifyObservers(){
        Map<Integer,String> states= new HashMap<>();
        for(Player player : players) states.put(player.getId(),buildState(player));
        gameObserver.notifyAll(states);
    }

    private Pile getPile(Deck deck){
        if(deck == Deck.I) return pileI;
        else if(deck == Deck.II) return pileII;
        return null;
    }

    private boolean isValidPlayer(int playerId) {
        if ((playerId >= 0 && playerId < players.length) && playerId == onTurn && state != GameState.Finish){
            return true;
        }else{
            System.out.println("Player " + playerId + " is not in activation pattern");
            return false;
        }
    }

    @Override
    public boolean takeCard(int playerId, CardSource source, GridPosition destination) {
        if (!isValidPlayer(playerId)) {
            System.out.println("Player " + playerId + " cannot take card");
            return false;
        }

        if (state != GameState.TakeCardNoCardDiscarded &&
                state != GameState.TakeCardCardDiscarded) {
            System.out.println("Player " + playerId + " is not in correct state");
            return false;
        }
        Pile pile= getPile(source.getDeck());
        if (pile == null) {
            System.out.println("pile is null");
            return false;
        }

        Grid grid= players[playerId].getGrid();
        if(grid == null){
            System.out.println("grid is null");
            return false;
        }

        boolean result = moveCard.moveCard(pile,destination,grid, source.getIndex());

        if(result){
            state = GameState.ActivateCard;
            notifyObservers();
        }else{
            System.out.println("move card failed");
        }
        return result;
    }

    @Override
    public boolean discardLastCardFromDeck(int playerId, Deck deck) {
        if (!isValidPlayer(playerId)) {
            System.out.println("Player " + playerId + " cannot discard Last card from deck");
            return false;
        }

        if (state != GameState.TakeCardNoCardDiscarded) {
            System.out.println("Player " + playerId + " is not in correct state");
            return false;
        }

        Pile pile= getPile(deck);
        if(pile == null) {
            System.out.println("pile is null");
            return false;
        }

        try{
            pile.removeLastCard();
            state = GameState.TakeCardCardDiscarded;
            notifyObservers();
            return true;
        }catch (Exception e){
            System.out.println("discardLastCardFromDeck failed");
            return false;
        }
    }

    @Override
    public boolean activateCard(
            int playerId,
            GridPosition card,
            List<Pair<Resource, GridPosition>> inputs,
            List<Pair<Resource, GridPosition>> outputs,
            List<GridPosition> pollution,
            Optional<Integer> otherPlayerId,
            Optional<GridPosition> otherCard) {

        if (!isValidPlayer(playerId)) {
            System.out.println("Player " + playerId + " cannot activate card");
            return false;
        }
        if (state != GameState.ActivateCard) {
            System.out.println("Player " + playerId + " is not in correct state");
            return false;
        }
        if(players[playerId].getGrid() == null) {
            System.out.println("Grid is null");
            return false;
        }
        if(!players[playerId].getGrid().canBeActivated(card)){
            System.out.println("Grid can't be activated");
            return false;
        }

        Optional<Card> c = players[playerId].getGrid().getCard(card);
        if(!c.isPresent()) {
            System.out.println("Player " + playerId +  " cannot get card");
            return false;
        }
        boolean result =  processAction.activateCard(c.get(), players[playerId].getGrid(), inputs, outputs, pollution);

        //if we could activate that particular card, then we can set the card to activated,
        //so grid can return the canBeActivated method correctly
        if(result){
            players[playerId].getGrid().setActivated(card);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean turnFinished(int playerId) {
        if (!isValidPlayer(playerId)) {
            System.out.println("Player " + playerId + " cannot turn finished");
            return false;
        }
        Grid grid= players[playerId].getGrid();
        if(grid == null) {
            System.out.println("grid is null");
            return false;
        }
        grid.endTurn();
        if(onTurn == players.length-1) turnNumber++;
        onTurn = (onTurn + 1) % players.length;
        if(turnNumber == 9) {
            state = GameState.SelectActivationPattern;
        }
        else if(state == GameState.ActivateCard) {
            state = GameState.TakeCardNoCardDiscarded;
        }

        notifyObservers();
        return true;
    }

    @Override
    public boolean selectActivationPattern(int playerId, int card) {
        if (!isValidPlayer(playerId)) {
            System.out.println("Player " + playerId + " cannot select activation pattern");
            return false;
        }

        if (state != GameState.SelectActivationPattern) {
            System.out.println("Incorrect state");
            return false;
        }

        onTurn = (onTurn + 1) % players.length;

        if(card == 1) players[playerId].setActivationPattern(players[playerId].activationPatterns.getFirst());
        else if (card == 2)players[playerId].setActivationPattern(players[playerId].activationPatterns.getSecond());
        else {
            System.out.println("Incorrect card integer");
            return false;
        }

        //assign the activation pattern to the grid too, so grid.canBeActivated works in this state too
        players[playerId].getGrid().setActivationPattern(players[playerId].getActivationPattern().getPattern());

        return true;
    }

    public boolean activationPatternActivateCard(int playerId,
                                         GridPosition card,
                                         List<Pair<Resource, GridPosition>> inputs,
                                         List<Pair<Resource, GridPosition>> outputs,
                                         List<GridPosition> pollution,
                                         Optional<Integer> otherPlayerId,
                                         Optional<GridPosition> otherCard){
        ArrayList<SimpleEntry<Integer, Integer>> pattern = players[playerId].getActivationPattern().getPattern();
        SimpleEntry<Integer, Integer> position = new SimpleEntry<>(card.getX(), card.getY());
        if(!pattern.contains(position)) {
            System.out.println("Incorrect position, pattern not found in activation pattern");
            return false;
        }
        state = GameState.ActivateCard;
        pattern.remove(position);
        return activateCard(playerId, card, inputs, outputs, pollution, otherPlayerId, otherCard);
    }

    public boolean activationPatternTurnFinished(int playerId){
        if (!isValidPlayer(playerId)) {
            System.out.println("Player " + playerId + " cannot activate pattern turn finished");
            return false;
        }
        Grid grid= players[playerId].getGrid();
        if(grid == null) {
            System.out.println("grid is null");
            return false;
        }
        grid.endTurn();
        if(onTurn == players.length-1) state = GameState.SelectScoringMethod;
        onTurn = (onTurn + 1) % players.length;
        state = GameState.SelectActivationPattern;
        notifyObservers();
        return true;
    }

    @Override
    public boolean selectScoring(int playerId, int card) {
        if (!isValidPlayer(playerId)) {
            System.out.println("Player " + playerId + " cannot select Scoring method");
            return false;
        }

        if (state != GameState.SelectScoringMethod) {
            System.out.println("Incorrect state");
            return false;
        }

        onTurn = (onTurn + 1) % players.length;


        if (card == 1){
            players[playerId].setScoringMethod(players[playerId].methods.getFirst());
        }
        else if (card == 2){
            players[playerId].setScoringMethod(players[playerId].methods.getSecond());

        }
        else {
            System.out.println("Incorrect card integer");
            return false;
        }

        List<Resource> allResources = getAllResources(players[playerId]);
        ScoringMethod selected = players[playerId].getScoringMethod();
        selected.setAllResources(allResources);
        selected.selectThisMethodAndCalculate();
        System.out.println("For player: " + playerId + " " + selected.state());
        
        if (onTurn == players.length -1) {
            state = GameState.Finish;
            notifyObservers();
        }
        return true;
    }

    private List<Resource> getAllResources(Player player){
        List<Resource> result = new ArrayList<>();
        Grid grid = player.getGrid();
        for(GridPosition pos : GridPosition.values()){
            if(grid.getCard(pos).isPresent()){
                result.addAll(grid.getCard(pos).get().getResourceList());
            }
            else{continue;}
        }
        return result;
    };

}
