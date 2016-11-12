import java.io.IOException;

/**
 * Project: IR_blatt1
 * Created by elias.john on 02.11.16.
 */
public class test {

    public static void main(String[] args){

       IMDBSpider spider = new IMDBSpider();

        try {
            spider.fetchIMDBMovies("/Users/elias.john/git/IR_blatt1/src/main/data/movies.json","/Users/elias.john/git/IR_blatt1/src/main/data");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
