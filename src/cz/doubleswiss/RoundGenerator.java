package cz.doubleswiss;

import java.util.*;
import java.util.stream.Collectors;

public abstract class RoundGenerator {
    public static List<Game> generateRound(Tournament tournament) {
        Ranking ranking = RankingGenerator.createRanking(tournament);
        Map<String, PlayerInfo> playerInfoMap = new HashMap<>();
        for (String playerKey : tournament.players.keySet()) {
            Map<String, Integer> alliesCount = getAlliesCount(tournament, playerKey);
            Map<String, Integer> enemiesCount = getEnemiesCount(tournament, playerKey);
            Integer byeCount = getByeCount(tournament, playerKey);
            Standing standing = getStanding(ranking, playerKey);
            playerInfoMap.put(playerKey, new PlayerInfo(alliesCount, enemiesCount, byeCount, standing));
        }

        Long startAllRounds = System.currentTimeMillis();
        System.out.println("Started generation of possible Rounds");
//        List<List<Game>> allRounds = getAllRoundsOptimized(new ArrayList<>(tournament.players.keySet()), null, new ArrayList<>());
        List<List<Game>> allRounds = tournament.rounds.size() == 0 ?
                getFirstRoundOf(tournament) :
                tournament.players.size()>13 ?
                        geRoundSimple(tournament) :
                        getAllRoundsOptimized(new ArrayList<>(tournament.players.keySet()), getMostFrequentAllies(tournament), getPlayersWithExtremalByes(tournament, true));

//        List<List<Game>> allRounds = getAllRounds(new ArrayList<>(tournament.players.keySet()));
        Long endAllRounds = System.currentTimeMillis();
        System.out.println("Generation of possible Rounds took " + (endAllRounds - startAllRounds) + " ms.");
        System.out.println("Evaluation of Rounds started.");
        List<Game> bestRound = null;
        Integer bestScore = Integer.MIN_VALUE;
        for (List<Game> round : allRounds) {
            Integer score = evaluateRound(round, tournament, playerInfoMap);
            if (score > bestScore) {
                bestScore = score;
                bestRound = round;
            }
        }
        Long endEvaluation = System.currentTimeMillis();
        System.out.println("Evaluation of Rounds took " + (endEvaluation - endAllRounds) + " ms.");

        return bestRound;
    }

    private static List<List<Game>> geRoundSimple(Tournament tournament) {
        List<Game> roundToReturn = new ArrayList<>();
        Ranking ranking = RankingGenerator.createRanking(tournament);
        List<Integer> ranks = ranking.ranking;
        List<Player> players = ranking.players;
//        List<String> sortedPlayers = ranking.players.stream().sorted((a,b)->ranking.ranking.get(ranking.players.indexOf(a)).compareTo(ranking.ranking.get(ranking.players.indexOf(b)))).collect(Collectors.toList()).stream().map(a->a.name).collect(Collectors.toList());
//        List<String> sortedPlayers = ranking.players.stream().sorted((a,b)->ranking.ranking.get(ranking.players.indexOf(a)).compareTo(ranking.ranking.get(ranking.players.indexOf(b)))).map(a->a.name).collect(Collectors.toList());
//        Collections.reverse(sortedPlayers);
        List<String> candidatesToBeByed = getPlayersWithExtremalByes(tournament, false);
        Integer byeCountThisRound = tournament.players.size() % 4;
        List<String> forceByesThisRound = new ArrayList<>();
        if(candidatesToBeByed.size() > byeCountThisRound){
            for(int i = 1; (i<=ranks.size()) && (forceByesThisRound.size()<byeCountThisRound); i++){
                if(candidatesToBeByed.contains(players.get(ranks.get(ranks.size() - i)).name)){
                    forceByesThisRound.add(players.get(ranks.get(ranks.size() - i)).name);
                }
            }
        }
        else{
            forceByesThisRound = candidatesToBeByed;
        }
        List<String> playersSorted = new ArrayList<>();
        for(int i = 0; i<ranks.size(); i++){
            if(!forceByesThisRound.contains(players.get(ranks.get(i)).name)){
                playersSorted.add(players.get(ranks.get(i)).name);
            }
        }
        Integer pivot = 0;
        while(pivot + 3 < playersSorted.size()){
            roundToReturn.add(
                    new Game(
                            playersSorted.get(pivot),
                            playersSorted.get(pivot+3),
                            playersSorted.get(pivot+1),
                            playersSorted.get(pivot+2)
                    ));
            pivot = pivot + 4;
        }
        return Arrays.asList(roundToReturn);
    }

    private static List<List<Game>> getFirstRoundOf(Tournament tournament) {
        List<Game> roundToReturn = new ArrayList<>();
        Set<String> playerNames = tournament.players.keySet();
        List<String> sortedPlayers = playerNames.stream().sorted((a,b)->{
            Player aa = tournament.players.get(a);
            Player bb = tournament.players.get(b);
            if(aa.bonus.equals(bb.bonus)){
                return bb.preGameValue.compareTo(aa.preGameValue);
            }
            else{
                return bb.bonus.compareTo(aa.bonus);
            }
        }).collect(Collectors.toList());
        Integer pivot = 0;
        while(pivot + 3 < sortedPlayers.size()){
            roundToReturn.add(new Game(sortedPlayers.get(pivot),sortedPlayers.get(pivot+3),sortedPlayers.get(pivot+1),sortedPlayers.get(pivot+2)));
            pivot = pivot + 4;
        }
        return Arrays.asList(roundToReturn);
    }

    private static List<List<String>> getMostFrequentAllies(Tournament tournament) {
        if (tournament.rounds.size() == 0) {
            System.out.println("Not performing ally optimization due to this being the first round");
            return null;
        }
        System.out.println("Performing ally optimization...");
        Set<String> players = tournament.players.keySet();
        Map<String, Map<String, Integer>> allyCount = new LinkedHashMap<>();
        Integer mostFrequentAlliesOccurrences = 0;
        for (String player : players) {
            allyCount.put(player, getAlliesCount(tournament, player));
            mostFrequentAlliesOccurrences = Math.max(mostFrequentAlliesOccurrences, getAlliesCount(tournament, player).values().stream().reduce((a, b) -> Math.max(a, b)).orElse(null));
        }
        if (mostFrequentAlliesOccurrences == 0) {
            throw new RuntimeException("Something is fishy here");
        }
        List<List<String>> retVal = new ArrayList<>();
        for (String player1 : players) {
            for (String player2 : players){
                if (!player1.equals(player2)){
                    if(allyCount.get(player1).get(player2) == mostFrequentAlliesOccurrences){
                        retVal.add(Arrays.asList(player1, player2));
                    }
                }
            }
        }
        return retVal;
    }

    private static List<String> getPlayersWithExtremalByes(Tournament tournament, Boolean extremumTypeIsMaximum) {
        if (tournament.rounds.size() == 0) {
            System.out.println("Not performing bye optimization due to this being the first round");
            return new ArrayList<>();
        }
//        if (tournament.players.size() % 2 == 0) {
//            System.out.println("Not performing bye optimization due to even number of players");
//            return new ArrayList<>();
//        }
        System.out.println("Performing bye optimization...");
        Set<String> players = tournament.players.keySet();
        Map<String, Integer> byes = new LinkedHashMap<>();
        for (String player : players) {
            byes.put(player, getByeCount(tournament, player));
        }
        Integer extremeByes = byes.values().stream().reduce((a, b) -> extremumTypeIsMaximum ? Math.max(a, b) : Math.min(a, b)).orElse(null);
        List<String> retVal = new ArrayList<>();
        for (String player : players) {
            if (extremeByes == byes.get(player)) {
                retVal.add(player);
            }
        }
        return retVal;
    }

    private static Standing getStanding(Ranking ranking, String playerKey) {
        return new Standing(
                ranking.points.get(getIndexOfPlayerByName(ranking.players, playerKey)),
                ranking.scores.get(getIndexOfPlayerByName(ranking.players, playerKey)),
                ranking.sonneborn.get(getIndexOfPlayerByName(ranking.players, playerKey)),
                ranking.buchholz.get(getIndexOfPlayerByName(ranking.players, playerKey))
        );
    }

    private static Integer getIndexOfPlayerByName(List<Player> playerList, String playerName) {
        List<Player> copy = new ArrayList<>(playerList);
        copy.removeIf((player) -> !player.name.equals(playerName));
        return playerList.indexOf(copy.get(0));
    }

    private static Integer getByeCount(Tournament tournament, String playerKey) {
        Integer byeCount = 0;
        for (List<Game> round : tournament.rounds) {
            Boolean found = false;

            for (Game game : round) {
                Boolean isInGame = false;
                if (game.teams.get(0).get(0).equals(playerKey)) {
                    isInGame = true;
                }
                if (game.teams.get(0).get(1).equals(playerKey)) {
                    isInGame = true;
                }
                if (game.teams.get(1).get(0).equals(playerKey)) {
                    isInGame = true;
                }
                if (game.teams.get(1).get(1).equals(playerKey)) {
                    isInGame = true;
                }
                if (isInGame) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                byeCount++;
            }
        }
        return byeCount;
    }

    private static Map<String, Integer> getAlliesCount(Tournament tournament, String playerKey) {
        Map<String, Integer> count = new HashMap<>();

        for (String playerName : tournament.players.keySet()) {
            count.put(playerName, 0);
        }

        for (List<Game> round : tournament.rounds) {
            for (Game game : round) {
                if (game.teams.get(0).get(0).equals(playerKey)) {
                    count.put(game.teams.get(0).get(1), count.get(game.teams.get(0).get(1)) + 1);
                }
                if (game.teams.get(0).get(1).equals(playerKey)) {
                    count.put(game.teams.get(0).get(0), count.get(game.teams.get(0).get(0)) + 1);
                }
                if (game.teams.get(1).get(0).equals(playerKey)) {
                    count.put(game.teams.get(1).get(1), count.get(game.teams.get(1).get(1)) + 1);
                }
                if (game.teams.get(1).get(1).equals(playerKey)) {
                    count.put(game.teams.get(1).get(0), count.get(game.teams.get(1).get(0)) + 1);
                }
            }
        }

        return count;
    }

    private static Map<String, Integer> getEnemiesCount(Tournament tournament, String playerKey) {
        Map<String, Integer> count = new HashMap<>();

        for (String playerName : tournament.players.keySet()) {
            count.put(playerName, 0);
        }

        for (List<Game> round : tournament.rounds) {
            for (Game game : round) {
                if (game.teams.get(0).get(0).equals(playerKey) || game.teams.get(0).get(1).equals(playerKey)) {
                    count.put(game.teams.get(1).get(0), count.get(game.teams.get(1).get(0)) + 1);
                    count.put(game.teams.get(1).get(1), count.get(game.teams.get(1).get(1)) + 1);
                }
                if (game.teams.get(1).get(0).equals(playerKey) || game.teams.get(1).get(1).equals(playerKey)) {
                    count.put(game.teams.get(0).get(0), count.get(game.teams.get(0).get(0)) + 1);
                    count.put(game.teams.get(0).get(1), count.get(game.teams.get(0).get(1)) + 1);
                }
            }
        }

        return count;
    }

    private static List<List<Game>> getAllRoundsOptimized(List<String> playerNames, List<List<String>> mostFrequentAllies, List<String> playersWithMostByes) {
//        gets all possible team divisions
        List<List<List<String>>> allPlayerPairsCombination = getAllPairsCombinations(playerNames);
        List<List<Game>> rounds = new ArrayList<>();
        for (List<List<String>> pairings : allPlayerPairsCombination) {

//          ignores those that would let byes unpaired
            if (!playersWithMostByes.isEmpty()) {
                Boolean allByesPaired = true;
                for (String bye : playersWithMostByes) {
                    Boolean byePaired = false;
                    for (List<String> pair : pairings) {
                        if (pair.contains(bye)) {
                            byePaired = true;
                            break;
                        }
                    }
                    if (!byePaired) {
                        allByesPaired = false;
                        break;
                    }
                }
                if (!allByesPaired) {
                    continue;
                }
            }

//            ignores pairings with the most frequent allies paired again
            if(mostFrequentAllies != null && !mostFrequentAllies.isEmpty()){
//                checks pairing for the most frequent allies
                Boolean foundFrequentAllies = false;
                for(List<String> pair : pairings){
                    for(List<String> frequentAllies : mostFrequentAllies){
                        if(pair.get(0).equals(frequentAllies.get(0)) && pair.get(1).equals(frequentAllies.get(1))){
                            foundFrequentAllies = true;
                            break;
                        }
                    }
                    if(foundFrequentAllies){
                        break;
                    }
                }
                if(foundFrequentAllies){
                    continue;
                }
            }

//            continues proper computation
            List<List<List<List<String>>>> gamesCombinations = getAllPairsCombinations(pairings);
            for (List<List<List<String>>> gameCombination : gamesCombinations) {
                List<Game> round = new ArrayList<>();
                for (List<List<String>> gameEmbryo : gameCombination) {
                    Game game = new Game(
                            gameEmbryo.get(0).get(0),
                            gameEmbryo.get(0).get(1),
                            gameEmbryo.get(1).get(0),
                            gameEmbryo.get(1).get(1)
                    );
                    round.add(game);
                }
                rounds.add(round);
            }
        }
        if(rounds.isEmpty()){
//            the "most ally optimization" is a heuristics. If it fails, let us acquire rounds "normally"
            if(mostFrequentAllies != null){
                return getAllRoundsOptimized(playerNames, null, playersWithMostByes);
            }
            else{
                throw new RuntimeException("Round Generation failed");
//                if this happens, use following commented line
//                return getAllRounds();
            }
        }
        else {
//            expected outcome
            return rounds;
        }
    }

    private static List<List<Game>> getAllRounds(List<String> playerNames) {
        List<List<List<String>>> allPlayerPairsCombination = getAllPairsCombinations(playerNames);
        List<List<Game>> rounds = new ArrayList<>();
        for (List<List<String>> pairings : allPlayerPairsCombination) {
            List<List<List<List<String>>>> gamesCombinations = getAllPairsCombinations(pairings);
            for (List<List<List<String>>> gameCombination : gamesCombinations) {
                List<Game> round = new ArrayList<>();
                for (List<List<String>> gameEmbryo : gameCombination) {
                    Game game = new Game(
                            gameEmbryo.get(0).get(0),
                            gameEmbryo.get(0).get(1),
                            gameEmbryo.get(1).get(0),
                            gameEmbryo.get(1).get(1)
                    );
                    round.add(game);
                }
                rounds.add(round);
            }
        }
        return rounds;
    }

    private static Integer evaluateRound(List<Game> round, Tournament tournament, Map<String, PlayerInfo> playerInfoMap) {
        final Integer POINTS_PER_BYE_SQUARED = -1000000;
        final Integer POINTS_PER_ALLY_SQUARED = -100000;
        final Integer POINTS_PER_BYE_POINT = -10000;
        final Integer POINTS_PER_TEAM_POINTS_DIFFERENCE_SQ = -1000;
        final Integer POINTS_PER_ENEMY_SQUARED = -100;
        final Integer POINTS_PER_TEAM_POINTS_VARIANCE_DIFFERENCE_SQ = -10;
        final Integer POINTS_PER_PREGAME_VALUE_TEAM_DIFFERENCE_SQ = -1;

        Integer roundFitness = 0;

        roundFitness += getPlayerEncounterFitnessComponent(round, playerInfoMap, POINTS_PER_ALLY_SQUARED, POINTS_PER_ENEMY_SQUARED);

        roundFitness += getByeCountFitnessComponent(round, POINTS_PER_BYE_SQUARED, playerInfoMap);

        roundFitness += getGamePointsFitnessComponent(tournament, playerInfoMap, round,
                POINTS_PER_TEAM_POINTS_DIFFERENCE_SQ,
                POINTS_PER_TEAM_POINTS_VARIANCE_DIFFERENCE_SQ,
                POINTS_PER_PREGAME_VALUE_TEAM_DIFFERENCE_SQ
        );

        roundFitness += getByePointsComponent(playerInfoMap, round, POINTS_PER_BYE_POINT);

        return roundFitness;
    }

    private static Integer getByePointsComponent(Map<String, PlayerInfo> playerInfoMap, List<Game> round, Integer POINTS_PER_BYE_POINT) {
        Set<String> byePlayers = new HashSet(Arrays.asList(playerInfoMap.keySet().toArray()));

        for (Game game : round) {
            byePlayers.remove(game.teams.get(0).get(0));
            byePlayers.remove(game.teams.get(0).get(1));
            byePlayers.remove(game.teams.get(1).get(0));
            byePlayers.remove(game.teams.get(1).get(1));
        }

        Integer byePoints = 0;

        for (String byePlayer : byePlayers) {
            byePoints += playerInfoMap.get(byePlayer).standing.points;
        }

        return POINTS_PER_BYE_POINT * byePoints;
    }

    private static Integer getPlayerEncounterFitnessComponent(List<Game> round, Map<String, PlayerInfo> playerInfoMap, Integer points_per_ally_squared, Integer points_per_enemy_squared) {

        Integer totalSquaredAllyCount = 0;
        Integer totalSquaredEnemyCount = 0;

        Map<String, Map<String, Integer>> roundAllyMap = new HashMap<>();
        Map<String, Map<String, Integer>> roundEnemyMap = new HashMap<>();

        for (Game game : round) {
            String p11 = game.teams.get(0).get(0);
            String p12 = game.teams.get(0).get(1);
            String p21 = game.teams.get(1).get(0);
            String p22 = game.teams.get(1).get(1);

            roundAllyMap.put(p11, new HashMap<>());
            roundAllyMap.put(p12, new HashMap<>());
            roundAllyMap.put(p21, new HashMap<>());
            roundAllyMap.put(p22, new HashMap<>());

            roundEnemyMap.put(p11, new HashMap<>());
            roundEnemyMap.put(p12, new HashMap<>());
            roundEnemyMap.put(p21, new HashMap<>());
            roundEnemyMap.put(p22, new HashMap<>());

            roundAllyMap.get(p11).put(p12, 1);
            roundAllyMap.get(p12).put(p11, 1);
            roundAllyMap.get(p21).put(p22, 1);
            roundAllyMap.get(p22).put(p21, 1);

            roundEnemyMap.get(p11).put(p21, 1);
            roundEnemyMap.get(p11).put(p22, 1);
            roundEnemyMap.get(p12).put(p21, 1);
            roundEnemyMap.get(p12).put(p22, 1);

            roundEnemyMap.get(p21).put(p11, 1);
            roundEnemyMap.get(p22).put(p11, 1);
            roundEnemyMap.get(p21).put(p12, 1);
            roundEnemyMap.get(p22).put(p12, 1);
        }

        for (String playerKey1 : playerInfoMap.keySet()) {
            if (!roundAllyMap.containsKey(playerKey1)) {
                roundAllyMap.put(playerKey1, new HashMap<>());
            }
            if (!roundEnemyMap.containsKey(playerKey1)) {
                roundEnemyMap.put(playerKey1, new HashMap<>());
            }
            for (String playerKey2 : playerInfoMap.keySet()) {
                if (!roundAllyMap.get(playerKey1).containsKey(playerKey2)) {
                    roundAllyMap.get(playerKey1).put(playerKey2, 0);
                }
                if (!roundEnemyMap.get(playerKey1).containsKey(playerKey2)) {
                    roundEnemyMap.get(playerKey1).put(playerKey2, 0);
                }
                Integer allyCount = playerInfoMap.get(playerKey1).alliesCount.get(playerKey2) + roundAllyMap.get(playerKey1).get(playerKey2);
                Integer enemyCount = playerInfoMap.get(playerKey1).enemiesCount.get(playerKey2) + roundAllyMap.get(playerKey1).get(playerKey2);

                totalSquaredAllyCount += (allyCount * allyCount);
                totalSquaredEnemyCount += (enemyCount * enemyCount);
            }
        }

        return totalSquaredAllyCount * points_per_ally_squared + totalSquaredEnemyCount * points_per_enemy_squared;
    }

    private static Integer getByeCountFitnessComponent(
            List<Game> round,
            Integer points_per_bye_squared,
            Map<String, PlayerInfo> playerInfoMap) {
        Integer fitnessComponent = 0;

        Set<String> byePlayers = new HashSet(Arrays.asList(playerInfoMap.keySet().toArray()));

        for (Game game : round) {
            byePlayers.remove(game.teams.get(0).get(0));
            byePlayers.remove(game.teams.get(0).get(1));
            byePlayers.remove(game.teams.get(1).get(0));
            byePlayers.remove(game.teams.get(1).get(1));
        }

        for (String playerKey : playerInfoMap.keySet()) {
            Integer byeCount = playerInfoMap.get(playerKey).byeCount + (byePlayers.contains(playerKey) ? 1 : 0);
            fitnessComponent += (points_per_bye_squared * byeCount * byeCount);
        }

        return fitnessComponent;
    }

    private static Integer getGamePointsFitnessComponent(
            Tournament tournament,
            Map<String, PlayerInfo> playerInfoMap,
            List<Game> round,
            Integer POINTS_PER_TEAM_POINTS_DIFFERENCE_SQ,
            Integer POINTS_PER_TEAM_POINTS_VARIANCE_DIFFERENCE_SQ,
            Integer POINTS_PER_PREGAME_VALUE_TEAM_DIFFERENCE_SQ
    ) {
        Integer fitnessComponent = 0;
        Integer totalSquaredTeamPointsDifference = 0;
        Integer totalSquaredTeamPointsVarianceDifference = 0;
        Integer totalSquaredTeamPregameValueDifference = 0;

        for (Game game : round) {
            Integer points11 = playerInfoMap.get(game.teams.get(0).get(0)).standing.points;
            Integer points12 = playerInfoMap.get(game.teams.get(0).get(1)).standing.points;
            Integer points21 = playerInfoMap.get(game.teams.get(1).get(0)).standing.points;
            Integer points22 = playerInfoMap.get(game.teams.get(1).get(1)).standing.points;

            Integer pre11 = tournament.players.get(game.teams.get(0).get(0)).preGameValue;
            Integer pre12 = tournament.players.get(game.teams.get(0).get(1)).preGameValue;
            Integer pre21 = tournament.players.get(game.teams.get(1).get(0)).preGameValue;
            Integer pre22 = tournament.players.get(game.teams.get(1).get(1)).preGameValue;

            Integer teamPointsDifference = points11 + points12 - (points21 + points22);
            Integer teamPointsVarianceDifference = Math.abs(points11 - points12) - Math.abs(points21 - points22);
            Integer teamPregameValueDifference = Math.abs(pre11 - pre12) - Math.abs(pre21 - pre22);

            totalSquaredTeamPointsDifference += teamPointsDifference * teamPointsDifference;
            totalSquaredTeamPointsVarianceDifference += teamPointsVarianceDifference * teamPointsVarianceDifference;
            totalSquaredTeamPregameValueDifference += teamPregameValueDifference * teamPregameValueDifference;
        }
        fitnessComponent += totalSquaredTeamPointsDifference * POINTS_PER_TEAM_POINTS_DIFFERENCE_SQ;
        fitnessComponent += totalSquaredTeamPointsVarianceDifference * POINTS_PER_TEAM_POINTS_VARIANCE_DIFFERENCE_SQ;
        fitnessComponent += totalSquaredTeamPregameValueDifference * POINTS_PER_PREGAME_VALUE_TEAM_DIFFERENCE_SQ;

        return fitnessComponent;
    }

    static <T> List<List<List<T>>> getAllPairsCombinations(List<? extends T> elements) {
        if (elements.size() < 2) {
            return new ArrayList<>();
        }
        if (elements.size() == 2) {
            List<List<T>> pairCombination = new ArrayList();
            pairCombination.add(Arrays.asList(elements.get(0), elements.get(1)));
            List<List<List<T>>> combinations = new ArrayList();
            combinations.add(pairCombination);
            return combinations;
        }
        if (elements.size() == 3) {
            List<List<List<T>>> combinations = new ArrayList();
            List<T> pair0 = Arrays.asList(elements.get(0), elements.get(1));
            List<T> pair1 = Arrays.asList(elements.get(0), elements.get(2));
            List<T> pair2 = Arrays.asList(elements.get(2), elements.get(1));

            List<List<T>> pairCombination0 = new ArrayList<>();
            List<List<T>> pairCombination1 = new ArrayList<>();
            List<List<T>> pairCombination2 = new ArrayList<>();

            pairCombination0.add(pair0);
            pairCombination1.add(pair1);
            pairCombination2.add(pair2);

            combinations.add(pairCombination0);
            combinations.add(pairCombination1);
            combinations.add(pairCombination2);
            return combinations;
        }
        List<List<List<T>>> retVal = new ArrayList<>();
//        for (int i = 0; i < elements.size() - 1; i++) {
        for (int j = 1; j < elements.size(); j++) {
            List<T> remainingElements = new ArrayList<>();
            remainingElements.addAll(elements);
            remainingElements.remove(elements.get(0));
            remainingElements.remove(elements.get(j));
            List<List<List<T>>> pairsFromRemainingElements = getAllPairsCombinations(remainingElements);
            List<T> initialPair = Arrays.asList(elements.get(0), elements.get(j));
            for (List<List<T>> combination : pairsFromRemainingElements) {
                combination.add(initialPair);
                retVal.add(combination);
            }
        }
        if (elements.size() % 2 == 1) {
            List<T> remainingElements = new ArrayList<>();
            remainingElements.addAll(elements);
            remainingElements.remove(elements.get(0));
            List<List<List<T>>> pairsFromRemainingElements = getAllPairsCombinations(remainingElements);
            for (List<List<T>> combination : pairsFromRemainingElements) {
                retVal.add(combination);
            }
        }
//            }
//        }
        return retVal;
    }
}

class PlayerInfo {
    Map<String, Integer> alliesCount;
    Map<String, Integer> enemiesCount;
    Integer byeCount;
    Standing standing;

    PlayerInfo(Map<String, Integer> alliesCount, Map<String, Integer> enemiesCount, Integer byeCount, Standing standing) {
        this.alliesCount = alliesCount;
        this.enemiesCount = enemiesCount;
        this.byeCount = byeCount;
        this.standing = standing;
    }
}
