function login(event) {

    event.preventDefault();

    const username =
        document.getElementById("username").value;

    const password =
        document.getElementById("password").value;

    const message =
        document.getElementById("loginMessage");

    if (username === "admin" && password === "admin123") {

        message.textContent = "Login successful.";
        message.style.color = "#2ecc71";

        setTimeout(() => {
            window.location.href = "dashboard.html";
        }, 500);

    } else {

        message.textContent =
            "Invalid username or password.";

        message.style.color = "#ff5c5c";
    }
}


function logout() {

    window.location.href = "index.html";
}


// ================= REFRESH =================

function refreshProjects() {

    fetch("http://localhost:8081/projects")

        .then(response => response.json())

        .then(projects => {

            const table =
                document.getElementById("projectTableBody");

            table.innerHTML = "";

            projects.forEach(project => {

                table.innerHTML += `

                    <tr>

                        <td>${project.id}</td>

                        <td>${project.name}</td>

                        <td>${project.location}</td>

                        <td>${project.manager}</td>

                        <td>${project.progress}%</td>

                        <td>
                            <span class="status ${project.status.toLowerCase()}">
                                ${project.status}
                            </span>
                        </td>

                    </tr>

                `;
            });

            window.allProjects = projects;
            populateProjectDropdown(projects);
            updateStats(projects);

        })

        .catch(error => {

            console.error(error);

            alert("Could not connect to the database.");

        });
}


function updateStats(projects) {

    if (!Array.isArray(projects)) return;

    const pad = n => String(n).padStart(2, '0');

    const activeCount = projects.filter(
        p => (p.status || "").trim().toUpperCase() === "ACTIVE"
    ).length;

    const completedCount = projects.filter(
        p => (p.status || "").trim().toUpperCase() === "COMPLETED"
    ).length;

    const totalCount = projects.length;

    const ongoingSites = activeCount;

    const statActive = document.getElementById("statActive");
    if (statActive) statActive.textContent = pad(activeCount);

    const statSites = document.getElementById("statSites");
    if (statSites) statSites.textContent = pad(ongoingSites);

    const statTotal = document.getElementById("statTotal");
    if (statTotal) statTotal.textContent = pad(totalCount);

    const statCompleted = document.getElementById("statCompleted");
    if (statCompleted) statCompleted.textContent = pad(completedCount);

    const statAvgProgress = document.getElementById("statAvgProgress");
    if (statAvgProgress && projects.length > 0) {
        const totalProgress = projects.reduce(
            (sum, p) => sum + (Number(p.progress) || 0), 0
        );
        const avg = Math.round(totalProgress / projects.length);
        statAvgProgress.textContent = `${avg}% Average`;
    }
}


// ================= INSERT =================

function insertProject() {

    const id =
        prompt("Enter Project ID:");

    if (!id) return;

    const name =
        prompt("Enter Project Name:");

    if (!name) return;

    const location =
        prompt("Enter Location:");

    if (!location) return;

    const manager =
        prompt("Enter Manager:");

    if (!manager) return;

    const progress =
        prompt("Enter Progress (0-100):");

    if (!progress) return;

    const status =
        prompt("Enter Status (ACTIVE / COMPLETED / PLANNING):");

    if (!status) return;


    const url =
        `http://localhost:8081/insert?id=${encodeURIComponent(id)}&name=${encodeURIComponent(name)}&location=${encodeURIComponent(location)}&manager=${encodeURIComponent(manager)}&progress=${encodeURIComponent(progress)}&status=${encodeURIComponent(status)}`;


    fetch(url)

        .then(response => response.text())

        .then(result => {

            alert(result);

            refreshProjects();

        })

        .catch(error => {

            console.error(error);

            alert("INSERT failed.");

        });
}


// ================= UPDATE =================

function updateProject() {

    const id =
        prompt("Enter Project ID to update:");

    if (!id) return;

    const name =
        prompt("Enter New Project Name:");

    if (!name) return;

    const location =
        prompt("Enter New Location:");

    if (!location) return;

    const manager =
        prompt("Enter New Manager:");

    if (!manager) return;

    const progress =
        prompt("Enter New Progress (0-100):");

    if (!progress) return;

    const status =
        prompt("Enter New Status:");

    if (!status) return;


    const url =
        `http://localhost:8081/update?id=${encodeURIComponent(id)}&name=${encodeURIComponent(name)}&location=${encodeURIComponent(location)}&manager=${encodeURIComponent(manager)}&progress=${encodeURIComponent(progress)}&status=${encodeURIComponent(status)}`;


    fetch(url)

        .then(response => response.text())

        .then(result => {

            alert(result);

            refreshProjects();

        })

        .catch(error => {

            console.error(error);

            alert("UPDATE failed.");

        });
}


// ================= DELETE =================

function deleteProject() {

    const id =
        prompt("Enter Project ID to delete:");

    if (!id) return;


    if (!confirm(
        "Are you sure you want to delete " + id + "?"
    )) return;


    fetch(
        `http://localhost:8081/delete?id=${encodeURIComponent(id)}`
    )

        .then(response => response.text())

        .then(result => {

            alert(result);

            refreshProjects();

        })

        .catch(error => {

            console.error(error);

            alert("DELETE failed.");

        });
}


// ================= SCHEDULE & HEALTH EVALUATOR =================

function populateProjectDropdown(projects) {
    const sel = document.getElementById("evalSelectProject");
    if (!sel || !Array.isArray(projects)) return;
    const currentVal = sel.value;
    sel.innerHTML = "";
    projects.forEach(p => {
        const opt = document.createElement("option");
        opt.value = p.id;
        opt.textContent = `${p.id} - ${p.name}`;
        sel.appendChild(opt);
    });
    if (currentVal && projects.some(p => p.id === currentVal)) {
        sel.value = currentVal;
    }
    onProjectSelectChanged();
}

function onProjectSelectChanged() {
    const sel = document.getElementById("evalSelectProject");
    if (!sel || !window.allProjects) return;
    const selected = window.allProjects.find(p => p.id === sel.value);
    if (selected) {
        const progInput = document.getElementById("evalProgress");
        if (progInput) progInput.value = selected.progress;
    }
}

function evaluateProjectSchedule(autoUpdate) {
    const sel = document.getElementById("evalSelectProject");
    const id = sel ? sel.value : "P001";
    const progress = parseInt(document.getElementById("evalProgress").value) || 0;
    const daysElapsed = parseInt(document.getElementById("evalDaysElapsed").value) || 30;
    const targetDays = parseInt(document.getElementById("evalTargetDays").value) || 90;

    const url = `http://localhost:8081/evaluate?id=${encodeURIComponent(id)}&progress=${encodeURIComponent(progress)}&daysElapsed=${encodeURIComponent(daysElapsed)}&targetDays=${encodeURIComponent(targetDays)}&autoUpdate=${autoUpdate ? "true" : "false"}`;

    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.status === "ERROR") {
                alert("Error evaluating project: " + (data.message || "Unknown error"));
                return;
            }

            const titleEl = document.getElementById("outProjectTitle");
            if (titleEl) titleEl.textContent = `${data.name} (${data.id})`;

            const badge = document.getElementById("healthBadge");
            if (badge) {
                badge.textContent = data.health;
                if (data.health === "ON TRACK" || data.health === "COMPLETED") {
                    badge.className = "status active";
                } else if (data.health === "DELAY RISK") {
                    badge.className = "status planning";
                } else {
                    badge.className = "status deficit";
                }
            }

            const recEl = document.getElementById("outRecommendation");
            if (recEl) recEl.textContent = data.recommendation;

            const remainingEl = document.getElementById("outRemaining");
            if (remainingEl) remainingEl.textContent = `${data.remainingWork}%`;

            const paceEl = document.getElementById("outPaceVariance");
            if (paceEl) {
                if (data.variance < 0) {
                    paceEl.textContent = `${Math.abs(data.variance)} Days Ahead`;
                    paceEl.style.color = "#2ecc71";
                } else if (data.variance === 0) {
                    paceEl.textContent = "Exact Schedule (0 Days)";
                    paceEl.style.color = "#2ecc71";
                } else {
                    paceEl.textContent = `${data.variance} Days Behind`;
                    paceEl.style.color = "#ff5c5c";
                }
            }

            const projectedEl = document.getElementById("outProjectedDays");
            if (projectedEl) projectedEl.textContent = `${data.projectedDays} Days Total`;

            const siteEl = document.getElementById("outSiteInfo");
            if (siteEl) siteEl.textContent = `${data.location} (${data.manager})`;

            const statusEl = document.getElementById("outNewStatus");
            if (statusEl) statusEl.textContent = data.newStatus;

            const syncEl = document.getElementById("outDbSync");
            if (syncEl) {
                if (autoUpdate) {
                    syncEl.textContent = `Updated in DB (${data.id})`;
                    syncEl.style.color = "#54b5ff";
                    alert(`Project ${data.id} successfully updated in database with Progress: ${data.progress}% and Status: ${data.newStatus}!`);
                    refreshProjects();
                } else {
                    syncEl.textContent = "Pace Preview Mode";
                    syncEl.style.color = "#8998a8";
                }
            }
        })
        .catch(err => {
            console.error(err);
            alert("Could not connect to project evaluation service.");
        });
}


// ================= LOAD =================

function initDashboard() {
    if (document.getElementById("projectTableBody")) {
        refreshProjects();
    }
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initDashboard);
} else {
    initDashboard();
}

window.onload = initDashboard;