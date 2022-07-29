package arwstate;

import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;

public class Obstacle {
    String tagid;
    Float posx,posy;
    Float lado;
    LocalDateTime dateTime;

    public Obstacle(String tagid, Float posx, Float posy, Float lado, LocalDateTime dateTime) {
        this.tagid = tagid;
        this.posx = posx;
        this.posy = posy;
        this.lado = lado;
        this.dateTime=dateTime;
    }

    public Rectangle2D geraRect(){
        Rectangle2D rect= new Rectangle2D.Float(posx,posy,lado,lado );
        return rect;
    }

    public void refreshTime(LocalDateTime dateTime){this.dateTime=dateTime;}

    public LocalDateTime getDateTime(){return dateTime;}

    //public LocalDateTime getTimediff(LocalDateTime now){
    //    return now.minus(dateTime);

    //}
}
