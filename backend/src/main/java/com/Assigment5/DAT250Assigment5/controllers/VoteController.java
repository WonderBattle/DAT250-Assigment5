package com.Assigment5.DAT250Assigment5.controllers;

import com.Assigment5.DAT250Assigment5.PollManager;
import com.Assigment5.DAT250Assigment5.model.Vote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/votes")
@CrossOrigin
@Tag(name = "Votes", description = "Vote management APIs") // Step 6: API Documentation
public class VoteController {

    @Autowired
    private PollManager pollManager;

    @Operation(summary = "Create a new vote", description = "Creates a new vote and returns it") // Step 6: API Documentation
    @PostMapping
    public Vote createVote(@RequestBody Vote vote) {
        return pollManager.createVote(vote);
    }

    @Operation(summary = "Remove a vote", description = "Deletes a user's vote for a given vote option")
    @DeleteMapping("/{voteId}")
    public void deleteVote(@PathVariable Long voteId) {
        pollManager.deleteVote(voteId);
    }

    @Operation(summary = "Get all votes", description = "Returns a list of all votes") // Step 6: API Documentation
    @GetMapping
    public List<Vote> getAllVotes() {
        return pollManager.getAllVotes();
    }
}
