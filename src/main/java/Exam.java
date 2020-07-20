/*
A representation of the exam, including all question variations.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Exam {
    private final List<Question> questions;

    public Exam() {
        this.questions = new ArrayList<>();
    }

    public void addQuestion(Question question){
        questions.add(question);
    }

    public List<Question.QuestionVariant> generateExamVariant(){
        return questions.stream().map(Question::pickVariant).collect(Collectors.toList());
    }
}

