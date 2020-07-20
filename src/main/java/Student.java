public class Student {
    private final String id, lastname, firstname, email;

    public Student (String id, String lastname, String firstname, String email){
        this.id = id;
        this.lastname = lastname;
        this.firstname = firstname;
        this.email = email;
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

}
