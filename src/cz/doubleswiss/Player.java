package cz.doubleswiss;

public class Player {
    String name;
    Integer preGameValue;
    Integer bonus;

    Player(String name, Integer preGameValue){
        this.name = name;
        this.preGameValue = preGameValue;
        this.bonus = 0;
    }

    Player(String name, Integer preGameValue, Integer bonus){
        this.name = name;
        this.preGameValue = preGameValue;
        this.bonus = bonus;
    }
}
