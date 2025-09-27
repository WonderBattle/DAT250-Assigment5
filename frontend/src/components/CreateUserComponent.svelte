<!-- CreateUserComponent.svelte
     Purpose:
      - Create a User object with username and optional email
      - POST /users to create the user
      - Store user ID and username in localStorage for persistence
      - Dispatch a 'created' event with user data (so parent can update state)
-->
<script>
    // Event dispatcher so we can tell the parent (App) that a user was created
    import { createEventDispatcher } from "svelte";
    const dispatch = createEventDispatcher();

    // Pick the backend base URL depending on environment:
    // - During local dev (Vite) import.meta.env.DEV is true -> use full localhost URL so CORS/dev server works.
    // - In production (after building and serving from Spring Boot) import.meta.env.DEV is false -> use '' so fetch uses same-origin relative URLs.
    const API_BASE = import.meta.env.DEV ? 'http://localhost:8080' : ''; // API root used by fetch calls

    // Form state
    let username = "";
    let email = "";
    let message = "";

    // Called when user clicks "Create user"
    async function createUser() {
        message = "";
        const payload = { username, email };

        try {
            // POST /users
            const res = await fetch(`${API_BASE}/users`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                message = `Failed to create user (status ${res.status})`;
                return;
            }

            // Parse created user (backend returns the created user with id and username)
            const created = await res.json();

            // Save id and username to localStorage (used by other components)
            localStorage.setItem("userId", created.id);
            localStorage.setItem("userName", created.username);

            // Notify parent that a user was created (gives App the created object)
            // event.detail will be the created user object — App uses event.detail.id and event.detail.username
            dispatch("created", created);

            // Provide UI feedback and clear form
            message = `User created: ${created.username} (id ${created.id}). Saved to localStorage.`;
            username = "";
            email = "";
        } catch (err) {
            message = "Error creating user: " + err.message;
        }
    }
</script>

<section>
    <h2>Create User</h2>

    <!-- Accessibility: label 'for' must match input 'id' -->
    <div class="form">
        <div>
            <label for="username" class="label">Username</label>
            <input id="username" class="input" type="text" bind:value={username} placeholder="alice" />
        </div>

        <div>
            <label for="email" class="label">Email (optional)</label>
            <input id="email" class="input" type="email" bind:value={email} placeholder="alice@example.com" />
        </div>

        <div>
            <button class="btn btn-primary" on:click={createUser}>Create user</button>
        </div>
    </div>

    {#if message}
        <div class="message">{message}</div>
    {/if}
</section>

<style>
    /* Actual styles in app.css */
    .form { display: grid; gap: 8px; }
    .message { margin-top: 8px; }
</style>
