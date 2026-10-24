package proto.model;

/** PROTOTYPE sample: flat POJO, no toString (as many real POJOs). */
public class Person {
    private final String name;
    private final int age;
    private final String email;
    private final String phone;

    public Person(String name, int age, String email, String phone) {
        this.name = name; this.age = age; this.email = email; this.phone = phone;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
