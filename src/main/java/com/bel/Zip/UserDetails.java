package com.bel.Zip;

public class UserDetails {

	@Override
	public String toString() {
		return "UserDetails [DOB=" + DOB + ", MobNumber=" + MobNumber + ", Name=" + Name + ", Gender=" + Gender + "]";
	}
	private String DOB;
	private String MobNumber;
	private String Name;
	private String Gender;
	public UserDetails() {
		super();
		// TODO Auto-generated constructor stub
	}
	public String getDOB() {
		return DOB;
	}
	public void setDOB(String dOB) {
		DOB = dOB;
	}
	public String getMobNumber() {
		return MobNumber;
	}
	public void setMobNumber(String mobNumber) {
		MobNumber = mobNumber;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getGender() {
		return Gender;
	}
	public void setGender(String gender) {
		Gender = gender;
	}
	
	
}
