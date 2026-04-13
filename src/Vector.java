public class Vector {
    public double x;
    public double y;

    public Vector(){
        x = 0;
        y = 0;
    }

    public Vector(double x, double y){
        this.x = x;
        this.y = y;
    }

    public Vector addVec(Vector v){
        return new Vector(x + v.x, y + v.y);
    }

    public Vector scalMult(double a){
        return new Vector(x * a, y * a);
    }

    public double getMagnitude() {
        return Math.sqrt(x*x + y*y);
    }
}
