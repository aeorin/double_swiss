package cz.doubleswiss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Ranking {
    String name;
    List<Player> players;
    List<Integer> points;
    List<Integer> scores;
    List<Integer> sonneborn;
    List<Integer> buchholz;
    List<Integer> ranking;

    Ranking(String name, List<Player> players, List<Integer> points, List<Integer> scores, List<Integer> sonneborn, List<Integer> buchholz){
        this.name = name;
        this.players = players;
        this.points = points;
        this.scores = scores;
        this.sonneborn = sonneborn;
        this.buchholz = buchholz;

        List<Integer> values = new ArrayList<>();
        List<Integer> ranks = new ArrayList<>();
        for(Integer i = 0; i<players.size(); i++){
            ranks.add(i);
            Integer newValue = 0;
            newValue += 1000000000*points.get(i);
            newValue += 1000000*sonneborn.get(i);
            newValue += 1000*buchholz.get(i);
            newValue += 1*scores.get(i);
            values.add(newValue);
        }

//        ranking = ranks.stream().sorted((a,b)->values.get(a).compareTo(values.get(b))).collect(Collectors.toList());
        ranking = ranks.stream().sorted((a,b)->compare(a,b)).collect(Collectors.toList());
    }

    int compare(Integer a, Integer b){
        List<List<Integer>> breakers = Arrays.asList(points, sonneborn, buchholz, scores);
        int retVal = 0;
        for(List<Integer> breaker : breakers){
            retVal = breaker.get(a).compareTo(breaker.get(b));
            if(retVal!=0){
                break;
            }
        }
        return -retVal;
    }
}
