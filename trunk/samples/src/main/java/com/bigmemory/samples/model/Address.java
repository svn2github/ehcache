package com.bigmemory.samples.model;

import java.io.Serializable;

public class Address implements Serializable {
	private String street;
	private String state;
	private String city;

	public Address(String street, String city, String state) {
		this.setStreet(street);
		this.setState(state);
		this.setCity(city);
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCity() {
		return city;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getState() {
		return state;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getStreet() {
		return street;
	}

}
