package com.Assigment5.DAT250Assigment5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String publishedAt;

    @ManyToOne
    @JsonIgnoreProperties("votes") // break cycle
    private User castBy;        // Vote is made by a User

    @ManyToOne
    @JsonIgnoreProperties("votes") // break cycle
    private VoteOption votesOn; // Vote is for an Option

    @ManyToOne
    private Poll poll;

    protected Vote() {}

    // Constructor used by User.voteFor()
    public Vote(User castBy, VoteOption votesOn) {
        this.castBy= castBy;
        this.votesOn = votesOn;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getPublishedAt() {
        return publishedAt;
    }
    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public User getUser() {
        return castBy;
    }
    public void setUser(User user) {
        this.castBy = user;
    }

    public VoteOption getVoteOption() {
        return votesOn;
    }
    public void setVoteOption(VoteOption voteOption) {
        this.votesOn = voteOption;
    }
}