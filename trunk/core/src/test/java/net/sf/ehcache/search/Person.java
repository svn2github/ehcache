package net.sf.ehcache.search;

import java.io.Serializable;

/**
 * A domain object used for testing
 *
 * @author Greg Luck
 */
public class Person {

    private final String name;
    private final int age;
    private final Gender gender;

    public Person(String name, int age, Gender gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public Serializable getAge() {
        return age;
    }

    public Gender getGender() {
        return gender;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name:" + name + ", age:" + age + ", sex:" + gender.name().toLowerCase() + ")";
    }


    enum Gender {
        MALE, FEMALE;
    }


}
