package blatt2;

import java.util.List;

/**
 * Project: IR_blatt1
 * Created by elias.john on 28.11.16.
 */
public class Document {

    private List<Long> title;
    private List<Long> type;
    private List<Long> description;
    private short year;
    private List<Long> episodeTitle;

    public List<Long> getTitle() {
        return title;
    }

    public void setTitle(List<Long> title) {
        this.title = title;
    }

    public List<Long> getType() {
        return type;
    }

    public void setType(List<Long> type) {
        this.type = type;
    }

    public List<Long> getDescription() {
        return description;
    }

    public void setDescription(List<Long> description) {
        this.description = description;
    }

    public short getYear() {
        return year;
    }

    public void setYear(short year) {
        this.year = year;
    }

    public List<Long> getEpisodeTitle() {
        return episodeTitle;
    }

    public void setEpisodeTitle(List<Long> episodeTitle) {
        this.episodeTitle = episodeTitle;
    }
}
