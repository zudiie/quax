
public class Point {

    private int x;
    private int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
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
