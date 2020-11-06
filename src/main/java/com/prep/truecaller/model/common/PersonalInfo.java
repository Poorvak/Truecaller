package main.java.com.prep.truecaller.model.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalInfo {
    private String firstName;
    private String middleName;
    private String lastName;
    private String initials;
    private String dob;
    private Gender gender;
    private Address address;
    private String companyName;
    private String title;
    
    public PersonalInfo(String firstName) {
        this.setFirstName(firstName);
    }
}
