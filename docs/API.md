# College Voting System — REST API Specification

This document provides the formal contract for all REST endpoints exposed by the College Voting System backend.

---

## 1. Authentication & Base URL
- **Base URL:** `http://localhost:8080/api`
- **Swagger Interactive UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI 3.0 JSON Spec:** `http://localhost:8080/v3/api-docs`
- **Authentication Header:**
  ```http
  Authorization: Bearer <jwt-token>
  ```

---

## 2. Standardized Error Response Structure

All endpoints return a uniform error format in accordance with RFC 7807 when a request encounters an error:

```json
{
  "timestamp": "2026-10-20T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/auth/register",
  "fieldErrors": [
    {
      "field": "rollNumber",
      "rejectedValue": "AB",
      "message": "Roll number must be between 3 and 50 characters"
    }
  ]
}
```

### Standard HTTP Status Codes:
- `200 OK`: Request succeeded.
- `201 Created`: Resource successfully created.
- `204 No Content`: Resource deleted successfully.
- `400 Bad Request`: Validation failure, election closed, or invalid lifecycle transition.
- `401 Unauthorized`: Missing, expired, or invalid JWT token.
- `403 Forbidden`: Authenticated user lacks required role (e.g. voter accessing admin routes, or accessing embargoed results).
- `404 Not Found`: Resource (user, election, candidate) does not exist.
- `409 Conflict`: Business rule or DB constraint violation (e.g., duplicate roll number or already voted).

---

## 3. Endpoints Matrix

### A. Authentication Module (`/api/auth`)

| Method | Endpoint | Access | Description | Status Codes |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new student voter. Role is strictly forced to `VOTER`. | `201`, `400`, `409` |
| `POST` | `/api/auth/login` | Public | Authenticate voter or admin and receive signed JWT. | `200`, `400`, `401` |
| `GET` | `/api/auth/me` | Authenticated | Fetch current logged-in user profile, permissions, and roles. | `200`, `401` |

#### `POST /api/auth/register`
**Request Body:**
```json
{
  "fullName": "Aarav Sharma",
  "rollNumber": "24CS001",
  "email": "aarav.s@apex.edu",
  "department": "Computer Science",
  "password": "VoterSecure@123"
}
```
**Response (201 Created):**
```json
{
  "id": 1,
  "rollNumber": "24CS001",
  "fullName": "Aarav Sharma",
  "email": "aarav.s@apex.edu",
  "role": "VOTER"
}
```

#### `POST /api/auth/login`
**Request Body:**
```json
{
  "rollNumber": "24CS001",
  "password": "VoterSecure@123"
}
```
**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "rollNumber": "24CS001",
  "role": "VOTER",
  "fullName": "Aarav Sharma"
}
```

---

### B. Election Management Module (`/api/elections`)

| Method | Endpoint | Access | Description | Status Codes |
|---|---|---|---|---|
| `GET` | `/api/elections` | Authenticated | List all elections (voters see UPCOMING, ACTIVE, CLOSED; admins see all including DRAFT). | `200`, `401` |
| `POST` | `/api/elections` | `ADMIN` only | Create a new election in `DRAFT` status. | `201`, `400`, `401`, `403` |
| `GET` | `/api/elections/{id}` | Authenticated | Retrieve election details and status. | `200`, `401`, `404` |
| `PUT` | `/api/elections/{id}` | `ADMIN` only | Update election title, description, and window (only allowed while in `DRAFT`). | `200`, `400`, `401`, `403`, `404` |
| `DELETE` | `/api/elections/{id}` | `ADMIN` only | Delete an election (only allowed while in `DRAFT`). | `204`, `400`, `401`, `403`, `404` |
| `PATCH` | `/api/elections/{id}/status` | `ADMIN` only | Transition status: `DRAFT` -> `UPCOMING` -> `ACTIVE` -> `CLOSED`. | `200`, `400`, `401`, `403`, `404` |
| `PATCH` | `/api/elections/{id}/results-visibility` | `ADMIN` only | Toggle public results visibility (`true`/`false`). | `200`, `401`, `403`, `404` |
| `GET` | `/api/elections/{id}/participation` | `ADMIN` only | View turnout statistics (total registered voters, votes cast, turnout %). | `200`, `401`, `403`, `404` |
| `GET` | `/api/elections/{id}/results` | Authenticated | Fetch certified results. Embargoed for voters until published by admin. | `200`, `401`, `403`, `404` |

#### `PATCH /api/elections/{id}/status`
**Request Body:**
```json
{
  "status": "ACTIVE"
}
```

#### `PATCH /api/elections/{id}/results-visibility`
**Request Body:**
```json
{
  "published": true
}
```

#### `GET /api/elections/{id}/participation`
**Response (200 OK):**
```json
{
  "electionId": 1,
  "electionTitle": "Student Council President 2026",
  "totalRegisteredVoters": 450,
  "votesCast": 315,
  "turnoutPercentage": 70.0
}
```

---

### C. Candidate Nomination Module (`/api/elections/{id}/candidates` & `/api/candidates`)

| Method | Endpoint | Access | Description | Status Codes |
|---|---|---|---|---|
| `GET` | `/api/elections/{id}/candidates` | Authenticated | List all approved candidates nominated for this election. | `200`, `401`, `404` |
| `POST` | `/api/elections/{id}/candidates` | `ADMIN` only | Nominate a candidate for an election (allowed only in `DRAFT` or `UPCOMING`). | `201`, `400`, `401`, `403`, `404` |
| `PUT` | `/api/candidates/{id}` | `ADMIN` only | Update candidate details, manifesto, or photo URL. | `200`, `400`, `401`, `403`, `404` |
| `DELETE` | `/api/candidates/{id}` | `ADMIN` only | Remove a candidate nomination before voting begins. | `204`, `400`, `401`, `403`, `404` |

#### `POST /api/elections/{id}/candidates`
**Request Body:**
```json
{
  "name": "Priya Patel",
  "manifesto": "Foster inclusive tech societies and student innovation funds.",
  "department": "Computer Science",
  "position": "President",
  "photoUrl": "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2"
}
```

---

### D. Voting Transaction Module (`/api/elections/{id}/votes`)

| Method | Endpoint | Access | Description | Status Codes |
|---|---|---|---|---|
| `POST` | `/api/elections/{id}/votes` | `VOTER` only | Cast a ballot in an active election. Guarantees anonymity and prevents double voting. | `201`, `400`, `401`, `403`, `404`, `409` |

#### `POST /api/elections/{id}/votes`
**Request Body:**
```json
{
  "candidateId": 2
}
```
**Response (201 Created):**
```json
{
  "message": "Your vote has been cast and securely recorded.",
  "electionId": 1,
  "castTime": "2026-10-20T12:30:00.000"
}
```
> **Note:** The response intentionally omits the `candidateId` to prevent shoulder-surfing, receipt-generation, or coerced vote-proving.

---

### E. Certified Results Module (`/api/elections/{id}/results`)

**Response (200 OK):**
```json
{
  "electionId": 1,
  "electionTitle": "Student Council President 2026",
  "totalVotesCast": 315,
  "resultsPublished": true,
  "candidateResults": [
    {
      "candidateId": 2,
      "candidateName": "Priya Patel",
      "department": "Computer Science",
      "position": "President",
      "photoUrl": "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2",
      "voteCount": 195,
      "percentage": 61.9
    },
    {
      "candidateId": 1,
      "candidateName": "Aarav Sharma",
      "department": "Mechanical Engineering",
      "position": "President",
      "photoUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
      "voteCount": 120,
      "percentage": 38.1
    }
  ]
}
```
