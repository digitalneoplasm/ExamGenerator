import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Question {
    private final List<QuestionVariant> variants;

    public Question(){
        variants = new ArrayList<QuestionVariant>();
    }

    public void addVariant(String name, String id){
        variants.add(new QuestionVariant(name, id));
    }

    public QuestionVariant pickVariant(){
        int val = ThreadLocalRandom.current().nextInt(0, variants.size());
        return variants.get(val);
    }

    static final class QuestionVariant {
        private final String name;
        private final String id;

        QuestionVariant(String name, String id){
            this.name = name;
            this.id = id;
        }

        public String getName(){
            return name;
        }

        public String getId(){
            return id;
        }

        public String toString(){
            return name;
        }
    }
}