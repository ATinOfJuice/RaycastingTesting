import java.util.*;

public class Sprite {
    public double x;
    public double y;
    public int texture;

    public Sprite(double x, double y, int texture){
        this.x = x;
        this.y = y;
        this.texture = texture;
    }

    public void setXY (double x, double y){
        this.x = x;
        this.y = y;
    }

    public void setXY (Vector v) {
        x = v.x;
        y = v.y;
    }

    public static void sortSprites(int[] order, double[] dist, int amount){
        IDPair[] sprites = new IDPair[amount];
        for (int i = 0; i < amount; i++){
            sprites[i] = new IDPair(order[i], dist[i]);
        }
        Arrays.sort(sprites, new SortByDist());
        for (int i = 0; i < amount; i++){
            order[amount - 1 - i] = sprites[i].x;
            dist[amount - 1 - i] = sprites[i].y;
        }
    }
}

class IDPair {
    public int x;
    public double y;

    public IDPair(int x, double y) {
        this.x = x;
        this.y = y; 
    }
}

class SortByDist implements Comparator<IDPair>{
    public int compare(IDPair p1, IDPair p2){
        if (p1.y > p2.y) {
            return 1;
        } else {
            return -1;
        }
    }
}