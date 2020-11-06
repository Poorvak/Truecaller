package main.java.com.prep.truecaller.model;

import main.java.com.prep.truecaller.exception.*;
import main.java.com.prep.truecaller.model.common.Contact;
import main.java.com.prep.truecaller.model.common.PersonalInfo;
import main.java.com.prep.truecaller.model.common.SocialInfo;
import main.java.com.prep.truecaller.model.common.Tag;
import main.java.com.prep.truecaller.model.tries.ContactTrie;

import lombok.Getter;
import lombok.Setter;

import orestes.bloomfilter.CountingBloomFilter;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
public abstract class Account {
    private String id;
    private String phoneNumber;
    private String password;
    private LocalDateTime lastAccessed;
    private Tag tag;
    private Contact contact;
    private PersonalInfo personalInfo;
    private SocialInfo socialInfo;
    private Business Business;
    private UserCategory userCategory;
    private Map<String, User> contacts;
    private CountingBloomFilter<String> blockedContacts;
    private Set<String> blockedSet;
    private ContactTrie contactTrie;

    public Account() {}

    public Account(String phoneNumber, String firstName) {
        this.phoneNumber = phoneNumber;
        this.personalInfo = new PersonalInfo(firstName);
    }

    public Account(String phoneNumber, String firstName, String lastName) { 
        this(phoneNumber, firstName);
        this.personalInfo.setLastName(lastName);
    }

    public abstract void register(UserCategory userCategory, String userName, String password, String email, String phoneNumber, String countryCode, String firstName);
    public abstract void addContact(User user) throws ContactsExceedException;
    public abstract void removeContact(String number) throws ContactDoesNotExistsException;
    public abstract void blockNumber(String number) throws BlockLimitExceedException;
    public abstract void unblockNumber(String number);
    public abstract void reportSpam(String number, String reason);
    public abstract void upgrade(UserCategory userCategory);
    public abstract boolean isBlocked(String number);
    public abstract boolean canReceive(String number);
    public abstract boolean importContacts(List<User> users);
}
