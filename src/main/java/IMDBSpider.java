import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IMDBSpider {
    ExecutorService executorService;

    public IMDBSpider() {
    }

    /**
     * For each title in file movieListJSON:
     * <p>
     * <pre>
     * You should:
     * - First, read a list of 500 movie titles from the JSON file in 'movieListJSON'.
     *
     * - Secondly, for each movie title, perform a web search on IMDB and retrieve
     * movie’s URL: http://akas.imdb.com/find?q=<MOVIE>&s=tt&ttype=ft
     *
     * - Thirdly, for each movie, extract metadata (actors, budget, description)
     * from movie’s URL and store to a JSON file in directory 'outputDir':
     *    http://www.imdb.com/title/tt0499549/?ref_=fn_al_tt_1 for Avatar - store
     * </pre>
     *
     * @param movieListJSON JSON file containing movie titles
     * @param outputDir     output directory for JSON files with metadata of movies.
     * @throws IOException
     */
    public void fetchIMDBMovies(String movieListJSON, String outputDir)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode m = mapper.readValue(new File(movieListJSON), JsonNode.class);
        Logger.getAnonymousLogger().log(Level.INFO, "Going to use " + Runtime.getRuntime().availableProcessors() + " threads.");
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        m.forEach(f -> writeMovieThreaded(f.get("movie_name").asText(), outputDir));

        executorService.shutdown();

    }

    /**
     * Helper method to remove html and formating from text.
     *
     * @param text The text to be cleaned
     * @return clean text
     */
    protected static String cleanText(String text) {
        return text.replaceAll("\\<.*?>", "").replace("&nbsp;", " ")
                .replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    protected void writeMovieThreaded(String movieName, String outputDir) {
        executorService.submit(new Callable<Void>() {
            public Void call() {
                writeMovie(movieName, outputDir);
                return null;
            }
        });
    }

    protected void writeMovie(String movieName, String outputDir) {
        try {
            Movie movie = new Movie();
            Document doc = Jsoup.connect("http://akas.imdb.com/find?q=" + movieName + "&s=tt&ttype=ft").ignoreHttpErrors(true).get();

            // Get URL
            Element firstElementInTable = doc.select("td.result_text").first();
            if (firstElementInTable != null && firstElementInTable.children().size() > 0) {
                movie.setTitle(firstElementInTable.child(0).text());
                movie.setUrl(firstElementInTable.child(0).attr("abs:href"));
            }

            doc = Jsoup.connect(movie.getUrl()).ignoreHttpErrors(true).get();

            // Get Cast
            Element actors = doc.select("table.cast_list").first();
            if (actors != null && actors.children().size() > 0) {
                for (int i = 1; i < actors.child(0).children().size(); i++) {
                    Element child = actors.child(0).child(i);
                    if (child.className().equals("odd") || child.className().equals("even")) {
                        String actor = child.select("span.itemprop").text();
                        String role = child.select("td.character").first().child(0).text();
                        movie.getCastList().add(actor);
                        movie.getCharacterList().add(role);
                    }
                }
            }
            // Get Director
            Elements directors = doc.select("div.credit_summary_item > span[itemprop=director]");
            directors.forEach(d -> movie.getDirectorList().add(d.text().replace(",","")));

            // Get Year
            Element year = doc.select("#titleYear").first();
            if (year != null) {
                movie.setYear(year.text().substring(1, 5));
            }

            //Get RatingValue
            Element ratingVal = doc.select("div.ratingValue span[itemprop*=ratingValue]").first();
            if (ratingVal != null) movie.setRatingValue(ratingVal.text());

            //Get RatingCount
            Element ratingCount = doc.select("div.imdbRating span[itemprop*=ratingCount]").first();
            if (ratingCount != null) movie.setRatingCount(ratingCount.text());

            //Get Duration
            Element duration = doc.select("div.subtext").first().select("time").first();
            if (duration != null) movie.setDuration(duration.text());

            // Get Story
            Element story = doc.select("#titleStoryLine p").first();
            if (story != null) movie.setDescription(story.text());

            // Get Gengres
            Elements genres = doc.select("div[itemprop*=genre]").first().select("a[href^=/genre");
            if (genres != null) genres.forEach(f -> movie.getGenreList().add(f.text()));

            Element details = doc.select("#titleDetails").first();
            // Get Countries
            Elements countries = details.select("a[href*=/search/title?countries");
            if (countries != null) countries.forEach(f -> movie.getCountryList().add(f.text()));

            // Get Budget and Gross
            Element gross = details.select(":containsOwn(Gross:)").first();
            Element budget = details.select(":containsOwn(Budget:)").first();
            if (gross!=null) movie.setBudget(gross.parent().ownText());
            if (budget!=null) movie.setGross(budget.parent().ownText());


        System.out.println(movie.toString());


        JsonFactory jfactory = new JsonFactory();
        JsonGenerator jGenerator = null;
        try {
            jGenerator = jfactory.createGenerator(new File(
                    outputDir + "/" + movieName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".json"), JsonEncoding.UTF8);
            DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
            pp.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
            jGenerator.setPrettyPrinter(pp);
            jGenerator.writeStartArray();
            jGenerator.writeStartObject();
            jGenerator.writeStringField("url", movie.getUrl());
            jGenerator.writeStringField("title", movie.getTitle());
            jGenerator.writeStringField("year", movie.getYear());
            arrayListToJSONString(jGenerator, "genreList", movie.getGenreList());
            arrayListToJSONString(jGenerator, "countryList", movie.getCountryList());
            jGenerator.writeStringField("description", movie.getDescription());
            jGenerator.writeStringField("budget", movie.getBudget());
            jGenerator.writeStringField("gross", movie.getGross());
            jGenerator.writeStringField("ratingValue", movie.getRatingValue());
            jGenerator.writeStringField("ratingCount", movie.getRatingCount());
            jGenerator.writeStringField("duration", movie.getDuration());
            arrayListToJSONString(jGenerator, "castList", movie.getCastList());
            arrayListToJSONString(jGenerator, "directorList", movie.getDirectorList());
            arrayListToJSONString(jGenerator, "characterList", movie.getCharacterList());

            jGenerator.writeEndObject();
            jGenerator.writeEndArray();
            jGenerator.close();


        } catch (IOException e) {
            e.printStackTrace();
        }


    } catch(
    Exception e)

    {
        Logger.getLogger("IMDBSpider").log(Level.WARNING, "Could not read information for movie: " + movieName, e);
    }

}


    public static void main(String argv[]) throws IOException {
        String moviesPath = "./data/movies.json";
        String outputDir = "./data";

        if (argv.length == 2) {
            moviesPath = argv[0];
            outputDir = argv[1];
        } else if (argv.length != 0) {
            System.out.println("Call with: IMDBSpider.jar <moviesPath> <outputDir>");
            System.exit(0);
        }

        IMDBSpider sp = new IMDBSpider();
        sp.fetchIMDBMovies(moviesPath, outputDir);
    }

    private void arrayListToJSONString(JsonGenerator jGenerator, String field, List<String> list) throws IOException {
        jGenerator.writeArrayFieldStart(field);
        list.forEach(f -> {
            try {
                jGenerator.writeString(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        jGenerator.writeEndArray();
    }
}
