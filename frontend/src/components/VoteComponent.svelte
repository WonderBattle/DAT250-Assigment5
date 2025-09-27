<!-- VoteComponent.svelte
     Purpose:
       - Load polls (GET /polls)
       - Load vote options (GET /voteoptions) and map them by poll id
       - Load votes (GET /votes) to compute counts per voteOption id
       - Allow user to submit a vote (POST /votes with { user: { id }, voteOption: { id } })
-->
<script>
    import { onMount } from "svelte";                 // run code after component mounts

    // Receive current user's id as a prop from App.svelte (reactive)
    // We default to empty string when no user is selected.
    export let currentUserId = ""; // string | "" when no user

    // Pick the backend base URL depending on environment:
    // - During local dev (Vite) import.meta.env.DEV is true -> use full localhost URL so CORS/dev server works.
    // - In production (after building and serving from Spring Boot) import.meta.env.DEV is false -> use '' so fetch uses same-origin relative URLs.
    const API_BASE = import.meta.env.DEV ? 'http://localhost:8080' : ''; // API root used by fetch calls

    // Data state (lists fetched from backend)
    let polls = [];                                   // list of polls from backend
    let voteOptions = [];                             // global list from /voteoptions
    let votes = [];                                   // global list from /votes
    let message = "";                                 // UI feedback
    let loading = false;                              // loading state

    // Helper maps derived from the lists
    let optionsByPoll = {};                           // poll.id -> [voteOptions]
    let counts = {};                                  // voteOption.id -> number of votes

    // Load all data when component mounts (runs once)
    onMount(async () => {
        await loadAll();                              // fetch polls, options, votes
    });

    // Helper: fetch polls, voteoptions, votes and compute maps
    async function loadAll() {
        loading = true;                               // set loading spinner/flag
        message = "";                                 // clear message
        try {
            // 1) Load polls
            const resPolls = await fetch(`${API_BASE}/polls`);
            polls = resPolls.ok ? await resPolls.json() : [];

            // 2) Load options
            const resVO = await fetch(`${API_BASE}/voteoptions`);
            voteOptions = resVO.ok ? await resVO.json() : [];

            // Build pollId -> [options] mapping
            optionsByPoll = {};
            for (const vo of voteOptions) {                                   // Loop through each vote option
                const pid = vo.poll && vo.poll.id ? vo.poll.id : null;        // Extract poll ID safely
                if (!pid) continue;                                           // Skip options without a poll (orphans)
                if (!optionsByPoll[pid]) optionsByPoll[pid] = [];             // Create array if poll ID doesn't exist yet
                optionsByPoll[pid].push(vo);                                  // Add vote option to the array for this poll
            }

            // 3) Load votes
            const resVotes = await fetch(`${API_BASE}/votes`);
            votes = resVotes.ok ? await resVotes.json() : [];

            // Count votes per option id
            counts = {};
            for (const v of votes) {
                if (v.voteOption && v.voteOption.id) {
                    counts[v.voteOption.id] = (counts[v.voteOption.id] || 0) + 1;
                }
            }
        } catch (err) {
            console.error(err);
            message = "Failed to load data: " + err.message;
        } finally {
            loading = false;                          // clear loading flag
        }
    }

    // Submit a vote for the given voteOption id
    async function submitVote(voteOptionId) {
        message = "";                                 // clear any previous message

        // Using currentUserId (prop) instead of reading localStorage ----
        if (!currentUserId) {                         // must create or select a user first
            message = "Create or choose a user first (use Create User).";
            return;
        }
        if (!voteOptionId) {
            message = "Invalid option.";
            return;
        }

        // Build payload to match backend Vote model
        const payload = {
            user: { id: currentUserId },              // link vote to active user
            voteOption: { id: voteOptionId }          // link vote to option
        };

        try {
            // POST /votes to register the vote
            const res = await fetch(`${API_BASE}/votes`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {                            // error handling
                throw new Error(`Vote failed (status ${res.status})`);
            }

            await loadAll();                          // refresh counts after voting
            message = "Vote submitted!";              // success feedback
        } catch (err) {
            console.error(err);
            message = "Error submitting vote: " + err.message;
        }
    }

    // Helper: nice date display (handles invalid / null safely)
    function fmt(dateStr) {
        try { return new Date(dateStr).toLocaleString(); } catch { return dateStr || "—"; }
    }

    // Unvote: remove the current user's vote for a specific option
    async function unvote(pollId, optionId) {
        try {
            if (!currentUserId) {                     // must have an active user
                message = "Create or choose a user first (use Create User).";
                return;
            }

            // 1) Find this user's vote for the given option (from the loaded votes)
            const myVote = votes.find(v => v.user?.id == currentUserId && v.voteOption?.id == optionId);

            if (!myVote) {                            // user hasn't voted this option
                message = "You haven't voted on this option yet.";
                return;
            }

            // 2) DELETE /votes/{voteId}
            const res = await fetch(`${API_BASE}/votes/${myVote.id}`, {
                method: "DELETE"
            });

            if (!res.ok) {
                throw new Error("Unvote failed");
            }

            await loadAll(); // refresh polls and counts after unvoting
            message = "Vote removed!";
        } catch (err) {
            console.error("Error unvoting:", err);
            message = "Error unvoting: " + err.message;
        }
    }

    // Delete poll — client-side guard checks the poll's creator matches currentUserId
    async function deletePoll(pollId) {
        try {
            // 1) Find poll locally to check creator
            const poll = polls.find(p => p.id === pollId);

            // If poll or creator is missing, just try delete (safe fallback)
            if (poll && poll.creator && poll.creator.id !== currentUserId) {
                // Client-side block: user is not the creator of this poll
                message = "You can only delete polls you created.";
                return;
            }

            // 2) Proceed to delete on backend
            const res = await fetch(`${API_BASE}/polls/${pollId}`, { method: "DELETE" });
            if (!res.ok) {
                throw new Error(`Delete failed (status ${res.status})`);
            }

            // 3) Refresh list after deletion
            await loadAll();
            message = "Poll deleted.";
        } catch (err) {
            console.error("Error deleting poll:", err);
            message = "Error deleting poll: " + err.message;
        }
    }

</script>


<!-- Card wrapper for the list + actions -->
<section class="card">
    <h2>Available Polls</h2>

    <!-- Top action row: Refresh and hints -->
    <div class="mt-12 mb-12" style="display:flex; gap:10px; align-items:center;">
        <button class="btn btn-secondary" on:click={loadAll}>Refresh</button>
        <span class="help-text">Tip: Create a user first, then create a poll and vote.</span>
    </div>

    <!-- Loading indicator -->
    {#if loading}
        <div class="message">Loading…</div>
    {/if}

    <!-- Feedback message -->
    {#if message}
        <div class="message {message.startsWith('Error') || message.startsWith('Failed') ? 'message-error' : 'message-success'}">
            {message}
        </div>
    {/if}

    <!-- Empty state -->
    {#if !loading && polls.length === 0}
        <div class="message">No polls available.</div>
    {/if}

    <!-- Poll cards grid -->
    <div class="polls-grid">
        {#each polls as poll}
            <article class="poll-card">
                <!-- Question -->
                <h3 style="margin: 0 0 6px 0;">{poll.question}</h3>

                {#if poll.creator && poll.creator.id === currentUserId}
                    <button class="delete-btn" on:click={() => deletePoll(poll.id)}>Delete Poll</button>
                {/if}

                <!-- Meta -->
                <div class="meta">
                    Valid until: {fmt(poll.validUntil)}
                    {#if poll.creator && poll.creator.username}
                        • Created by: {poll.creator.username}
                    {/if}
                </div>

                <!-- Options list -->
                {#if optionsByPoll[poll.id] && optionsByPoll[poll.id].length > 0}
                    <ul class="options-list">
                        {#each optionsByPoll[poll.id] as opt}
                            <li class="option-item">
                                <!-- Caption + count pill -->
                                <div>
                                    <strong>{opt.caption}</strong>
                                    <span class="count-badge">{counts[opt.id] || 0} vote(s)</span>
                                </div>
                                <!-- Action buttons grouped -->
                                <div class="option-actions">
                                    <button class="btn btn-primary" on:click={() => submitVote(opt.id)}>Vote</button>
                                    <button class="unvote-btn" on:click={() => unvote(poll.id, opt.id)}>Unvote</button>
                                </div>
                            </li>
                        {/each}
                    </ul>
                {:else}
                    <!-- No options registered for this poll yet -->
                    <div class="help-text">No vote options registered for this poll yet.</div>
                {/if}
            </article>
        {/each}
    </div>
</section>
