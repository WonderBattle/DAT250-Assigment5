package com.Assigment5.DAT250Assigment5.controllers;

import com.Assigment5.DAT250Assigment5.PollManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/polls")
@CrossOrigin
public class PollResultsController {

    @Autowired
    private PollManager pollManager;

    /**
     * Return aggregated vote counts for a poll.
     * Response: JSON object mapping voteOptionId -> count
     */
    @GetMapping("/{pollId}/results")
    public Map<Long, Integer> getPollResults(@PathVariable Long pollId) {
        return pollManager.getVoteCountsForPoll(pollId);
    }
}

