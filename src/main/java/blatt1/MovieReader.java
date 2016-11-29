package blatt1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MovieReader {

    public MovieReader() {
    }

    /**
     * Read movies from JSON files in directory 'moviesDir' formatted according to
     * 'example_movie_avatar.json'.
     * <p>
     * Each movie should contain the attributes: url, title, year, genreList,
     * countryList, description, budget, gross, ratingValue, ratingCount,
     * duration, castList, characterList
     * <p>
     * Each attribute is treated as a String and names ending in 'list' like
     * 'genreList' refer to JSON lists.
     *
     * @param moviesDir The directory containing the set of JSON files, each ending with a
     *                  suffix ".json".
     * @return A list of movies
     * @throws IOException
     */
    public static List<Movie> readMoviesFrom(File moviesDir) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Movie> movies = new ArrayList<>();
        for (File f : moviesDir.listFiles()) {
            if (f.getName().endsWith(".json")) {

           JsonNode m =  mapper.readValue(f, JsonNode.class).get(0);


            Movie obj = new Movie();
            obj.setTitle(getString(m, "title"));
            obj.setYear(getString(m, "year"));
            obj.setUrl(getString(m, "url"));
            obj.setGenreList(getJsonArray(m, "genreList"));
            obj.setCountryList(getJsonArray(m, "countryList"));
            obj.setDescription(getString(m, "description"));
            obj.setBudget(getString(m, "budget"));
            obj.setGross(getString(m, "gross"));
            obj.setRatingValue(getString(m, "ratingValue"));
            obj.setRatingCount(getString(m, "ratingCount"));
            obj.setDuration(getString(m, "duration"));
            obj.setCastList(getJsonArray(m, ("castList")));
            obj.setCharacterList(getJsonArray(m, ("characterList")));
            obj.setDirectorList(getJsonArray(m, "directorList"));
            movies.add(obj);
            }
        }
        return movies;
    }


    /**
     * A helper function to parse a JSON array.
     *
     * @param m
     *          The JSON object, containing an array under the attribute 'key'.
     * @param key
     *          The key of the array
     * @return A list containing the Strings in the JSON object.
     */
  protected static List<String> getJsonArray(JsonNode m, String key) {

        JsonNode array = m.path(key);
        List<String> result = new ArrayList<>();
        for (int i = 0; i<array.size(); i++) {
            result.add(array.get(i).asText());
        }
        return result;
    }

    /**
     * A helper function to parse a JSON String.
     *
     * @param m
     *          The JSON object, containing a String under the attribute 'key'.
     * @param key
     *          The key of the array
     * @return The String in the JSON object.
     */
  protected static String getString(JsonNode m, String key) {

    return m.path(key).asText();
  }
}
