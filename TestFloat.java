public class TestFloat {
    public static void main(String[] args) {
        double score = 0d;
        score += 0.70d;
        score += 0.05d;
        System.out.println("Score: " + score);
        System.out.println("Is score < 0.75? " + (score < 0.75d));
    }
}
