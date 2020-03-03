package cz.doubleswiss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Standing {

    Integer points;
    Integer scores;
    Integer sonneborn;
    Integer buchholz;

    Standing(Integer points, Integer scores, Integer sonneborn, Integer buchholz){
        this.points = points;
        this.scores = scores;
        this.sonneborn = sonneborn;
        this.buchholz = buchholz;
    }
}
