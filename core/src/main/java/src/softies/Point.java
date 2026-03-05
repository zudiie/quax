<<<<<<< HEAD:Quax/src/Point.java
=======
package src.softies;

>>>>>>> 0bd1067f4dfabb8f693ecef440b728743658350e:core/src/main/java/src/softies/Point.java
public class Point {
    String title;
    private int x;
    private int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        char columnLetter = (char) (x+65);
        this.title = columnLetter + String.valueOf(y);
    }
    //this will be used to check if blocks are next to each other down the road
    public boolean isAdjacent(Point other){
        return Math.abs(this.x - other.x) <= 1 && Math.abs(this.y - other.y) <= 1;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}


