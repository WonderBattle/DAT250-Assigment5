package com.Assigment5.DAT250Assigment5.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    private String publishedAt;
    private String validUntil;

    @ManyToOne
    @JsonIgnoreProperties("createdPolls") // ignore back reference
    private User createdBy;                          // Poll has a creator

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("poll") // prevent infinite recursion
    private List<VoteOption> options = new ArrayList<>(); // Poll has options

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vote> votes = new ArrayList<>();            // Poll has votes

    protected Poll() {}

    // Constructor used by User.createPoll()
    public Poll(String question, User createdBy) {
        this.question = question;
        this.createdBy = createdBy;
    }

    /**
     *
     * Adds a new option to this Poll and returns the respective
     * VoteOption object with the given caption.
     * The value of the presentationOrder field gets determined
     * by the size of the currently existing VoteOptions for this Poll.
     * I.e. the first added VoteOption has presentationOrder=0, the secondly
     * registered VoteOption has presentationOrder=1 ans so on.
     */
    public VoteOption addVoteOption(String caption) {
        int order = options.size();            // <-- Determine presentationOrder
        VoteOption option = new VoteOption(caption, order, this);
        options.add(option);
        return option;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }
    public void setQuestion(String question) {
        this.question = question;
    }

    public String getPublishedAt() {
        return publishedAt;
    }
    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getValidUntil() {
        return validUntil;
    }
    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }

    // Relationship getters/setters

    public User getCreator() {
        return createdBy;
    }
    public void setCreator(User creator) {
        this.createdBy = creator;
    }

    public List<VoteOption> getVoteOptions() {
        return options;
    }
    public void setVoteOptions(List<VoteOption> voteOptions) {
        this.options = voteOptions;
    }

    @JsonIgnore
    public List<Vote> getVotes() {
        return votes;
    }
    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }
}