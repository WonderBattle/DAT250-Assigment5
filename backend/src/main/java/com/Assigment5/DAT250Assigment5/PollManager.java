package com.Assigment5.DAT250Assigment5;

import com.Assigment5.DAT250Assigment5.model.User;
import com.Assigment5.DAT250Assigment5.model.Poll;
import com.Assigment5.DAT250Assigment5.model.Vote;
import com.Assigment5.DAT250Assigment5.model.VoteOption;
import org.springframework.stereotype.Component;
import java.util.*;
import redis.clients.jedis.JedisPooled;

@Component
public class PollManager {
    private final Map<Long, User> users = new HashMap<>(); // key: user id
    private final Map<Long, Poll> polls = new HashMap<>(); // key: poll id
    private final Map<Long, Vote> votes = new HashMap<>(); // key: vote id
    private final Map<Long, VoteOption> voteOptions = new HashMap<>(); // key vote option id

    private long userIdSeq = 1;
    private long pollIdSeq = 1;
    private long voteIdSeq = 1;
    private long voteOptionIdSeq = 1;

    //private final JedisPooled jedis = new JedisPooled("localhost", 6379);
    private final JedisPooled jedis;

    public PollManager() {
        JedisPooled tmp = null;
        try {
            tmp = new JedisPooled("localhost", 6379);
            tmp.ping(); // check connectivity
        } catch (Exception e) {
            tmp = null;
            System.err.println("⚠ Redis not available — running without caching. Reason: " + e.getMessage());
        }
        this.jedis = tmp;
    }

    // User methods
    public User createUser(User user) {
        // Hibernate will assign ID on persist
        //String id = UUID.randomUUID().toString();  // Generate unique ID using UUID
        //user.setId(id);  // Set the generated ID on the user object
        user.setId(userIdSeq++);  // assign next id

        users.put(user.getId(), user); // Store user in the users map
        return user;  // Return the created user with ID
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());   // Return copy of all users as ArrayList
    }

    public User getUser(Long id) {
        return users.get(id); // Return user from map or null if not found
    }

    public void deleteUser(Long id) {
        User user = users.get(id);
        if (user != null) {
            // Cascade delete: remove polls created by this user
            for (Poll poll : new ArrayList<>(user.getCreatedPolls())) {
                deletePoll(poll.getId());
            }

            // Cascade delete: remove votes cast by this user
            for (Vote vote : new ArrayList<>(user.getVotes())) {
                deleteVote(vote.getId());
            }

            // Finally remove the user itself
            users.remove(id);
        }
    }

    // Helper method to find users by ID
    public User findUserById(Long userId) {
        return users.get(userId);  // Return user from map or null if not found
    }

    // Poll methods
    public Poll createPoll(Poll poll) {
        // Hibernate will assign ID on persist
        //String id = UUID.randomUUID().toString();  // Generate unique ID using UUID
        //poll.setId(id);   // Set the generated ID on the poll object
        poll.setId(pollIdSeq++);

        // Look up the full user object if only ID is provided
        if (poll.getCreator() != null && poll.getCreator().getId() != null) {
            User fullUser = users.get(poll.getCreator().getId());  // Get complete user object from storage
            if (fullUser != null) {
                poll.setCreator(fullUser); // Replace with complete user object (maintains relationship integrity)
                fullUser.getCreatedPolls().add(poll); // Link poll directly to the correct user
            }
        }

        // ✅ PROCESS VOTEOPTIONS BUT DON'T RETURN THEM (to avoid duplicates)
        if (poll.getVoteOptions() != null && !poll.getVoteOptions().isEmpty()) {
            for (VoteOption option : poll.getVoteOptions()) {
                // Set the poll reference for the options that came in the request
                option.setPoll(poll);
                // This ensures relationships are maintained even though we won't return these options
            }

            // ✅ CRITICAL: CLEAR THE VOTEOPTIONS FROM THE POLL RESPONSE
            // This prevents the null-ID options from appearing in the JSON
            poll.setVoteOptions(new ArrayList<>());
        }

        polls.put(poll.getId(), poll); // Store poll in the polls map

        return poll;
    }

    public List<Poll> getAllPolls() {
        return new ArrayList<>(polls.values());  // Return copy of all polls as ArrayList
    }

    public Poll getPoll(Long id) {
        return polls.get(id);  // Return poll from map or null if not found
    }

    public void deletePoll(Long id) {
        Poll poll = polls.get(id);  // Get the poll to be deleted
        if (poll != null) {
            // Remove poll from creator's created polls (maintain relationship integrity)
            if (poll.getCreator() != null) {
                poll.getCreator().getCreatedPolls().remove(poll);  // Remove from user's created polls
            }

            // Delete associated votes (cascade delete)
            deleteVotesByPollId(id);

            // Delete associated vote options (cascade delete)
            deleteVoteOptionsByPollId(id);

            // Finally remove the poll itself
            polls.remove(id);   // Remove poll from main storage
        }
    }

    public void deleteVoteOptionsByPollId(Long pollId) {
        // Remove vote options associated with a poll when it's deleted
        voteOptions.values().removeIf(voteOption ->  // Iterate through all vote options
                voteOption.getPoll() != null &&  // Check if vote option has a poll reference
                        voteOption.getPoll().getId().equals(pollId)  // Check if poll ID matches
        );
    }

    public void deleteVotesByPollId(Long pollId) {
        // Remove votes associated with a poll when it's deleted
        votes.values().removeIf(vote ->   // Iterate through all votes
                vote.getVoteOption() != null &&  // Check if vote has a vote option reference
                        vote.getVoteOption().getPoll() != null &&  // Check if vote option has a poll reference
                        vote.getVoteOption().getPoll().getId().equals(pollId)  // Check if poll ID matches
        );
    }

    // VoteOption methods (for poll options)
    public VoteOption createVoteOption(VoteOption voteOption) {
        // Hibernate will assign ID on persist
        //String id = UUID.randomUUID().toString();  // Generate unique ID using UUID
        //voteOption.setId(id);  // Set the generated ID on the vote option object
        voteOption.setId(voteOptionIdSeq++);

        if (voteOption.getPoll() != null && voteOption.getPoll().getId() != null) {
            Poll poll = polls.get(voteOption.getPoll().getId());
            if (poll != null) {
                // Set the full poll object (not just the reference)
                voteOption.setPoll(poll);
                // Add this vote option to the poll's collection
                if (poll.getVoteOptions() == null) {
                    poll.setVoteOptions(new ArrayList<>());
                }
                poll.getVoteOptions().add(voteOption);
            }
        }

        voteOptions.put(voteOption.getId(), voteOption);  // Store vote option in the voteOptions map
        return voteOption;  // Return the created vote option with ID
    }

    public List<VoteOption> getAllVoteOptions() {
        return new ArrayList<>(voteOptions.values());  // Return copy of all vote options as ArrayList
    }

    // Vote methods
    public Vote createVote(Vote vote) {
        // Hibernate will assign ID on persist
        //String id = UUID.randomUUID().toString();  // Generate unique ID using UUID
        //vote.setId(id);  // Set the generated ID on the vote object
        vote.setId(voteIdSeq++);

        vote.setPublishedAt(String.valueOf(System.currentTimeMillis()));  // Set current timestamp

        // PROPERLY SET USER RELATIONSHIP (resolve user reference)
        if (vote.getUser() != null && vote.getUser().getId() != null) {
            User user = users.get(vote.getUser().getId()); // Get complete user object from storage
            if (user != null) {
                vote.setUser(user); // Replace with full user object
                user.getVotes().add(vote); // Add this vote to user's votes list (bidirectional relationship)
            }
        }

        // PROPERLY SET VOTEOPTION RELATIONSHIP (resolve vote option reference)
        if (vote.getVoteOption() != null && vote.getVoteOption().getId() != null) {
            VoteOption voteOption = voteOptions.get(vote.getVoteOption().getId());  // Get complete vote option
            if (voteOption != null) {
                vote.setVoteOption(voteOption); // Replace with full voteOption object
                // The vote is properly connected to the voteOption and its poll
            }
        }

        // Invalidate cache for this poll (Assigment 5) — safe if Redis isn't available
        if (vote.getVoteOption() != null && vote.getVoteOption().getPoll() != null) {
            Long pollId = vote.getVoteOption().getPoll().getId();
            String redisKey = "poll:" + pollId + ":votes";
            if (jedis != null) {
                try {
                    jedis.del(redisKey);
                } catch (Exception e) {
                    // ignore Redis errors in tests/CI
                    System.err.println("Warning: Redis DEL failed: " + e.getMessage());
                }
            }
        }

        votes.put(vote.getId(), vote); // Store vote in the votes map
        return vote;
    }

    public void deleteVote(Long voteId) {
        Vote vote = votes.get(voteId);  // find the vote first
        if (vote != null) {
            // maintain bidirectional relationship with user
            if (vote.getUser() != null) {
                vote.getUser().getVotes().remove(vote);
            }
            // optional: you could also clean up from VoteOption if needed

            votes.remove(voteId);  // remove from map
        }
    }

    public List<Vote> getAllVotes() {
        return new ArrayList<>(votes.values());  // Return copy of all votes as ArrayList
    }

    //ASSIGMENT 5

    // Get aggregated votes (from cache or compute)
    public Map<Long, Integer> getVoteCountsForPoll(Long pollId) {
        String redisKey = "poll:" + pollId + ":votes";

        // 1. Try cache if Redis is available
        if (jedis != null) {
            try {
                if (jedis.exists(redisKey)) {
                    System.out.println("Fetching aggregated votes for poll " + pollId + " from Redis cache...");
                    Map<String, String> cached = jedis.hgetAll(redisKey);

                    Map<Long, Integer> result = new HashMap<>();
                    cached.forEach((k, v) -> result.put(Long.valueOf(k), Integer.valueOf(v)));
                    return result;
                }
            } catch (Exception e) {
                // If Redis read fails, fallback to in-memory
                System.err.println("Warning: Redis read failed — computing in-memory. Reason: " + e.getMessage());
            }
        }


        // 2. Otherwise, compute manually
        System.out.println("Computing aggregated votes for poll " + pollId + " from in-memory store...");
        Map<Long, Integer> counts = new HashMap<>();

        for (Vote vote : votes.values()) {
            if (vote.getVoteOption() != null
                    && vote.getVoteOption().getPoll() != null
                    && vote.getVoteOption().getPoll().getId().equals(pollId)) {
                Long optionId = vote.getVoteOption().getId();
                counts.put(optionId, counts.getOrDefault(optionId, 0) + 1);
            }
        }

        // 3. Store in Redis for next time
        Map<String, String> redisHash = new HashMap<>();
        counts.forEach((k, v) -> redisHash.put(String.valueOf(k), String.valueOf(v)));

        if (!redisHash.isEmpty() && jedis != null) {
            try {
                jedis.hset(redisKey, redisHash);
                jedis.expire(redisKey, 60); // cache expires in 60 seconds
            } catch (Exception e) {
                System.err.println("Warning: Redis write failed; continuing without caching. Reason: " + e.getMessage());
            }
        }


        return counts;
    }



}
