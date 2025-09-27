<script>
    // Import the three working components
    import CreateUser from "./components/CreateUserComponent.svelte"; // component that creates a user
    import CreatePoll from "./components/CreatePollComponent.svelte"; // component that creates polls
    import VotePoll from "./components/VoteComponent.svelte";         // component that shows polls and allows voting
    import { onMount } from "svelte";                     // lifecycle helper

    // Pick the backend base URL depending on environment:
    // - During local dev (Vite) import.meta.env.DEV is true -> use full localhost URL so CORS/dev server works.
    // - In production (after building and serving from Spring Boot) import.meta.env.DEV is false -> use '' so fetch uses same-origin relative URLs.
    const API_BASE = import.meta.env.DEV ? 'http://localhost:8080' : ''; // API root used by fetch calls


    // --- Users list state -------------------------------------------------
    let users = [];                 // in-memory list of users fetched from backend
    let isLoadingUsers = false;     // loading flag

    // Fetch users from backend and store in `users`
    async function loadUsers() {
        isLoadingUsers = true; // indicate we started loading
        try {
            const res = await fetch(`${API_BASE}/users`); // GET /users
            users = res.ok ? await res.json() : [];                 // parse JSON or set to empty
        } catch (err) {
            console.error("Failed to load users:", err);
            users = [];                                             // on error, empty list
        } finally {
            isLoadingUsers = false;                                 // done loading
        }
    }

    // Load user list once when the app mounts
    onMount(async () => {
        await loadUsers();
    });


    // --- App view/navigation state ----------------------------------------
    // which main view is visible: "user" | "create" | "vote"
    let view = "vote";

    // refresh key used as Svelte `key` to force re-mount VotePoll when incremented
    let refreshKey = 0;


    // --- Current user state ----------------------------------------------
    // Read both userId and userName from localStorage. Only create a `currentUser`
    // object when BOTH values exist. This prevents showing "Hello, " when name is missing.
    const storedId = localStorage.getItem("userId");      // maybe null
    const storedName = localStorage.getItem("userName");  // maybe null

    // currentUser is either { id, username } or null
    let currentUser = storedId && storedName
        ? { id: Number(storedId), username: storedName }
        : null;

    // The selected user id in the <select>. Keep this synchronized with currentUser.
    let selectedUserId = currentUser ? currentUser.id : null;


    // --- Poll created handler: used when CreatePoll dispatches 'created' ----
    function onPollCreated(event) {
        // Switch back to the vote view so the user sees the newly created poll
        view = "vote";
        // Bump the refreshKey so VotePoll component remounts and reloads data
        refreshKey += 1;
    }


    // --- Handler for when CreateUser dispatches 'created' -------------------
    // We expect the CreateUser component to dispatch the *created user object*
    // with shape: { id: "uuid", username: "alice", ... }.
    function onUserCreated(event) {
        const created = event.detail; // user object from CreateUser
        if (!created || !created.id) return; // defensive: ignore malformed events

        // Save to localStorage using consistent key 'userName' and 'userId'
        localStorage.setItem("userId", created.id);
        const name = created.username ?? created.name ?? "";  //safely extract a username or name from an object with fallback values.
        localStorage.setItem("userName", name);

        // Update app state to select this user immediately
        currentUser = { id: Number(created.id), username: name };
        selectedUserId = Number(created.id);

        // reload users so the select includes the new user
        loadUsers();

        // go to vote view so user can create polls or vote immediately
        view = "vote";
        // also bump refreshKey so VotePoll re-mounts and sees any changes
        refreshKey += 1;
    }


    // --- Delete currently active user -------
    async function onUserDeleted() {
        if (!currentUser) return; // nothing to delete

        // ✅ Store the ID before clearing currentUser
        const userIdToDelete = currentUser.id;

        try {
            await fetch(`${API_BASE}/users/${currentUser.id}`, { method: "DELETE" });

        } catch (err) {
            // if backend delete fails, we continue removing the client-side session
            console.warn("Backend user deletion failed (continuing client cleanup):", err);
        }

        // Clear local session and UI state
        localStorage.removeItem("userId");
        localStorage.removeItem("userName");

        // ✅ Remove the user from the local users list using the stored ID
        users = users.filter(user => user.id !== userIdToDelete);

        currentUser = null;
        selectedUserId = "";

        // Refresh user list so the select updates
        await loadUsers();

        // Also force refresh the VotePoll component
        refreshKey += 1;
    }


    // --- Switch user when a different id is selected in the dropdown -------
    function changeUserById(id) {
        // find user object from loaded list
        //const u = users.find(x => x.id === id);

        const numericId = Number(id);
        const u = users.find(x => x.id === numericId);

        if (!u) return; // ignore invalid id

        // set current user and persist
        selectedUserId = numericId;

        currentUser = { id: u.id, username: u.username ?? u.name ?? "" };
        localStorage.setItem("userId", String(u.id)); // stored as string in localStorage
        localStorage.setItem("userName", currentUser.username);

        //force VotePoll to refresh (so counts reflect this user's votes)
        refreshKey += 1;
    }
</script>

<!--
  App UI:
  - header shows app title and user greeting (only when username exists)
  - a select is shown when there are users to choose from
  - navigation switches between CreateUser, CreatePoll and VotePoll
-->
<main>
    <!-- Header bar with title -->
    <header class="app-header">
        <!-- App title -->
        <h1 class="app-title">Voting App</h1>

        <!-- Greeting: show only when we actually have a username -->
        {#if currentUser && currentUser.username}
            <p class="user-greeting">Hello, {currentUser.username}!</p>
            <!-- allow deleting the active session/user -->
            <button class="delete-btn" on:click={onUserDeleted}>Delete User</button>
        {:else}
            <!-- Friendly instruction when no user selected or available -->
            <p class="user-greeting">First, choose or create a user</p>
        {/if}

        <!-- Show a "Switch user" select only when the backend returned >= 1 users -->
        {#if users.length > 0}
            <div style="margin-top:8px;">
                <!-- label is associated via for="userSelect" to help a11y -->
                <label for="userSelect" class="label">Switch User:</label>

                <!--
                  We bind the select value to selectedUserId and handle change by
                  looking up the full user object and calling changeUserById(id).
                -->
                <select
                        id="userSelect"
                        class="input"
                        bind:value={selectedUserId}
                        on:change={(e) => changeUserById(e.target.value)}
                >
                    <!-- value "" ensures it is not a real user id -->
                    <option value="" disabled selected={selectedUserId === ""}>Choose user</option>

                    <!-- list users returned by the backend; show username -->
                    {#each users as user}
                        <option value={user.id}>{user.username ?? user.name ?? "(no name)"}</option>
                    {/each}
                </select>
            </div>
        {/if}
    </header>

    <!-- Navigation buttons to switch views (no redundant role attribute) -->
    <nav class="top-nav">
        <button class="nav-btn" aria-pressed={view === 'user'} on:click={() => view = 'user'}>Create User</button>
        <button class="nav-btn" aria-pressed={view === 'create'} on:click={() => view = 'create'}>Create Poll</button>
        <button class="nav-btn" aria-pressed={view === 'vote'} on:click={() => view = 'vote'}>Vote / View Polls</button>
    </nav>

    <!-- Main content centered -->
    <div class="container">
        <section class="card">
            {#if view === 'user'}
                <CreateUser on:created={onUserCreated} />
            {:else if view === 'create'}
                <CreatePoll on:created={onPollCreated} />
            {:else}
                <VotePoll key={refreshKey} currentUserId={currentUser ? currentUser.id : ""} />
            {/if}
        </section>
    </div>
</main>

<style>
    /* Keep font-smoothing for a nicer look. All visual styling stays in app.css */
    main {
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
    }

    /* Visual indicator for active nav button */
    .top-nav .nav-btn[aria-pressed="true"] {
        background: var(--primary-blue);
        color: #fff;
        border-color: var(--primary-blue);
    }
</style>
