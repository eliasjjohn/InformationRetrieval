import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("static-method")
public class IMDBQueries {

    /**
     * A helper class for pairs of objects of generic types 'K' and 'V'.
     *
     * @param <K> first value
     * @param <V> second value
     */
    class Tuple<K, V> {
        K first;
        V second;

        public Tuple(K f, V s) {
            this.first = f;
            this.second = s;
        }

        @Override
        public int hashCode() {
            return this.first.hashCode() + this.second.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this.first.equals(((Tuple<?, ?>) obj).first)
                    && this.second.equals(((Tuple<?, ?>) obj).second);
        }
    }

    /**
     * All-rounder: Determine all movies in which the director stars as an actor
     * (cast). Return the top ten matches sorted by decreasing IMDB rating.
     *
     * @param movies the list of movies which is to be queried
     * @return top ten movies and the director, sorted by decreasing IMDB rating
     */
    public List<Tuple<Movie, String>> queryAllRounder(List<Movie> movies) {

        List<Tuple<Movie, String>> allrounder = new ArrayList<>();

        for (int i=0; i<movies.size();i++) {
            Movie movie = movies.get(i);
            List<String> directorList = movie.getDirectorList();
            for (int j=0; j<directorList.size();j++) {
                String director = directorList.get(j);
                if (movie.getCastList().contains(director)) {
                    Tuple<Movie, String> result = new Tuple<>(movie, director);
                    allrounder.add(result);
                    break;
                }
            }
        }

        Collections.sort(allrounder, new Comparator<Tuple<Movie, String>>() {
            @Override
            public int compare(Tuple<Movie, String> mov1, Tuple<Movie, String> mov2) {
                return Float.compare(Float.valueOf(mov2.first.getRatingValue()), Float.valueOf((mov1.first.getRatingValue())));
            }
        });

        return allrounder.subList(0,10);
    }

    /**
     * Under the Radar: Determine the top ten US-American movies until (including)
     * 2015 that have made the biggest loss despite an IMDB score above
     * (excluding) 8.0, based on at least 1,000 votes. Here, loss is defined as
     * budget minus gross.
     *
     * @param movies the list of movies which is to be queried
     * @return top ten highest rated US-American movie until 2015, sorted by
     * monetary loss, which is also returned
     */
    public List<Tuple<Movie, Long>> queryUnderTheRadar(List<Movie> movies) {

        ArrayList<Movie> movies1 = movies.stream().filter(movie -> Float.valueOf(movie.getRatingValue()) > 8.0
                && Integer.valueOf(movie.getRatingCount().replaceAll(",", "")) >= 1000
                && Short.valueOf(movie.getYear()) <= 2015
                && movie.getCountryList().contains("USA")).collect(Collectors.toCollection(ArrayList::new));

        movies1.sort((m1, m2) -> {

            Long loss1 = Long.valueOf(m1.getBudget().replaceAll("[^\\d]", ""))
                    - Long.valueOf(m1.getGross().replaceAll("[^\\d]", ""));
            Long loss2 = Long.valueOf(m2.getBudget().replaceAll("[^\\d]", ""))
                    - Long.valueOf(m2.getGross().replaceAll("[^\\d]", ""));
            return Long.compare(loss1, loss2);
        });

        List<Tuple<Movie, Long>> returner = new ArrayList<>();
        movies1.forEach(f -> returner.add(new Tuple<>(f, Math.abs(Long.valueOf(f.getBudget().replaceAll("[^\\d]", ""))
                - Long.valueOf(f.getGross().replaceAll("[^\\d]", ""))))));
     return returner.subList(0, 10);
    }

    /**
     * The Pillars of Storytelling: Determine all movies that contain both
     * (sub-)strings "kill" and "love" in their lowercase description
     * (String.toLowerCase()). Sort the results by the number of appearances of
     * these strings and return the top ten matches.
     *
     * @param movies the list of movies which is to be queried
     * @return top ten movies, which have the words "kill" and "love" as part of
     * their lowercase description, sorted by the number of appearances of
     * these words, which is also returned.
     */
    public List<Tuple<Movie, Integer>> queryPillarsOfStorytelling(
            List<Movie> movies) {

        List<Movie> filtered = movies.stream().filter(new Predicate<Movie>() {
            @Override
            public boolean test(Movie f) {
                String descr = f.getDescription().toLowerCase();
                return (descr.contains("kill") || descr.contains("love"));
            }
        }).collect(Collectors.toList());

        filtered.sort(new Comparator<Movie>() {
            @Override
            public int compare(Movie m1, Movie m2) {
                int count1 = IMDBQueries.this.countSubString(m1.getDescription().toLowerCase(), "kill")
                        + IMDBQueries.this.countSubString(m1.getDescription().toLowerCase(), "love");
                int count2 = IMDBQueries.this.countSubString(m2.getDescription().toLowerCase(), "kill")
                        + IMDBQueries.this.countSubString(m2.getDescription().toLowerCase(), "love");
                return count2 - count1;
            }
        });

        ArrayList<Tuple<Movie, Integer>> returner = new ArrayList<>();

        filtered.subList(0, 10).forEach(new Consumer<Movie>() {
            @Override
            public void accept(Movie f) {
                returner.add(new Tuple<>(f, IMDBQueries.this.countSubString(f.getDescription().toLowerCase(), "kill")
                        + IMDBQueries.this.countSubString(f.getDescription().toLowerCase(), "love")));
            }
        });

        return returner;
    }

    private int countSubString(String full, String sub) {
        int index = 0;
        int count = 0;

        //http://www.java2s.com/Code/Java/Data-Type/Countthenumberofinstancesofsubstringwithinastring.htm
        while ((index = full.indexOf(sub, index)) != -1) {
            index++;
            count++;
        }
        return count;
    }

    /**
     * The Red Planet: Determine all movies of the Sci-Fi genre that mention
     * "Mars" in their description (case-aware!). List all found movies in
     * ascending order of publication (year).
     *
     * @param movies the list of movies which is to be queried
     * @return list of Sci-Fi movies involving Mars in ascending order of
     * publication.
     */
    public List<Movie> queryRedPlanet(List<Movie> movies) {
        List<Movie> filtered = movies.stream().filter(f ->
                f.getDescription().contains("Mars") && f.getGenreList().contains("Sci-Fi"))
                .collect(Collectors.toList());

        filtered.sort((m1, m2) -> {
            int year1 = Integer.valueOf(m1.getYear());
            int year2 = Integer.valueOf(m2.getYear());
            return year1 - year2;
        });

        return filtered;

    }

    /**
     * Colossal Failure: Determine all US-American movies with a duration beyond 2
     * hours, a budget beyond 1 million and an IMDB rating below 5.0. Sort results
     * by ascending IMDB rating.
     *
     * @param movies the list of movies which is to be queried
     * @return list of US-American movies with high duration, large budgets and a
     * bad IMDB rating, sorted by ascending IMDB rating
     */
    public List<Movie> queryColossalFailure(List<Movie> movies) {
       return movies.stream().filter(f ->
                (getDurationAsInt(f)) > 120 &&
                        Float.valueOf(f.getRatingValue()) < 5.0
                        && Long.valueOf(f.getBudget().replaceAll("[^\\d]", "")) > 1000000
                        && f.getCountryList().contains("USA"))
                .sorted((m1, m2) ->
                        (Float.compare(Float.valueOf(m1.getRatingValue()),
                                Float.valueOf(m2.getRatingValue())))).collect(Collectors.toList());

    }

    private int getDurationAsInt(Movie movie) {
        String durationStr = movie.getDuration().replaceAll("[^\\d ]", "");

        int duration = 0;
        if (!durationStr.trim().isEmpty()) {
            String[] durationRaw = durationStr.split(" ");
            if (durationRaw.length > 0) {
                if (durationRaw.length >= 1) duration += Integer.valueOf(durationRaw[0]) * 60;
                if (durationRaw.length == 2) duration += Integer.valueOf(durationRaw[1]);
            }
        }
        return duration;
    }

    /**
     * Uncreative Writers: Determine the 10 most frequent character names of all
     * times ordered by frequency of occurrence. Filter any lowercase names
     * containing substrings "himself", "doctor", and "herself" from the result.
     *
     * @param movies the list of movies which is to be queried
     * @return the top 10 character names and their frequency of occurrence;
     * sorted in decreasing order of frequency
     */
    public List<Tuple<String, Integer>> queryUncreativeWriters(List<Movie> movies) {
        ArrayList<Tuple<String, Integer>> returner = new ArrayList<>();

        Map<String, Integer> map = new HashMap<>();
        movies.forEach(movie -> movie.getCharacterList().forEach(character -> {
            if (!character.toLowerCase().contains("doctor") &&
                    !character.toLowerCase().contains("himself") &&
                    !character.toLowerCase().contains("herself") && character.length()>0) {
                if (map.containsKey(character)) {
                     map.put(character, map.get(character) + 1);
                } else {
                    map.put(character, 1);
                }
            }
        }));

        map.keySet().forEach(key -> returner.add(new Tuple<>(key, map.get(key))));

        returner.sort((t1, t2) ->
                Integer.compare(t2.second, t1.second)
        );

        return returner.subList(0, 10);
    }

    /**
     * Workhorse: Provide a ranked list of the top ten most active actors (i.e.
     * starred in most movies) and the number of movies they played a role in.
     *
     * @param movies the list of movies which is to be queried
     * @return the top ten actors and the number of movies they had a role in,
     * sorted by the latter.
     */
    public List<Tuple<String, Integer>> queryWorkHorse(List<Movie> movies) {

        ArrayList<Tuple<String, Integer>> returner = new ArrayList<>();

        Map<String, Integer> map = new HashMap<>();
        movies.forEach(movie -> movie.getCastList().forEach(new Consumer<String>() {
            @Override
            public void accept(String cast) {
                if (map.containsKey(cast)) {
                    map.put(cast, map.get(cast) + 1);
                } else {
                    map.put(cast, 1);
                }
            }
        }));

        map.keySet().forEach(key -> returner.add(new Tuple<>(key, map.get(key))));

        returner.sort((t1, t2) -> Integer.compare(t2.second, t1.second)
        );

        return returner.subList(0, 10);
    }

    /**
     * Must See: List the best-rated movie of each year starting from 1990 until
     * (including) 2010 with more than 10,000 ratings. Order the movies by
     * ascending year.
     *
     * @param movies the list of movies which is to be queried
     * @return best movies by year, starting from 1990 until 2010.
     */
    public List<Movie> queryMustSee(List<Movie> movies) {
        ArrayList<Movie> returner = new ArrayList<>();
        for (short i = 1990; i <= 2010; i++) {
            int finalI = i;
            Movie top = movies.stream().filter(m -> Integer.valueOf(m.getYear()) == finalI &&
                    Integer.valueOf(m.getRatingCount().replace(",", "")) > 10000)
                    .sorted((m1, m2) -> Float.compare(Float.valueOf(m2.getRatingValue()), Float.valueOf(m1.getRatingValue())))
                    .findFirst().orElse(null);
            if (top != null) returner.add(top);

        }
        return returner;
    }

    /**
     * Rotten Tomatoes: List the worst-rated movie of each year starting from 1990
     * till (including) 2010 with an IMDB score larger than 0. Order the movies by
     * increasing year.
     *
     * @param movies the list of movies which is to be queried
     * @return worst movies by year, starting from 1990 till (including) 2010.
     */
    public List<Movie> queryRottenTomatoes(List<Movie> movies) {
        ArrayList<Movie> returner = new ArrayList<>();
        for (short i = 1990; i <= 2010; i++) {
            int finalI = i;
            Movie top = movies.stream().filter(m -> Integer.valueOf(m.getYear()) == finalI &&
                    Float.valueOf(m.getRatingValue()) > 0f)
                    .sorted((m1, m2) -> Float.compare(Float.valueOf(m1.getRatingValue()), Float.valueOf(m2.getRatingValue())))
                    .findFirst().orElse(null);
            if (top != null) returner.add(top);

        }
        return returner;
    }

    /**
     * Magic Couples: Determine those couples that feature together in the most
     * movies. E.g., Adam Sandler and Allen Covert feature together in multiple
     * movies. Report the top ten pairs of actors, their number of movies and sort
     * the result by the number of movies.
     *
     * @param movies the list of movies which is to be queried
     * @return report the top 10 pairs of actors and the number of movies they
     * feature together. Sort by number of movies.
     */
    public List<Tuple<Tuple<String, String>, Integer>> queryMagicCouple(
            List<Movie> movies) {
        ArrayList<Tuple<Tuple<String,String>, Integer>> returner = new ArrayList<>();

        Map<Tuple<String,String>, Integer> map = new HashMap<>();
        movies.forEach(movie -> {
            List<String> castList = movie.getCastList();
            int size = castList.size();
            for (int i=0;i<size;i++)
                for (int j=i+1; j<size;j++) {
                    Tuple<String,String> tuple = new Tuple<>(castList.get(i),castList.get(j));
                    if (map.containsKey(tuple)) {
                        map.put(tuple, map.get(tuple) + 1);
                    } else {
                        map.put(tuple, 1);
                    }
                }

        });

        map.keySet().forEach(key -> returner.add(new Tuple<>(key, map.get(key))));

        returner.sort((t1, t2) ->
                Integer.compare(t2.second, t1.second));

        return returner.subList(0, 10);
    }


    public static void main(String argv[]) throws IOException {
        String moviesPath = "./data/movies/";

        if (argv.length == 1) {
            moviesPath = argv[0];
        } else if (argv.length != 0) {
            System.out.println("Call with: IMDBQueries.jar <moviesPath>");
            System.exit(0);
        }
        System.out.println(moviesPath);

        List<Movie> movies = MovieReader.readMoviesFrom(new File(moviesPath));

        System.out.println("All-rounder");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, String>> result = queries.queryAllRounder(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() == 10) {
                for (Tuple<Movie, String> tuple : result) {
                    System.out.println("\t" + tuple.first.getRatingValue() + "\t"
                            + tuple.first.getTitle() + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Under the radar");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, Long>> result = queries.queryUnderTheRadar(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<Movie, Long> tuple : result) {
                    System.out.println("\t" + tuple.first.getTitle() + "\t"
                            + tuple.first.getRatingCount() + "\t"
                            + tuple.first.getRatingValue() + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("The pillars of storytelling");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, Integer>> result = queries
                    .queryPillarsOfStorytelling(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<Movie, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first.getTitle() + "\t"
                            + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("The red planet");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryRedPlanet(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Movie movie : result) {
                    System.out.println("\t" + movie.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("ColossalFailure");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryColossalFailure(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Movie movie : result) {
                    System.out.println("\t" + movie.getTitle() + "\t"
                            + movie.getRatingValue());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Uncreative writers");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<String, Integer>> result = queries
                    .queryUncreativeWriters(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<String, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Workhorse");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<String, Integer>> result = queries.queryWorkHorse(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<String, Integer> actor : result) {
                    System.out.println("\t" + actor.first + "\t" + actor.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Must see");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryMustSee(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && !result.isEmpty()) {
                for (Movie m : result) {
                    System.out.println("\t" + m.getYear() + "\t" + m.getRatingValue()
                            + "\t" + m.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Rotten tomatoes");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryRottenTomatoes(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && !result.isEmpty()) {
                for (Movie m : result) {
                    System.out.println("\t" + m.getYear() + "\t" + m.getRatingValue()
                            + "\t" + m.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Magic Couples");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Tuple<String, String>, Integer>> result = queries
                    .queryMagicCouple(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Tuple<Tuple<String, String>, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first.first + ":"
                            + tuple.first.second + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
            System.out.println("");

        }
    }
}