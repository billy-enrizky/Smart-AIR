package com.example.myapplication.userdata;


public class ChildAccount extends UserData {
    String Parent_id;
    String password;
    String dob;
    String name;
    String notes;
    String age;
    public ChildAccount(String ID) {
        super(ID);
    }
    public ChildAccount() {
        super();
    }
    public ChildAccount(String Parent_id, String password, String dob, String name, String notes, String age, String ID) {
        this.Parent_id = Parent_id;
        this.password = password;
        this.dob = dob;
        this.name = name;
        this.notes = notes;
        this.age = age;
        this.ID = ID;
    }
    public void setDob(String Dob) {
        this.dob = Dob;
    }
    public void setAge(String age) {
        this.age = age;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setParent_id(String Parent_id) {
        this.Parent_id = Parent_id;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setID(String ID) {
        this.ID = ID;
    }
    public String getParent_id(){
        return Parent_id;
    }
    public String getPassword(){
        return password;
    }
    public String getDob(){
        return dob;
    }
    public String getName(){
        return name;
    }
    public String getNotes(){
        return notes;
    }
    public String getAge(){
        return age;
    }

}
