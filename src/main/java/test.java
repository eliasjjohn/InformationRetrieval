import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Project: IR_blatt1
 * Created by elias.john on 02.11.16.
 */
public class test {

    public static void main(String[] args){
        MovieReader r = new MovieReader();
        List<Movie> list = null;
        try {
            list = r.readMoviesFrom(new File("/Users/elias.john/git/IR_blatt1/src/main/resources/example"));
           // System.out.println()
            System.out.println(list.get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }





       IMDBSpider spider = new IMDBSpider();

        try {
            spider.fetchIMDBMovies("/Users/elias.john/git/IR_blatt1/src/main/data/movies.json","/Users/elias.john/git/IR_blatt1/src/main/data");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
