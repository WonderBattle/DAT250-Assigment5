package com.Assigment5.DAT250Assigment5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class VoteOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caption;
    private int presentationOrder;

    @ManyToOne
    @JsonIgnoreProperties("voteOptions") // avoid loop
    private Poll poll; // VoteOption belongs to a Poll

    @OneToMany(mappedBy = "votesOn", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("voteOption") // avoid loop
    private Set<Vote> votes = new LinkedHashSet<>();

    protected VoteOption() {}

    // Constructor used by Poll.addVoteOption()
    public VoteOption(String caption, int order, Poll poll) {
        this.caption = caption;
        this.presentationOrder = order;
        this.poll = poll;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getCaption() {
        return caption;
    }
    public void setCaption(String caption) {
        this.caption = caption;
    }

    public int getPresentationOrder() {
        return presentationOrder;
    }
    public void setPresentationOrder(int presentationOrder) {
        this.presentationOrder = presentationOrder;
    }


    public Poll getPoll() {
        return poll;
    }
    public void setPoll(Poll poll) {
        this.poll = poll;
    }
}