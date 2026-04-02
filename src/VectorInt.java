public class VectorInt {
    public int x;
    public int y;

    public VectorInt(){
        x = 0;
        y = 0;
    }

    public VectorInt(int x, int y){
        this.x = x;
        this.y = y;
    }

    public VectorInt addVec(VectorInt v){
        return new VectorInt(x + v.x, y + v.y);
    }

    public VectorInt scalMult(int a){
        return new VectorInt(x * a, y * a);
    }
}
