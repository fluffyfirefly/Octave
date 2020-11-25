package gg.octave.bot.db.music;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gg.octave.bot.db.ManagedObject;

import java.beans.ConstructorProperties;

public class Insights extends ManagedObject {
    @JsonSerialize
    @JsonDeserialize
    private long songsListenedToCount;

    @JsonSerialize
    @JsonDeserialize
    private long minutesListened;

    @ConstructorProperties("id")
    public Insights(String id) {
        super(id, "insights");
    }

    /**
     * Resets all values to 0.
     *
     * @return The insights class, useful for chaining.
     */
    public Insights reset() {
        this.songsListenedToCount = 0;
        this.minutesListened = 0;
        return this;
    }

    @JsonIgnore
    public long getSongsListenedToCount() {
        return songsListenedToCount;
    }

    @JsonIgnore
    public void setSongsListenedToCount(long songsListenedToCount) {
        this.songsListenedToCount = songsListenedToCount;
    }

    @JsonIgnore
    public long getMinutesListened() {
        return minutesListened;
    }

    @JsonIgnore
    public void setMinutesListened(long minutesListened) {
        this.minutesListened = minutesListened;
    }
}
