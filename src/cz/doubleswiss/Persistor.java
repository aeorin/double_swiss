package cz.doubleswiss;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class Persistor {
    String root;
    private File playersFile;
    private File roundsFolder;
    private File rankingFolder;

    Persistor(String root) throws IOException {
        this.root = root;
        File resourceDirectory = new File("persistence/");
        if (!resourceDirectory.exists()) {
            resourceDirectory.mkdir();
        }
        File rootDirectory = new File("persistence/" + root + "/");
        if (!rootDirectory.exists()) {
            rootDirectory.mkdir();
        }
        playersFile = new File("persistence/" + root + "/players.lugr");
        if (!playersFile.exists()) {
            playersFile.createNewFile();
        }
        roundsFolder = new File("persistence/" + root + "/rounds/");
        if (!roundsFolder.exists()) {
            roundsFolder.mkdir();
        }
        rankingFolder = new File("persistence/" + root + "/rankings/");
        if (!rankingFolder.exists()) {
            rankingFolder.mkdir();
        }
    }

    Tournament loadTournament() throws IOException {
        Tournament retVal = new Tournament();

        List<Player> playerList = loadPlayers();
        retVal.players = new HashMap<>();
        for(Player player : playerList){
            retVal.players.put(player.name, player);
        }

        retVal.rounds = loadRounds();

        return retVal;
    }

    void persistTournament(Tournament tournament) throws IOException {
        int persistedRoundsCount = roundsFolder.listFiles().length;
        for(int i = persistedRoundsCount; i<tournament.rounds.size(); i++){
            File file = new File(roundsFolder.toPath().toString()+"/"+((Integer)i).toString()+"round");
            file.createNewFile();
            PrintWriter writer = new PrintWriter(file);
            writer.print(stringFromRound(tournament.rounds.get(i)));
            writer.close();
        }
    }

    List<Player> loadPlayers() throws IOException {
        List<String> strings = Files.readAllLines(playersFile.toPath());
        if(strings.size()==0){
            return new ArrayList<>();
        }
        return strings.stream().map((string) -> playerFromString(string)).collect(Collectors.toList());
    }

    List<List<Game>> loadRounds() {
        return Arrays.asList(roundsFolder.listFiles()).stream().map((file)->roundFromFile(file)).collect(Collectors.toList());
    }

    private List<Game> roundFromFile(File file) {
        List<String> strings = null;
        try {
            strings = Files.readAllLines(file.toPath());
        }
        catch (Throwable t){
            t.printStackTrace();
        }
        return strings.stream().map((string)->gameFromString(string)).collect(Collectors.toList());
    }

    Game gameFromString(String string){
        List<String> strings = Arrays.asList(string.split(";"));
        Game game = new Game(strings.get(0), strings.get(1), strings.get(2), strings.get(3));
        game.result = Arrays.asList(Integer.valueOf(strings.get(4)), Integer.valueOf(strings.get(5)));
        game.overtime = Boolean.valueOf(strings.get(6));
        return game;
    }

    String stringFromGame(Game game){
        StringBuilder builder = new StringBuilder();
        builder.append(game.teams.get(0).get(0));
        builder.append(";");
        builder.append(game.teams.get(0).get(1));
        builder.append(";");
        builder.append(game.teams.get(1).get(0));
        builder.append(";");
        builder.append(game.teams.get(1).get(1));
        builder.append(";");
        builder.append(game.result == null ? null : game.result.get(0));
        builder.append(";");
        builder.append(game.result == null ? null : game.result.get(1));
        builder.append(";");
        builder.append(game.overtime == null ? false : game.overtime);
        return builder.toString();
    }

    String stringFromRound(List<Game> round){
        StringBuilder builder = new StringBuilder();
        Boolean first = true;
        for(Game game : round){
            if(!first){
                builder.append("\n");
            }
            builder.append(stringFromGame(game));
            first = false;
        }
        return builder.toString();
    }

    private Player playerFromString(String string) {
        List<String> strings = Arrays.asList(string.split(";"));

        return new Player(strings.get(0), Integer.valueOf(strings.get(1)), Integer.valueOf(strings.get(2)));
    }

    public void persistRanking(Ranking ranking) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("Rank;Name;Points;SONN;BUCH;Score");
        int size = ranking.players.size();
        for(int rank=0;rank<size;rank++){
            builder.append("\n");
            Integer j = ranking.ranking.get(rank);
            builder.append(rank);
//            Integer j = rank;
//            builder.append(rank);
//            builder.append(";");
//            builder.append(ranking.ranking.indexOf(rank));
            builder.append(";");
            builder.append(ranking.players.get(j).name);
            builder.append(";");
            builder.append(ranking.points.get(j));
            builder.append(";");
            builder.append(ranking.sonneborn.get(j));
            builder.append(";");
            builder.append(ranking.buchholz.get(j));
            builder.append(";");
            builder.append(ranking.scores.get(j));
        }

        File file = new File(rankingFolder.toPath().toString()+"/"+ranking.name);
        file.createNewFile();
        PrintWriter writer = new PrintWriter(file);
        writer.print(builder.toString());
        writer.close();
    }
}
