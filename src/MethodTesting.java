public class MethodTesting {
    public static void main(String[] args) {
        int[] order = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        double[] dist = {6, 2, 9, 10, 2, 1, 67, 8, 9, 3};
        Sprite.sortSprites(order, dist, 10);
        for (int i = 0; i < order.length; i++){
            System.out.println(order[i] + ", " + dist[i]);
        }
    }
}