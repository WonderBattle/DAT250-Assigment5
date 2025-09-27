<!-- CreatePollComponent.svelte
     Purpose:
      - Build a Poll object according to your backend model
      - POST /polls to create the poll
      - Immediately POST /voteoptions for each option with { caption, presentationOrder, poll: { id: pollId } }
      - Dispatch a 'created' event when done (so parent can refresh lists)
-->
<script>
    import { createEventDispatcher } from "svelte";     // to notify parent when poll is created
    const dispatch = createEventDispatcher();           // dispatcher instance

    // Pick the backend base URL depending on environment:
    // - During local dev (Vite) import.meta.env.DEV is true -> use full localhost URL so CORS/dev server works.
    // - In production (after building and serving from Spring Boot) import.meta.env.DEV is false -> use '' so fetch uses same-origin relative URLs.
    const API_BASE = import.meta.env.DEV ? 'http://localhost:8080' : ''; // API root used by fetch calls

    // Form state
    let question = "";                                  // poll question
    let validUntil = "";                                // yyyy-mm-dd from <input type="date">
    let options = ["", ""];                             // start with two empty option fields
    let message = "";                                   // UI feedback message
    let isCreating = false;                             // loading state

    // Current user id must exist (use CreateUser first)
    let userId = localStorage.getItem("userId");

    // Add a new empty option field
    function addOption() {
        options = [...options, ""];                     // append empty string
    }

    // Remove an option by index (keep at least two)
    function removeOption(index) {
        if (options.length > 2) {                       // enforce minimum 2 options
            options = options.filter((_, i) => i !== index);
            //This creates a new array with the elements that have different index. If i !== index, add it to the new array
        }
    }

    // Update an option value
    function updateOption(index, value) {
        options = options.map((o, i) => (i === index ? value : o));
        //This creates a new map after apply the function
        // o, actual element; i currently index being processed
        // If i === index, o = value, else: o=o (keep unchanged)
    }

    // Create poll then register each option at /voteoptions
    async function createPoll() {
        message = "";                                   // clear any previous message
        if (!userId) {                                  // must have created a user first
            message = "You must create a user first (use Create User).";
            return;
        }
        //This ensures the question isn't just empty spaces, and that there are at least 2 options with actual content (not just spaces)
        if (!question.trim() || options.filter(o => o.trim()).length < 2) {
            message = "Provide a question and at least 2 options.";
            return;
        }

        isCreating = true;                              // set loading flag

        // Build payload matching backend Poll model
        const pollPayload = {
            question: question.trim(),                  // text of the question
            publishedAt: new Date().toISOString(),      // timestamp now
            validUntil: validUntil                      // if user set a date, use it, else +7 days
                ? new Date(validUntil).toISOString()    // if validUntil has a value, converts the user-provided date to ISO string format
                : new Date(Date.now() + 7*24*3600*1000).toISOString(),  // if validUntil don't have a value, creates a date 7 days from now
            creator: { id: userId },                    // backend resolves to full user object
            voteOptions: options                        // embed options (caption + order)
                .map(o => o.trim())                     // Remove whitespace from each option
                .filter(o => o.length > 0)              // Remove any empty strings (after trimming)
                .map((caption, idx) => ({
                    caption,                            //The option text (using shorthand property name)
                    presentationOrder: idx + 1          //(converts 0-based index to 1-based order)
                }))
        };

        try {
            // 1) Create the poll
            const res = await fetch(`${API_BASE}/polls`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(pollPayload)   //converts to a JSON string that can be sent to the server.
            });

            if (!res.ok) {                              // handle non-2xx
                throw new Error(`Create poll failed: ${res.status}`);
            }

            const createdPoll = await res.json();       // poll with generated id

            // 2) Register each vote option at /voteoptions so they get their own ids

            const createdVoteOptions = [];              // collect created options
            for (const vo of pollPayload.voteOptions) {
                const voPayload = {
                    caption: vo.caption,                // option text
                    presentationOrder: vo.presentationOrder, // original order
                    poll: {id: createdPoll.id}        // link option to the poll
                };

                const resVo = await fetch(`${API_BASE}/voteoptions`, {
                    method: "POST",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify(voPayload)
                });

                if (!resVo.ok) {                        // warn but continue
                    console.warn("Failed to create voteoption:", voPayload, resVo.status);
                    continue;
                }
                const createdVo = await resVo.json();   // store created option (with id)
                createdVoteOptions.push(createdVo);
            }



            // 3) Success feedback + clear form + notify parent
            //message = `Poll created (id: ${createdPoll.id}). ${createdVoteOptions.length} option(s) registered.`;
            message = `Poll created (id: ${createdPoll.id}). ${createdPoll.voteOptions.length} option(s) registered.`;
            question = "";                              // reset fields
            validUntil = "";
            options = ["", ""];

            /*
            dispatch("created", {                       // let <App/> or parent know
                pollId: createdPoll.id,
                voteOptions: createdVoteOptions
            });
            */
            dispatch("created", createdPoll);
        } catch (err) {
            console.error(err);
            message = "Error creating poll: " + err.message;
        } finally {
            isCreating = false;                         // clear loading state
        }
    }
</script>

<!-- Card wrapper for the form -->
<section class="card">
    <h2>Create New Poll</h2>

    <!-- Grid form for aligned fields -->
    <div class="form">

        <div>  <!-- Question -->
            <label class="label">Question *</label>
            <input
                    class="input"
                    type="text"
                    bind:value={question}
                    placeholder="What's your question?"
            />
        </div>

        <div>  <!-- Valid until -->
            <label class="label">Valid until (optional)</label>
            <input
                    class="input"
                    type="date"
                    bind:value={validUntil}
            />
            <div class="help-text">Leave empty to set 7 days from now automatically.</div>
        </div>

        <div>  <!-- Options block -->
            <label class="label">Options *</label>

            <!-- List of option inputs -->
            <div class="form" style="gap:10px;">
                {#each options as opt, idx}
                    <div class="option-item">
                        <!-- Option input field -->
                        <input
                                class="input"
                                type="text"
                                bind:value={options[idx]}
                                on:input={(e) => updateOption(idx, e.target.value)}
                                placeholder={`Option ${idx+1}`}
                        />
                        <!-- Remove button (shown if > 2 options) -->
                        {#if options.length > 2}
                            <button class="icon-btn" on:click={() => removeOption(idx)} title="Remove option">×</button>
                        {/if}
                    </div>
                {/each}
            </div>

            <!-- Add option button -->
            <div class="mt-12">
                <button class="btn btn-secondary" on:click={addOption}>+ Add option</button>
            </div>
        </div>

        <!-- Submit button -->
        <div class="mt-16">
            <button class="btn btn-primary btn-block" on:click={createPoll} disabled={isCreating}>
                {isCreating ? "Creating..." : "Create Poll"}
            </button>
        </div>

        <!-- Feedback message (green or red) -->
        {#if message}
            <div class="message {message.startsWith('Error') ? 'message-error' : 'message-success'}">
                {message}
            </div>
        {/if}
    </div>
</section>
