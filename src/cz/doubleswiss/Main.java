package cz.doubleswiss;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        Persistor persistor = new Persistor("tournament_sample");

        List<Player> playerList = persistor.loadPlayers();
        if(playerList.size()==0){
            System.out.println("Please fill out players");
            return;
        }
        System.out.println("Players filled");

        Tournament tournament = persistor.loadTournament();
        List<Game> nextRound = RoundGenerator.generateRound(tournament);

        Ranking ranking = RankingGenerator.createRanking(tournament);
        tournament.rounds.add(nextRound);
        persistor.persistTournament(tournament);
        persistor.persistRanking(ranking);
    }
}
