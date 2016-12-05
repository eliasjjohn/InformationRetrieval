// DO NOT CHANGE THIS PACKAGE NAME.
package blatt2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BooleanQuery {

    /**
     * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
     */
    public BooleanQuery() {
    }


    //token: star -> [5 -> {0},3 -> {1},4] i
    //token: wars -> [5 -> {1},3 {0},4] i+1

    HashMap<String, HashMap<Long, HashSet<Integer>>> titleDict = new HashMap<>();
    HashMap<String, HashSet<Long>> yearDict = new HashMap<>();
    HashMap<String, HashMap<Long, HashSet<Integer>>> plotDict = new HashMap<>();
    HashMap<String, HashSet<Long>> typeDict = new HashMap<>();
    HashMap<String, HashMap<Long, HashSet<Integer>>> episodeDict = new HashMap<>();
    HashMap<Long, String> docNames = new HashMap<>();
    Set<Long> intersection = null;

    /**
     * A method for reading the textual movie plot file and building indices. The
     * purpose of these indices is to speed up subsequent boolean searches using
     * the {@link #booleanQuery(String) booleanQuery} method.
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param plotFile the textual movie plot file 'plot.list', obtainable from <a
     *                 href="http://www.imdb.com/interfaces"
     *                 >http://www.imdb.com/interfaces</a> for personal, non-commercial
     *                 use.
     */
    public void buildIndices(String plotFile) {

        Pattern year = Pattern.compile("(\\([1-2][0-9]+)");

        long index = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1))) {
            String line;
            boolean passedHeader = false;
            while ((line = bufferedReader.readLine()) != null) {

                if (!passedHeader) {
                    if (line.equals("===================")) {
                        passedHeader = true;
                    }
                } else if (!line.isEmpty() && line.length() > 4) {

                    String prefix = line.substring(0, 3);
                    String rest = line.substring(4).toLowerCase();
                    if (rest.endsWith("{{SUSPENDED}}")) continue;

                    if (prefix.equals("MV:")) {
                        index++;
                        Matcher yearMatcher = year.matcher(rest);
                        int start = 0;
                        if (yearMatcher.find()) {
                            String y = yearMatcher.group().replace("(", "").replace(")", "");
                            start = yearMatcher.start();
                            putOrExpand(yearDict, y, index);
                        }

                        if (rest.contains("{")) {
                            putOrExpand(typeDict, "episode", index);
                            String[] episodeTokens = rest.substring(rest.indexOf("{") + 1, rest.lastIndexOf("}")).split("[\\s,.;!?]");
                            for (int i = 0; i < episodeTokens.length; i++) {
                                String s = episodeTokens[i];
                                s = s.replace(" ", "").replace("\"", "");
                                if (!s.isEmpty()) {
                                    putOrExpand(episodeDict, s, index, i);
                                }
                            }
                        } else if (rest.contains("(tv)"))
                            putOrExpand(typeDict, "television", index);
                        else if (rest.contains("(v)")) {
                            putOrExpand(typeDict, "video", index);
                        } else if (rest.contains("(vg)"))
                            putOrExpand(typeDict, "videogame", index);
                        else if (rest.startsWith("\"")) {
                            putOrExpand(typeDict, "series", index);
                        } else {
                            putOrExpand(typeDict, "movie", index);
                        }

                        String[] titleTokens = rest.substring(0, start).split("[\\s,.;!?]");
                        for (int i = 0; i < titleTokens.length; i++) {
                            String s = titleTokens[i];
                            s = s.replace(" ", "").replace("\"", "");
                            if (!s.isEmpty()) {
                                putOrExpand(titleDict, s, index, i);
                            }
                        }
                        docNames.put(index, line);

                    } else if (prefix.equals("PL:")) {
                        String[] plotTokens = rest.split("[\\s,.;!?]");
                        for (int i = 0; i < plotTokens.length; i++) {
                            String s = plotTokens[i];
                            s = s.replace(" ", "");
                            if (!s.isEmpty()) {

                                putOrExpand(plotDict, s, index, i);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void putOrExpand(HashMap<String, HashSet<Long>> map, String key, Long val) {
        HashSet<Long> ids = map.get(key);
        if (ids == null) {
            ids = new HashSet<>();
            ids.add(val);
            map.put(key, ids);
        } else {
            ids.add(val);
        }
    }

    private void putOrExpand(HashMap<String, HashMap<Long, HashSet<Integer>>> map, String key, Long val, int pos) {
        HashMap<Long, HashSet<Integer>> ids = map.get(key);
        if (ids == null) {
            ids = new HashMap<>();
            HashSet<Integer> positions = new HashSet<>();
            positions.add(pos);
            ids.put(val, positions);
            map.put(key, ids);
        } else if (ids.get(val) == null) {
            HashSet<Integer> positions = new HashSet<>();
            positions.add(pos);
            ids.put(val, positions);
        } else {
            ids.get(val).add(pos);
        }
    }

    /**
     * A method for performing a boolean search on a textual movie plot file after
     * indices were built using the {@link #buildIndices(String) buildIndices}
     * method. The movie plot file contains entries of the <b>types</b> movie,
     * series, episode, television, video, and videogame. This method allows term
     * and phrase searches (the latter being enclosed in double quotes) on any of
     * the <b>fields</b> title, plot, year, episode, and type. Multiple term and
     * phrase searches can be combined by using the character sequence " AND ".
     * Note that queries are case-insensitive.<br>
     * <br>
     * Examples of queries include the following:
     * <p>
     * <pre>
     * title:"game of thrones" AND type:episode AND plot:shae AND plot:Baelish
     * plot:Skywalker AND type:series
     * plot:"year 2200"
     * plot:Berlin AND plot:wall AND type:television
     * plot:Cthulhu
     * title:"saber rider" AND plot:april
     * plot:"James Bond" AND plot:"Jaws" AND type:movie
     * title:"Pimp my Ride" AND episodetitle:mustang
     * plot:"matt berninger"
     * title:"grand theft auto" AND type:videogame
     * plot:"Jim Jefferies"
     * plot:Berlin AND type:videogame
     * plot:starcraft AND type:movie
     * type:video AND title:"from dusk till dawn"
     * </pre>
     * <p>
     * More details on (a superset of) the query syntax can be found at <a
     * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
     * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param queryString the query string, formatted according to the Lucene query syntax,
     *                    but only supporting term search, phrase search, and the AND
     *                    operator
     * @return the exact content (in the textual movie plot file) of the title
     * lines (starting with "MV: ") of the documents matching the query
     */
    public Set<String> booleanQuery(String queryString) {

        intersection = null;
        String[] pairs = queryString.split("\\sAND\\s");

        for (String pair : pairs) {
            int split = pair.indexOf(":");
            String key = pair.substring(0, split).toLowerCase();
            String value = pair.substring(split + 1).toLowerCase();

            ArrayList<String> terms;

            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
                terms = new ArrayList<>(Arrays.asList(value.split("[\\s,.;!?]")));
            } else {
                terms = new ArrayList<>();
                terms.add(value);
            }

            switch (key) {
                case "title":
                    if (!processPhrase(terms, titleDict)) return new HashSet<>();
                    break;
                case "plot":
                    if (!processPhrase(terms, plotDict)) return new HashSet<>();
                    break;
                case "type":
                    if (!processToken(value, typeDict)) return new HashSet<>();
                    break;
                case "year":
                    if (!processToken(value, yearDict)) return new HashSet<>();
                    break;
                case "episodetitle":
                    if (!processPhrase(terms, episodeDict)) return new HashSet<>();
                    break;
                default:
                    Logger.getAnonymousLogger().log(Level.WARNING, "Unknown field.");
                    break;

            }

        }
        Set<String> returner = new HashSet<>();
        for (Long id : intersection) {
            returner.add(docNames.get(id));
        }
        return returner;
    }


    private boolean processPhrase(ArrayList<String> terms, HashMap<String, HashMap<Long, HashSet<Integer>>> map) {
        //    System.out.println("Term " + terms.get(0)+ ": " + map.get(terms.get(0)));
        //  System.out.println("Intersection: " + intersection);
        if (map.get(terms.get(0)) != null) {

            int i = 0;
            if (intersection == null) {
                intersection = new HashSet<>(map.get(terms.get(0)).keySet());
                i = 1;
            }


            for (; i < terms.size(); i++) {
                //     System.out.println("Term " + terms.get(i)+ ": " + map.get(terms.get(i)));

                ArrayList toRemove = new ArrayList();
                if (map.get(terms.get(i)) == null) return false;
                intersection.retainAll(map.get(terms.get(i)).keySet());
                if (i > 0) {
                    for (Long l : intersection) { // jede ID welche passt
                        boolean passed = false;
                        for (int pos : map.get(terms.get(i)).get(l)) {
                            for (int pos_old : map.get(terms.get(i - 1)).get(l)) {
                                if (pos - pos_old == 1) {
                                    passed = true;
                                    break;
                                }
                            }
                            if (passed) break;
                        }
                        if (!passed) {
                            toRemove.add(l);
                        }
                    }
                }
                intersection.removeAll(toRemove);
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean processToken(String term, HashMap<String, HashSet<Long>> map) {

        if (map.get(term) != null) {
            if (intersection == null) {
                intersection = new HashSet<>(map.get(term));
            } else {
                if (map.get(term) == null) return false;
                intersection.retainAll(map.get(term));

            }
        } else {
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        BooleanQuery bq = new BooleanQuery();


    if (args.length < 3) {
      System.err
          .println("usage: java -jar BooleanQuery.jar <plot list file> <queries file> <results file>");
      System.exit(-1);
    }


      //  args = new String[]{"/users/elias.john/Desktop/plot_big.list", "/users/elias.john/Desktop/queries.txt", "/users/elias.john/Desktop/results.txt"};

        // build indices
        System.out.println("building indices...");
        long tic = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long mem = runtime.totalMemory() - runtime.freeMemory();
        bq.buildIndices(args[0]);

        System.out
                .println("runtime: " + (System.nanoTime() - tic) + " nanoseconds");
        System.out
                .println("memory: " + (((runtime.totalMemory() - runtime.freeMemory()) - mem) / (1048576l))
                        + " MB (rough estimate)");


//        System.out.println(bq.booleanQuery("title:\"from dusk till dawn\""));

        // parsing the queries that are to be run from the queries file
        List<String> queries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(args[1]), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null)
                queries.add(line);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // parsing the queries' expected results from the results file
        List<Set<String>> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(args[2]), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Set<String> result = new HashSet<>();
                results.add(result);
                for (int i = 0; i < Integer.parseInt(line); i++) {
                    result.add(reader.readLine());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // run queries
        for (int i = 0; i < queries.size(); i++) {
            String query = queries.get(i);
            Set<String> expectedResult = i < results.size() ? results.get(i)
                    : new HashSet<>();
            System.out.println();
            System.out.println("query:           " + query);
            tic = System.nanoTime();
            Set<String> actualResult = bq.booleanQuery(query);

            // sort expected and determined results for human readability
            List<String> expectedResultSorted = new ArrayList<>(expectedResult);
            List<String> actualResultSorted = new ArrayList<>(actualResult);
            Comparator<String> stringComparator = new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            };
            expectedResultSorted.sort(stringComparator);
            actualResultSorted.sort(stringComparator);

            System.out.println("runtime:         " + (System.nanoTime() - tic)
                    + " nanoseconds.");
            System.out.println("expected result: " + expectedResultSorted.toString());
            System.out.println("actual result:   " + actualResultSorted.toString());
            System.out.println(expectedResult.equals(actualResult) ? "SUCCESS"
                    : "FAILURE");
        }

    }


}
