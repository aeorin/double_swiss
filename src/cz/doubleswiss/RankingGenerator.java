package cz.doubleswiss;

import java.util.*;

public abstract class RankingGenerator {
    static Ranking createRanking(Tournament tournament){

//        standing after *current* round
        String rankingName = "Poradi po "+String.valueOf(tournament.rounds.size())+". kole";
        Map<String, Integer> pointsMap = getPoints(tournament);
        Map<String, Integer> scores = getScores(tournament);
        Map<String, Integer> buchholzs = getBuchholz(tournament, pointsMap);
        Map<String, Integer> sonneborns = getSonnenborn(tournament, pointsMap);

        List<Player> players = new ArrayList<>(tournament.players.values());
        List<Integer> points = new ArrayList<>();
        List<Integer> score = new ArrayList<>();
        List<Integer> sonneborn = new ArrayList<>();
        List<Integer> buchholz = new ArrayList<>();

        for(int i = 0; i<players.size(); i++){
            points.add(pointsMap.get(players.get(i).name));
            score.add(scores.get(players.get(i).name));
            sonneborn.add(sonneborns.get(players.get(i).name));
            buchholz.add(buchholzs.get(players.get(i).name));
        }

        return new Ranking(rankingName, players, points, score, sonneborn, buchholz);
    }

    private static Map<String, Integer> getPoints(Tournament tournament) {
        Map<String, Integer> points = new HashMap<>();

        for(String playerName : tournament.players.keySet()){
            points.put(playerName, 0);
        }

        for(List<Game> round : tournament.rounds){
            List<String> byePlayers = new ArrayList<>();
            byePlayers.addAll(points.keySet());
            for(Game game : round){
                Integer score = null;
                if(game.overtime){
                    if(game.result.get(0) == game.result.get(1)){score = 2;}
                    if(game.result.get(0) > game.result.get(1)){score = 3;}
                    if(game.result.get(0) < game.result.get(1)){score = 1;}
                }
                else{
                    if(game.result.get(0) == game.result.get(1)){throw new RuntimeException("INVALID ROUND RESULT");}
                    if(game.result.get(0) > game.result.get(1)){score = 4;}
                    if(game.result.get(0) < game.result.get(1)){score = 0;}
                }
                byePlayers.remove(game.teams.get(0).get(0));
                byePlayers.remove(game.teams.get(0).get(1));
                byePlayers.remove(game.teams.get(1).get(0));
                byePlayers.remove(game.teams.get(1).get(1));
                points.put(game.teams.get(0).get(0), points.get(game.teams.get(0).get(0))+score);
                points.put(game.teams.get(0).get(1), points.get(game.teams.get(0).get(1))+score);
                points.put(game.teams.get(1).get(0), points.get(game.teams.get(1).get(0))+4-score);
                points.put(game.teams.get(1).get(1), points.get(game.teams.get(1).get(1))+4-score);
            }
            for(String playerName: byePlayers){
                points.put(playerName, points.get(playerName)+4);
            }
        }

        for(Player player : tournament.players.values()){
            points.put(player.name, points.get(player.name)+player.bonus);
        }

        return points;
    }

    private static Map<String, Integer> getScores(Tournament tournament) {
        Map<String, Integer> retVal = new HashMap<>();

        for(String playerName : tournament.players.keySet()){
            retVal.put(playerName, 0);
        }

        for(List<Game> round : tournament.rounds){
            for(Game game : round){
                Integer score = game.result.get(0) - game.result.get(1);
                retVal.put(game.teams.get(0).get(0), retVal.get(game.teams.get(0).get(0))+score);
                retVal.put(game.teams.get(0).get(1), retVal.get(game.teams.get(0).get(1))+score);
                retVal.put(game.teams.get(1).get(0), retVal.get(game.teams.get(1).get(0))-score);
                retVal.put(game.teams.get(1).get(1), retVal.get(game.teams.get(1).get(1))-score);
            }
        }
        return retVal;
    }

    private static Map<String, Integer> getSonnenborn(Tournament tournament, Map<String, Integer> points) {
        Map<String, Integer> retVal = new HashMap<>();

        for(String playerName : tournament.players.keySet()){
            retVal.put(playerName, 0);
        }

        for(List<Game> round : tournament.rounds){
            for(Game game : round){
                Integer bonus = 4*tournament.rounds.size();
                Integer score = null;
                if(game.overtime){
                    if(game.result.get(0) == game.result.get(1)){score = 2;}
                    if(game.result.get(0) > game.result.get(1)){score = 3;}
                    if(game.result.get(0) < game.result.get(1)){score = 1;}
                }
                else{
                    if(game.result.get(0) == game.result.get(1)){throw new RuntimeException("INVALID ROUND RESULT");}
                    if(game.result.get(0) > game.result.get(1)){score = 4;}
                    if(game.result.get(0) < game.result.get(1)){score = 0;}
                }
                Integer team0points = points.get(game.teams.get(0).get(0)) + points.get(game.teams.get(0).get(1));
                Integer team1points = points.get(game.teams.get(1).get(0)) + points.get(game.teams.get(1).get(1));
                retVal.put(game.teams.get(0).get(0), retVal.get(game.teams.get(0).get(0))+(score    *(team1points+bonus-points.get(game.teams.get(0).get(1)))));
                retVal.put(game.teams.get(0).get(1), retVal.get(game.teams.get(0).get(1))+(score    *(team1points+bonus-points.get(game.teams.get(0).get(0)))));
                retVal.put(game.teams.get(1).get(0), retVal.get(game.teams.get(1).get(0))+((4-score)*(team0points+bonus-points.get(game.teams.get(1).get(1)))));
                retVal.put(game.teams.get(1).get(1), retVal.get(game.teams.get(1).get(1))+((4-score)*(team0points+bonus-points.get(game.teams.get(1).get(0)))));
            }
        }
        return retVal;
    }

    private static Map<String, Integer> getBuchholz(Tournament tournament, Map<String, Integer> points) {
        Map<String, Integer> retVal = new HashMap<>();

        for(String playerName : tournament.players.keySet()){
            retVal.put(playerName, 0);
        }

        for(List<Game> round : tournament.rounds){
            for(Game game : round){
                Integer bonus = 4*tournament.rounds.size();
                Integer team0points = points.get(game.teams.get(0).get(0)) + points.get(game.teams.get(0).get(1));
                Integer team1points = points.get(game.teams.get(1).get(0)) + points.get(game.teams.get(1).get(1));
                retVal.put(game.teams.get(0).get(0), retVal.get(game.teams.get(0).get(0))+team1points+bonus-points.get(game.teams.get(0).get(1)));
                retVal.put(game.teams.get(0).get(1), retVal.get(game.teams.get(0).get(1))+team1points+bonus-points.get(game.teams.get(0).get(0)));
                retVal.put(game.teams.get(1).get(0), retVal.get(game.teams.get(1).get(0))+team0points+bonus-points.get(game.teams.get(1).get(1)));
                retVal.put(game.teams.get(1).get(1), retVal.get(game.teams.get(1).get(1))+team0points+bonus-points.get(game.teams.get(1).get(0)));
            }
        }
        return retVal;
    }
}
