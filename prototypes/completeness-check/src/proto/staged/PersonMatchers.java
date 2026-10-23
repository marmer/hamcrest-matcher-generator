package proto.staged;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.Matcher;
import proto.Person;
import proto.hamcrest.PersonMatcher;

/**
 * PROTOTYPE. Candidate H2: compile-time completeness via a staged builder.
 * One step interface per property, in declaration order; only after the last
 * step does a {@link PersonMatcher} exist. Adding a property inserts a step,
 * so every existing chain stops compiling.
 */
public final class PersonMatchers {
    private PersonMatchers() {}

    public static NameStep isPersonWithAll() {
        PersonMatcher m = PersonMatcher.isPerson().checkingAllProperties();
        return new Steps(m);
    }

    public interface NameStep {
        AgeStep name(Matcher<? super String> m);
        AgeStep name(String v);
        AgeStep ignoringName();
    }
    public interface AgeStep {
        EmailStep age(Matcher<? super Integer> m);
        EmailStep age(int v);
        EmailStep ignoringAge();
    }
    public interface EmailStep {
        PhoneStep email(Matcher<? super String> m);
        PhoneStep email(String v);
        PhoneStep ignoringEmail();
    }
    public interface PhoneStep {   // the step that appears when phone is added
        PersonMatcher phone(Matcher<? super String> m);
        PersonMatcher phone(String v);
        PersonMatcher ignoringPhone();
    }

    private static final class Steps implements NameStep, AgeStep, EmailStep, PhoneStep {
        private final PersonMatcher m;
        Steps(PersonMatcher m) { this.m = m; }
        public AgeStep name(Matcher<? super String> x) { m.withName(x); return this; }
        public AgeStep name(String v) { return name(equalTo(v)); }
        public AgeStep ignoringName() { m.ignoringName(); return this; }
        public EmailStep age(Matcher<? super Integer> x) { m.withAge(x); return this; }
        public EmailStep age(int v) { return age(equalTo(v)); }
        public EmailStep ignoringAge() { m.ignoringAge(); return this; }
        public PhoneStep email(Matcher<? super String> x) { m.withEmail(x); return this; }
        public PhoneStep email(String v) { return email(equalTo(v)); }
        public PhoneStep ignoringEmail() { m.ignoringEmail(); return this; }
        public PersonMatcher phone(Matcher<? super String> x) { return m.withPhone(x); }
        public PersonMatcher phone(String v) { return phone(equalTo(v)); }
        public PersonMatcher ignoringPhone() { return m.ignoringPhone(); }
    }
}
