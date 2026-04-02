import java.util.HashMap;

public class Fibonacci {

    public static HashMap<Long, Long> fibs = new HashMap<Long, Long>();

    public static long fib (long num){
        if (num == 1 || num == 2) return 1;
        long num1, num2;
        if (fibs.containsKey(num - 1)){
            num1 = fibs.get(num - 1);
        } else {
            num1 = fib(num - 1);
            fibs.put(num - 1, num1);
        }
        if (fibs.containsKey(num - 2)){
            num2 = fibs.get(num - 2);
        } else {
            num2 = fib(num - 2);
            fibs.put(num - 2, num2);
        }
        return num1 + num2;
    }













    

    public static long fibBad (long num){
        if (num == 1 || num == 2) return 1;
        return fibBad(num - 1) + fibBad(num - 2);
    }

    public static void main(String[] args) {
        System.out.println(fib(100));
        System.out.println(fibBad(100));
    }

    
}