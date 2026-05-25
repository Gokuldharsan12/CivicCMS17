# 🏛️ CivicCMS v3 — AI-Powered Civic Complaint Management System
### Final Year Project | Spring Boot + MySQL + AI Chatbot + Real-time Alerts

---

## 🎯 What This System Does

CivicCMS is a full-stack civic complaint management platform that uses AI to:
- **Auto-classify** complaints into 10 categories (Water, Road, Electricity, etc.)
- **Detect urgency** from LOW to CRITICAL using NLP keyword analysis
- **Check duplicates** using cosine similarity (>80% = flagged)
- **Route complaints** to the correct government department automatically
- **Generate descriptions** — expands "water problem" into a 4-line formal complaint
- **Alert admins** in real-time via SSE when HIGH/CRITICAL complaints arrive

---

## 🚀 Quick Start (5 Minutes)

### Prerequisites
```
Java 21+  |  MySQL 8.0+  |  Maven 3.9+  |  Chrome/Edge (for voice input)
```

### Step 1 — Database
```bash
mysql -u root -p
source schema.sql
```

### Step 2 — Configure
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD
# Optional: Real AI (leave blank for built-in mock AI)
# anthropic.api.key=sk-ant-api03-...
```

### Step 3 — Build and Run
```bash
mvn clean package -DskipTests
java -jar target/civiccms-0.0.1-SNAPSHOT.jar
```

### Step 4 — Open
```
http://localhost:8080
```

---

## 🔑 Demo Login Credentials

| Role    | Email                 | Password  |
|---------|-----------------------|-----------|
| Citizen | citizen@civiccms.in   | Test@123  |
| Admin   | admin@civiccms.in     | Admin@123 |

Google Login: Click "Continue with Google", enter any email, works instantly.
Phone Login: Click "Continue with Phone", any 10-digit number, OTP is 123456.

---

## 📱 All Pages

| URL | Description |
|-----|-------------|
| /index.html | Home — KPIs, quick track, AI promo |
| /chatbot.html | AI Chatbot — main feature |
| /submit.html | Multi-step form-based submission |
| /track.html | Track any complaint by ID |
| /history.html | User's complaint history |
| /login.html | Login — email / Google / Phone |
| /api-test.html | Live API test console |
| /admin/dashboard.html | Admin analytics dashboard |
| /admin/complaints.html | Manage all complaints |
| /admin/alerts.html | Real-time urgency alert centre |
| /admin/heatmap.html | Geographic complaint heatmap |

---

## 🤖 AI Chatbot Features

### Inputs Supported
- Text: Type complaint in plain English
- Voice: Click microphone, speak (Chrome/Edge)
- Image: Upload photo, AI detects issue type
- Quick Chips: One-click common issues

### AI Analysis Output
- Title: Auto-generated 5-8 word title
- Detailed Description: 3-4 professional sentences
- Category: Water / Electricity / Road / Garbage / etc.
- Urgency: LOW / MEDIUM / HIGH / CRITICAL with reason
- Department: Auto-routed government department
- Suggested Action: Recommended response
- Duplicate Check: Flags if >80% similar to existing complaint

---

## 📡 REST API Reference

### Authentication
```
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/social-login
```

### Complaints
```
POST   /api/v1/complaints
GET    /api/v1/complaints          (admin)
GET    /api/v1/complaints/my       (citizen)
GET    /api/v1/complaints/track/{id}
PATCH  /api/v1/complaints/{id}/status
```

### AI Endpoints
```
POST /api/ai/analyze
GET  /api/heatmap
GET  /api/complaints/stats
```

### Example — AI Analyze
```bash
curl -X POST http://localhost:8080/api/ai/analyze \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"text":"Water pipe burst near school","lat":11.1271,"lng":78.6569}'
```

Response:
```json
{
  "title": "Water Supply Issue - water",
  "detailedDescription": "Residents at the reported area are facing a serious water supply issue...",
  "category": "WATER",
  "urgency": "HIGH",
  "urgencyReason": "The issue severely impacts daily life and essential services...",
  "department": "Water Supply and Sewerage Board",
  "suggestedAction": "Dispatch a plumbing team within 24 hours to inspect and repair.",
  "isDuplicate": false,
  "similarityScore": 0.0
}
```

---

## 🧠 How AI Works Without an API Key

The built-in rule-based AI runs entirely in Java with no internet needed:

Category Detection — keyword matching:
  "water", "pipe", "leak" = WATER
  "road", "pothole" = ROAD
  "electricity", "power cut" = ELECTRICITY

Urgency Detection — severity keyword scoring:
  "fire", "emergency", "collapse" = CRITICAL
  "no water", "burst pipe" = HIGH
  "problem", "issue" = MEDIUM

Duplicate Detection — cosine similarity on word vectors:
  similarity > 0.80 = DUPLICATE

To use real Claude AI, set the environment variable:
```bash
export ANTHROPIC_API_KEY=sk-ant-api03-...
```

---

## 🏗️ Project Structure

```
civiccms/
├── src/main/java/com/civiccms/
│   ├── ai/
│   │   ├── AiAnalyzeService.java        (MAIN AI ENGINE - NEW)
│   │   ├── AiPipelineService.java       (Async post-submit pipeline)
│   │   ├── DuplicateDetectorService.java
│   │   ├── KeywordExtractorService.java
│   │   ├── RiskScoringEngine.java
│   │   ├── SentimentAnalyserService.java
│   │   └── DepartmentRouterService.java
│   ├── controller/
│   │   ├── AiAnalyzeController.java     (NEW - /api/ai/analyze)
│   │   ├── AuthController.java
│   │   ├── ComplaintController.java
│   │   ├── AnalyticsController.java
│   │   ├── SseController.java           (UPDATED - /events endpoint)
│   │   └── ...
│   ├── dto/
│   │   ├── AiAnalyzeDtos.java           (NEW)
│   │   └── ...
│   └── config/SecurityConfig.java       (UPDATED - new routes added)
│
├── src/main/resources/static/
│   ├── chatbot.html                     (NEW - AI chatbot UI)
│   ├── login.html                       (FIXED - Google + Phone working)
│   ├── index.html                       (UPDATED - AI promo card)
│   ├── submit.html                      (UPDATED - AI pre-fill)
│   ├── api-test.html                    (NEW - API test console)
│   └── admin/
│       ├── alerts.html                  (NEW - urgency alert centre)
│       └── ...
│
├── schema.sql                           (Complete MySQL schema)
└── README.md
```

---

## 🔔 Urgency Alert System

When a complaint is HIGH or CRITICAL:
1. Red banner appears on chatbot page
2. SSE broadcast to admin dashboard
3. Email alert sent to department head
4. Admin alerts page shows live feed with action buttons
5. Admin can click In Progress / Resolve / Escalate directly

---

## 🐛 Common Issues

| Issue | Fix |
|-------|-----|
| Access Denied on login | Register first at /register.html |
| Google/Phone login fails | Backend must be running on port 8080 |
| Voice input not working | Use Chrome or Edge; allow microphone |
| MySQL connection refused | Start MySQL; check password in application.properties |
| Port 8080 already in use | Kill existing process or change port |
| Email errors | Configure Gmail App Password or ignore (optional feature) |

---

## 📊 Demo Walkthrough

1. Open http://localhost:8080 — show home page with KPIs
2. Click "Try AI Chatbot" — demonstrate:
   - Type: "Water pipe burst near school"
   - Click quick chip: "Power Cut" — show CRITICAL detection
   - Click mic and speak a complaint
   - Show urgency badge colors
   - Click Submit Complaint
3. Open admin dashboard — show real-time stats
4. Open /admin/alerts.html — show HIGH/CRITICAL feed with action buttons
5. Open /admin/heatmap.html — show geographic visualization
6. Open /api-test.html — test all API endpoints live

---

CivicCMS v3 — Final Year Project | Spring Boot + MySQL + Claude AI + SSE
