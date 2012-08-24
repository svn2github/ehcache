package com.bigmemory.samples.model;

import java.io.Serializable;

public class Person implements Serializable {

    /**
     * Simple Person class that can be searched on by the new Ehcache Search API
     */
    private static final long serialVersionUID = 1L;
    private final int age;
    private final String name;
    private final Gender gender;
    private final Address address;

    public Person(String name, int age, Gender gender, String street, String state, String zip) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.address = new Address(street, state, zip);
    }

    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }

    public enum Gender {
        MALE, FEMALE;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name:" + name + ", age:" + age + ", sex:" + gender.name().toLowerCase() + ")";
    }

    public Address getAddress() {
        return address;
    }

}