# ConstructPro - Construction Management System

A full-stack Construction Management and Site Analytics application developed for DBMS Lab.

- **Frontend**: Pure HTML, Vanilla CSS, and JavaScript (SPA Dashboard).
- **Backend**: Native Java HTTP Server (`backend/Main.java`).
- **Database**: Cloud Neon PostgreSQL (with local Oracle XE 11g fallback).
- **Deployment**:
  - Backend: **Render** (Docker Web Service)
  - Frontend: **Vercel** (Static Web App)
  - Database: **Neon** (Serverless PostgreSQL)

---

## Architecture & Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/projects` | GET | List all active/completed/planning construction projects |
| `/insert` | GET/POST | Insert a new project (`id`, `name`, `location`, `manager`, `progress`, `status`) |
| `/update` | GET/POST | Update an existing project's progress, manager, status |
| `/delete` | GET/POST | Delete a project by `id` |
| `/evaluate` | GET | Evaluate project schedule health, velocity, and milestone delay risks |
| `/estimate` | GET | Automated civil cost, labor, steel, and cement estimation |
| `/estimations` | GET | Fetch saved project estimations history |

---

## Deployment Guide

### 1. Backend on Render (Web Service)

1. Go to [Render Dashboard](https://dashboard.render.com/) -> **New** -> **Web Service**.
2. Connect your GitHub repository: `shettydakshraj/ConstructPro`.
3. Configuration:
   - **Language / Environment**: `Docker`
   - **Root Directory**: *(Leave empty / blank)*
   - **Dockerfile Path**: `Dockerfile` *(Default)*
4. Under **Environment Variables**, add:
   - **KEY**: `DATABASE_URL`
   - **VALUE**: `postgresql://neondb_owner:npg_GH3JM7nagPYo@ep-curly-dust-b4vwh9uo-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require`
5. Click **Create Web Service**.
6. Render will automatically build the container and provide your live URL (e.g., `https://constructpro.onrender.com`).

---

### 2. Frontend on Vercel

1. Go to [Vercel Dashboard](https://vercel.com/dashboard) -> **Add New Project**.
2. Import the `ConstructPro` repository.
3. Configuration:
   - **Root Directory**: Select `frontend`
   - **Framework Preset**: `Other`
   - **Build & Output Settings**: Leave default
4. Click **Deploy**.
5. Once your Render backend is live, configure your frontend by updating `DEPLOYED_RENDER_BACKEND` in `frontend/script.js` or by opening your Vercel site's console and running:
   ```javascript
   setBackendUrl('https://YOUR-RENDER-BACKEND.onrender.com');
   ```

---

## Local Development

### Run Backend
```powershell
cd backend
javac -cp "postgresql-42.7.3.jar;ojdbc6.jar" Main.java
java -cp ".;postgresql-42.7.3.jar;ojdbc6.jar" Main
```
*Note: If `DATABASE_URL` is configured, it connects to Neon PostgreSQL. If omitted, it falls back to local Oracle XE (`localhost:1521:XE`).*

### Run Frontend
Simply open `frontend/index.html` in your browser, or run:
```powershell
cd frontend
python -m http.server 3000
```
- **Login Credentials**: Username: `admin` | Password: `admin123`
