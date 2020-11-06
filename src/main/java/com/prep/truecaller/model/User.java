package main.java.com.prep.truecaller.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import main.java.com.prep.truecaller.GlobalContacts;
import main.java.com.prep.truecaller.exception.BlockLimitExceedException;
import main.java.com.prep.truecaller.exception.ContactDoesNotExistsException;
import main.java.com.prep.truecaller.exception.ContactsExceedException;
import main.java.com.prep.truecaller.model.common.Contact;
import main.java.com.prep.truecaller.model.common.GlobalSpam;
import main.java.com.prep.truecaller.model.common.PersonalInfo;
import main.java.com.prep.truecaller.model.tries.ContactTrie;

import orestes.bloomfilter.FilterBuilder;

import static main.java.com.prep.truecaller.model.common.Constant.*;


public class User extends Account {
    
    public User() {
        setContactTrie(new ContactTrie());
    }

    public User(String phoneNumber, String firstName) {
        super(phoneNumber, firstName);
    }

    public User(String phoneNumber, String firstName, String lastName) {
        super(phoneNumber, firstName, lastName);
    }

    public void register(UserCategory userCategory, String userName, String password, String email, String phoneNumber, String countryCode, String firstName) {
        setId(UUID.randomUUID().toString());
        setUserCategory(userCategory);
        // setUserName(userName);
        setPassword(password);
        setContact(new Contact(phoneNumber, email, countryCode));
        setPersonalInfo(new PersonalInfo(firstName));
        init(userCategory);
        insertToTries(phoneNumber, firstName);
    }

    private void init(UserCategory userCategory) {
        switch (userCategory) {
            case FREE:
                setContacts(new HashMap<>(MAX_FREE_USER_CONTACTS));
                setBlockedContacts(new FilterBuilder(MAX_FREE_USER_BLOCKED_CONTACTS, .01).buildCountingBloomFilter());
                setBlockedSet(new HashSet<>(MAX_FREE_USER_BLOCKED_CONTACTS));
                break;
            case GOLD:
                setContacts(new HashMap<>(MAX_GOLD_USER_CONTACTS));
                setBlockedContacts(new FilterBuilder(MAX_GOLD_USER_BLOCKED_CONTACTS, .01).buildCountingBloomFilter());
                setBlockedSet(new HashSet<>(MAX_GOLD_USER_BLOCKED_CONTACTS));
                break;
            case PLATINUM:
                setContacts(new HashMap<>(MAX_PLATINUM_USER_CONTACTS));
                setBlockedContacts(new FilterBuilder(MAX_PLATINUM_USER_BLOCKED_CONTACTS, .01).buildCountingBloomFilter());
                setBlockedSet(new HashSet<>(MAX_PLATINUM_USER_BLOCKED_CONTACTS));
                break;
        }
    }

    public void addContact(User user) throws ContactsExceedException {
        checkAddUser();
        getContacts().putIfAbsent(user.getPhoneNumber(), user);
        insertToTries(user.getPhoneNumber(), user.getPersonalInfo().getFirstName());
    }

    public void removeContact(String number) throws ContactDoesNotExistsException {
        User contact =  getContacts().get(number);
        if ( contact == null ) {
            throw new ContactDoesNotExistsException("Contact does not exists");
        }
        getContacts().remove(number);
        getContactTrie().delete(number);
        getContactTrie().delete(contact.getPersonalInfo().getFirstName());
    }

    public void blockNumber(String number) throws BlockLimitExceedException {
        checkBlockUser();
        getBlockedContacts().add(number);
    }

    public void unblockNumber(String number) {
        getBlockedContacts().remove(number);
    }

    public void reportSpam(String number, String reason) {
        getBlockedContacts().add(number);
        GlobalSpam.INSTANCE.reportSpam(number, this.getPhoneNumber(), reason);
    }

    public void upgrade(UserCategory userCategory) {
        int count = 0;
        int blockedCount = 0;
        switch (userCategory) {
            case GOLD:
                count = MAX_GOLD_USER_CONTACTS;
                blockedCount = MAX_GOLD_USER_BLOCKED_CONTACTS;
                break;
            case PLATINUM:
                count = MAX_PLATINUM_USER_CONTACTS;
                blockedCount = MAX_PLATINUM_USER_BLOCKED_CONTACTS;
                break;
        }
        upgradeContacts(count);
        upgradeBlockedContact(blockedCount);
    }

    public boolean isBlocked(String number) {
        return getBlockedContacts().contains(number);
    }

    public boolean canReceive(String number) {
        return !isBlocked(number) && !GlobalSpam.INSTANCE.isGlobalSpam(number);
    }

    @Override
    public boolean importContacts(List<User> users) {
        for ( User user : users ) {
            try {
                addContact(user);
            } catch (ContactsExceedException cee) {
                return false;
            }
        }
        return true;
    }

    private void upgradeBlockedContact(int blockedCount) {
        setBlockedContacts(new FilterBuilder(blockedCount, .01).buildCountingBloomFilter());
        Set<String> upgradedSet = new HashSet<>();
        for (String blocked : getBlockedSet()) {
            upgradedSet.add(blocked);
            getBlockedContacts().add(blocked);
        }
    }

    private void upgradeContacts(int count) {
        Map<String, User> upgradedContacts = new HashMap<>(count);
        for (Map.Entry<String, User> entry : getContacts().entrySet()) {
            upgradedContacts.putIfAbsent(entry.getKey(), entry.getValue());
        }
        setContacts(upgradedContacts);
    }

    private void checkAddUser() throws ContactsExceedException {
        switch (this.getUserCategory()) {
            case FREE:
                if (this.getContacts().size() >= MAX_FREE_USER_CONTACTS)
                    throw new ContactsExceedException("Default contact size exceeded");
            case GOLD:
                if (this.getContacts().size() >= MAX_GOLD_USER_CONTACTS)
                    throw new ContactsExceedException("Default contact size exceeded");
            case PLATINUM:
                if (this.getContacts().size() >= MAX_PLATINUM_USER_CONTACTS)
                    throw new ContactsExceedException("Default contact size exceeded");
        }
    }

    private void checkBlockUser() throws BlockLimitExceedException {
        switch (this.getUserCategory()) {
            case FREE:
                if (this.getContacts().size() >= MAX_FREE_USER_BLOCKED_CONTACTS)
                    throw new BlockLimitExceedException("Exceeded max contacts to be blocked");
            case GOLD:
                if (this.getContacts().size() >= MAX_GOLD_USER_BLOCKED_CONTACTS)
                    throw new BlockLimitExceedException("Exceeded max contacts to be blocked");
            case PLATINUM:
                if (this.getContacts().size() >= MAX_PLATINUM_USER_BLOCKED_CONTACTS)
                    throw new BlockLimitExceedException("Exceeded max contacts to be blocked");
        }
    }

    private void insertToTries(String phoneNumber, String firstName) {
        getContactTrie().insert(phoneNumber);
        getContactTrie().insert(firstName);
        GlobalContacts.INSTANCE.getContactTrie().insert(phoneNumber);
        GlobalContacts.INSTANCE.getContactTrie().insert(firstName);
    }

}
