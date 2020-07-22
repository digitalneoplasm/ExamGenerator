import java.util.Optional;

public class Student {
    private final String id, lastname, firstname, email;
    private final Optional<Integer> overrideTime;

    public Student (String id, String lastname, String firstname, String email, String overrideTime){
        this.id = id;
        this.lastname = lastname;
        this.firstname = firstname;
        this.email = email;

        if (overrideTime != null){
            this.overrideTime = Optional.of(Integer.parseInt(overrideTime));
        }
        else this.overrideTime = Optional.empty();
    }

    public String getEmail(){
        return email;
    }

    public String getId(){
        return id;
    }

    public String getLastname(){
        return lastname;
    }

    public String getFirstname(){
        return firstname;
    }

    public String toString(){
        return lastname + "_" + firstname + "_" + id;
    }

    public Optional<Integer> getOverrideTime(){
        return overrideTime;
    }
}
