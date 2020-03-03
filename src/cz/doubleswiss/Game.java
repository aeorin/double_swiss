package cz.doubleswiss;

import java.util.Arrays;
import java.util.List;

public class Game {
    List<List<String>> teams;
    Boolean overtime = false;
    List<Integer> result;

    Game(String team1player1, String team1player2, String team2player1, String team2player2){
        teams = Arrays.asList(Arrays.asList(team1player1, team1player2), Arrays.asList(team2player1, team2player2));
    }
}
