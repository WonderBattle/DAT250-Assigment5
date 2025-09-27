package com.Assigment5.DAT250Assigment5;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PollAppIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCompleteAssignmentScenario() throws Exception {
        // This test will follow the exact same scenario as the manual test
        System.out.println("=== Starting Complete Assignment Scenario Test ===");

        // Step 1: List all users (empty at first)
        ResponseEntity<String> response = restTemplate.getForEntity("/users", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().equals("[]") || response.getBody().contains("[]"));
        System.out.println("✓ Step 1: Empty users list");

        // Step 2: Create User 1 (Alice)
        String aliceJson = """
            {
                "username": "alice",
                "email": "alice@example.com"
            }
            """;
        ResponseEntity<String> aliceResponse = restTemplate.postForEntity(
                "/users",
                new HttpEntity<>(aliceJson, createJsonHeaders()),
                String.class
        );
        assertEquals(HttpStatus.OK, aliceResponse.getStatusCode());
        JsonNode alice = objectMapper.readTree(aliceResponse.getBody());
        String aliceId = alice.get("id").asText();
        System.out.println("✓ Step 2: Created Alice with ID: " + aliceId);

        // Step 3: List all users (should show Alice)
        response = restTemplate.getForEntity("/users", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("alice"));
        System.out.println("✓ Step 3: Users list shows Alice");

        // Step 4: Create User 2 (Bob)
        String bobJson = """
            {
                "username": "bob",
                "email": "bob@example.com"
            }
            """;
        ResponseEntity<String> bobResponse = restTemplate.postForEntity(
                "/users",
                new HttpEntity<>(bobJson, createJsonHeaders()),
                String.class
        );
        assertEquals(HttpStatus.OK, bobResponse.getStatusCode());
        JsonNode bob = objectMapper.readTree(bobResponse.getBody());
        String bobId = bob.get("id").asText();
        System.out.println("✓ Step 4: Created Bob with ID: " + bobId);

        // Step 5: List all users again (should show both users)
        response = restTemplate.getForEntity("/users", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("alice"));
        assertTrue(response.getBody().contains("bob"));
        System.out.println("✓ Step 5: Users list shows both Alice and Bob");

        // Step 6: Alice creates a poll
        String pollJson = String.format("""
            {
                "question": "What's your favorite color?",
                "publishedAt": "2024-01-15T10:00:00Z",
                "validUntil": "2024-01-22T10:00:00Z",
                "creator": {
                    "id": "%s",
                    "username": "alice",
                    "email": "alice@example.com"
                }
            }
            """, aliceId);

        ResponseEntity<String> pollResponse = restTemplate.postForEntity(
                "/polls",
                new HttpEntity<>(pollJson, createJsonHeaders()),
                String.class
        );
        assertEquals(HttpStatus.OK, pollResponse.getStatusCode());
        JsonNode poll = objectMapper.readTree(pollResponse.getBody());
        String pollId = poll.get("id").asText();
        System.out.println("✓ Step 6: Created poll with ID: " + pollId);

        // Step 7: List polls (should show the new poll)
        response = restTemplate.getForEntity("/polls", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("What's your favorite color?"));
        System.out.println("✓ Step 7: Polls list shows the new poll");

        // Step 8: Create vote options for the poll
        String redOptionJson = String.format("""
            {
                "caption": "Red",
                "presentationOrder": 1,
                "poll": {
                    "id": "%s"
                }
            }
            """, pollId);

        ResponseEntity<String> redOptionResponse = restTemplate.postForEntity(
                "/voteoptions",
                new HttpEntity<>(redOptionJson, createJsonHeaders()),
                String.class
        );
        assertEquals(HttpStatus.OK, redOptionResponse.getStatusCode());
        JsonNode redOption = objectMapper.readTree(redOptionResponse.getBody());
        String redOptionId = redOption.get("id").asText();
        System.out.println("✓ Step 8a: Created Red option with ID: " + redOptionId);

        String blueOptionJson = String.format("""
            {
                "caption": "Blue", 
                "presentationOrder": 2,
                "poll": {
                    "id": "%s"
                }
            }
            """, pollId);

        ResponseEntity<String> blueOptionResponse = restTemplate.postForEntity(
                "/voteoptions",
                new HttpEntity<>(blueOptionJson, createJsonHeaders()),
                String.class
        );
        assertEquals(HttpStatus.OK, blueOptionResponse.getStatusCode());
        JsonNode blueOption = objectMapper.readTree(blueOptionResponse.getBody());
        String blueOptionId = blueOption.get("id").asText();
        System.out.println("✓ Step 8b: Created Blue option with ID: " + blueOptionId);

        // Step 9: Bob votes for Red
        String voteRedJson = String.format("""
            {
                "user": {
                    "id": "%s",
                    "username": "bob",
                    "email": "bob@example.com"
                },
                "voteOption": {
                    "id": "%s",
                    "caption": "Red"
                }
            }
            """, bobId, redOptionId);

        ResponseEntity<String> voteRedResponse = restTemplate.postForEntity(
                "/votes",
                new HttpEntity<>(voteRedJson, createJsonHeaders()),
                String.class
        );
        assertEquals(HttpStatus.OK, voteRedResponse.getStatusCode());
        System.out.println("✓ Step 9: Bob voted for Red");

        // Step 10: List votes (should show Bob's vote for Red)
        response = restTemplate.getForEntity("/votes", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Red"));
        System.out.println("✓ Step 10: Votes list shows Bob's vote for Red");

        // Step 11: Bob changes vote to Blue
        String voteBlueJson = String.format("""
            {
                "user": {
                    "id": "%s",
                    "username": "bob",
                    "email": "bob@example.com"
                },
                "voteOption": {
                    "id": "%s", 
                    "caption": "Blue"
                }
            }
            """, bobId, blueOptionId);

        ResponseEntity<String> voteBlueResponse = restTemplate.postForEntity(
                "/votes",
                new HttpEntity<>(voteBlueJson, createJsonHeaders()),
                String.class
        );
        assertEquals(HttpStatus.OK, voteBlueResponse.getStatusCode());
        System.out.println("✓ Step 11: Bob changed vote to Blue");

        // Step 12: List votes again (should show vote for Blue)
        response = restTemplate.getForEntity("/votes", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Blue"));
        System.out.println("✓ Step 12: Votes list shows Bob's vote for Blue");

        // Step 13: Delete the poll
        restTemplate.delete("/polls/" + pollId);
        System.out.println("✓ Step 13: Deleted the poll");

        // Step 14: List votes again (should be empty)
        response = restTemplate.getForEntity("/votes", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().equals("[]") || response.getBody().contains("[]"));
        System.out.println("✓ Step 14: Votes list is empty after poll deletion");

        // Step 15: List polls (should be empty)
        response = restTemplate.getForEntity("/polls", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().equals("[]") || response.getBody().contains("[]"));
        System.out.println("✓ Step 15: Polls list is empty");

        // Step 16: List users (should still have both users)
        response = restTemplate.getForEntity("/users", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("alice"));
        assertTrue(response.getBody().contains("bob"));
        System.out.println("✓ Step 16: Users list still shows both users");

        System.out.println("=== All steps completed successfully! ===");
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
