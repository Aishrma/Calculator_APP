# Complete Setup & Run Guide — Calculator CI/CD Mini Project

This guide takes you from the project files to a working pipeline: a `git push` that builds, tests, containerises, pushes and deploys the app automatically and then emails you the result.

It is written for **WSL 2 (Ubuntu) on Windows 11 + Docker Desktop**. **Every command is run in the Ubuntu (WSL) terminal** with bash. Only the web pages (Jenkins, GitHub, Docker Hub, Gmail) are opened in your normal Windows browser. Do the parts in order.

| Part | What you do | Time |
|---|---|---|
| 0 | Prepare WSL: Docker integration, JDK 17, Git, GitHub CLI | 20 min |
| 1 | Move the project into the WSL filesystem | 5 min |
| 2 | Check the app locally (build, test, run) | 10 min |
| 3 | Commit & push with a proper Git history (feature branches + PRs) | 20 min |
| 4 | Docker Hub account + access token | 5 min |
| 5 | Check Docker and Compose by hand | 10 min |
| 6 | Start Jenkins in Docker | 15 min |
| 7 | Jenkins credentials + email configuration | 15 min |
| 8 | Create the pipeline job and run it | 15 min |
| 9 | GitHub webhook (automatic trigger) through ngrok | 15 min |
| 10 | Take the screenshots for the report | 15 min |
| 11 | Live demo script for Class 17 | — |
| 12 | Troubleshooting | — |

> **Values used below. Change them if yours differ.**
> * GitHub repo: `https://github.com/Aishrma/Calculator_APP`
> * Docker Hub username: `aishrma`. If yours is different, edit it in **two places**: `DOCKERHUB_USER` in the `Jenkinsfile`, and the default `${DOCKERHUB_USER:-aishrma}` in `docker-compose.yml`. Also update the Docker Hub link in `README.md`.
> * Notification email: `Aishwarya.Sharma@iiitb.ac.in` (`NOTIFY_EMAIL` in the `Jenkinsfile`)
> * Project folder inside WSL: `~/Calculator_APP`
>
> **Ports:** Jenkins `8080` · deployed app (nginx) `8081` · `docker run` test `9090` · ngrok inspector `4040`.
> WSL 2 forwards these to Windows, so `http://localhost:<port>` works both in the Windows browser and with `curl` inside Ubuntu.

---

## Part 0 — Prepare WSL

### 0.1 Open Ubuntu and update it

Open **Ubuntu** from the Start menu, or run `wsl` in a Windows terminal.

```bash
sudo apt update && sudo apt upgrade -y
lsb_release -a          # Ubuntu 22.04 or 24.04 expected
```

### 0.2 Docker: connect Docker Desktop to WSL

You already have Docker Desktop installed. The simplest and most reliable setup is to let your Ubuntu use Docker Desktop's engine:

1. Start **Docker Desktop** on Windows and wait for *Engine running*.
2. *Settings → General*: ☑ **Use the WSL 2 based engine**.
3. *Settings → Resources → WSL integration*: ☑ **Enable integration with my default WSL distro** and switch **Ubuntu** on → **Apply & restart**.
4. Close the Ubuntu terminal, open a new one, and check:
   ```bash
   docker version          # must show both Client AND Server
   docker compose version  # v2.17+ (it has the --wait flag)
   docker run --rm hello-world
   ```

> **Do not** also install `docker.io` / `docker-ce` with apt inside Ubuntu. Two engines would fight over `/var/run/docker.sock`. If `docker` says *"could not be found in this WSL 2 distro"*, the integration toggle in step 3 is off.
> Docker Desktop must be **running** whenever you work (Jenkins and the app run in it).

### 0.3 JDK 17, Git, curl, unzip

```bash
sudo apt install -y openjdk-17-jdk-headless git curl unzip
java -version           # openjdk version "17.x"
```
Maven is **not** needed. The project ships the Maven Wrapper `./mvnw`, which downloads Maven itself.

If `java -version` shows a different version, choose 17 with `sudo update-alternatives --config java`.

### 0.4 Git identity (WSL has its own Git config, separate from Windows)

```bash
git config --global user.name  "Aishrma"
git config --global user.email "Aishwarya.Sharma@iiitb.ac.in"
git config --global core.autocrlf input     # never write CRLF into the repo
git config --global init.defaultBranch main
git config --global pull.rebase false
```

> **Commit authorship:** every commit is made by you with this name and email. Do **not** add any `Co-Authored-By` lines, so you are the only contributor in the history.

### 0.5 GitHub login from WSL (for `git push` and creating PRs)

GitHub no longer accepts passwords over HTTPS. The easiest option is the GitHub CLI:

```bash
sudo apt install -y gh
gh auth login
#   ? Where do you use GitHub?            GitHub.com
#   ? Preferred protocol for Git?         HTTPS
#   ? Authenticate Git with credentials?  Yes
#   ? How would you like to authenticate? Login with a web browser
#   -> copy the one-time code, then open https://github.com/login/device in your Windows browser
gh auth setup-git
gh auth status          # Logged in to github.com as Aishrma
```

### 0.6 VS Code in WSL (optional but recommended)

In VS Code, install the **WSL** extension (by Microsoft). After that, `code .` inside an Ubuntu folder opens it in VS Code with a terminal that is already bash in WSL.

---

## Part 1 — Move the project into the WSL filesystem

The files are currently on the Windows drive (`/mnt/c/...`). Building there from WSL is **slow** (file access crosses the Windows/Linux boundary). Every file also shows up as executable, which would pollute the Git history. So work from a **fresh clone in your Linux home folder** and copy the project files into it.

```bash
cd ~
git clone https://github.com/Aishrma/Calculator_APP.git
cd ~/Calculator_APP

# Copy everything except Windows' .git folder and build output
SRC="/mnt/c/Users/Aishwarya Sharma/Desktop/College Work/Semester 7/Courses/SPE/Mini Project"
sudo apt install -y rsync
rsync -av --exclude '.git' --exclude 'target' --exclude 'SPE_Mini_Project.docx' "$SRC"/ ./

# Normal Linux permissions: folders 755, files 644, only mvnw executable
find . -path ./.git -prune -o -type d -exec chmod 755 {} +
find . -path ./.git -prune -o -type f -exec chmod 644 {} +
chmod +x mvnw

git status       # README.md modified + many untracked files; no target/, no .docx
code .           # (optional) open in VS Code
```

From now on, **only use `~/Calculator_APP`**. The Windows folder is just the original copy.
To reach WSL files from Windows Explorer (e.g. to drop in screenshots), go to `\\wsl.localhost\Ubuntu\home\<your-linux-user>\Calculator_APP`.

---

## Part 2 — Check the application locally

All commands run from `~/Calculator_APP`.

```bash
# 1. Run the tests (the first run downloads Maven + dependencies, which takes a few minutes)
./mvnw clean test
```
Expected result: `Tests run: 32, Failures: 0, Errors: 0`, then **BUILD SUCCESS**.
* JUnit XML reports: `target/surefire-reports/TEST-*.xml` (Jenkins publishes these)
* Coverage report: `explorer.exe target/site/jacoco/index.html` opens it in the Windows browser (about 94%)

```bash
# 2. Build the executable JAR
./mvnw clean package -DskipTests
ls -lh target/calculator-api.jar

# 3. Run it
java -jar target/calculator-api.jar
```

In a **second** Ubuntu terminal:
```bash
curl "http://localhost:8080/api/v1/add?a=2&b=3"
# {"operation":"add","a":2,"b":3,"result":5}
curl "http://localhost:8080/api/v1/divide?a=5&b=0"
# {"timestamp":"...","status":400,"error":"Bad Request","message":"Division by zero is not allowed: ...","path":"/api/v1/divide"}
curl "http://localhost:8080/actuator/health"
# {"status":"UP","groups":["liveness","readiness"]}
```
Stop the app with `Ctrl+C` in the first terminal. **Port 8080 must be free before Part 6**, because Jenkins uses it.

### What each source file does (be ready to explain these in the demo)

| File | Role |
|---|---|
| `CalculatorApplication.java` | Spring Boot entry point |
| `controller/CalculatorController.java` | The 4 `GET /api/v1/...` endpoints. `@RequestParam BigDecimal` makes Spring reject a missing or non-numeric parameter automatically. |
| `controller/GlobalExceptionHandler.java` | `@RestControllerAdvice` that turns `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `DivisionByZeroException` and `ArithmeticException` into **HTTP 400 + `ErrorResponse` JSON** |
| `service/CalculatorService.java` | The arithmetic. Uses `BigDecimal` (exact decimals, `0.1+0.2=0.3`), throws `DivisionByZeroException` when `b == 0` |
| `dto/CalculationResponse.java`, `dto/ErrorResponse.java` | Java `record`s that Jackson serialises to JSON |
| `application.properties` | port, Actuator exposure (`health`, `info`), BigDecimal written as plain numbers, graceful shutdown |
| `CalculatorServiceTest` | 22 pure JUnit 5 unit tests (parameterised), including divide-by-zero |
| `CalculatorControllerIntegrationTest` | `@WebMvcTest` + **MockMvc**: 8 tests through the real controller, exception handler and JSON |
| `CalculatorApplicationTests` | `@SpringBootTest` on a random port, makes real HTTP calls to `/actuator/health` and `/api/v1/add` |

---

## Part 3 — Git & GitHub (progressive history, feature branches, PRs)

The rubric checks for **a meaningful history, at least one feature branch and a merge/PR, a `.gitignore`, and no secrets**. Commit the files **in logical groups on feature branches** rather than in one "final commit". The PRs below are created and merged with `gh`. You can also do the same thing on the GitHub website instead.

Make sure the repo is **public**: GitHub → repo → *Settings → General → Danger Zone → Change visibility*. A public repo means Jenkins needs no GitHub credentials.

### 3.1 Commit 1, on `main`: project skeleton

```bash
cd ~/Calculator_APP
git checkout main
git add .gitignore .gitattributes pom.xml mvnw mvnw.cmd .mvn
git commit -m "chore: add Maven project skeleton, wrapper and .gitignore"
git push origin main
```
Check: `git ls-files -s mvnw` should start with `100755`, which means the wrapper is stored as executable.

### 3.2 Feature branch 1: `feature/calculator-api`

```bash
git checkout -b feature/calculator-api

git add src/main/java/calculator/CalculatorApplication.java src/main/java/calculator/service src/main/java/calculator/exception src/main/resources
git commit -m "feat: add CalculatorService with BigDecimal arithmetic and divide-by-zero check"

git add src/main/java/calculator/dto src/main/java/calculator/controller
git commit -m "feat: expose /api/v1 add, subtract, multiply, divide endpoints with structured 400 errors"

git add src/test/java/calculator/service
git commit -m "test: add JUnit 5 unit tests for every operation incl. division by zero"

git add src/test/java/calculator/controller src/test/java/calculator/CalculatorApplicationTests.java
git commit -m "test: add MockMvc integration tests and full-context health test"

git push -u origin feature/calculator-api

# Open the PR and merge it (keep the branch as evidence of branching)
gh pr create --base main --head feature/calculator-api \
  --title "Calculator REST API + tests" \
  --body "Adds add/subtract/multiply/divide endpoints, structured 400 error handling, Actuator health, JUnit 5 unit tests and MockMvc integration tests."
gh pr merge feature/calculator-api --merge

git checkout main
git pull origin main
```

### 3.3 Feature branch 2: `feature/containerization`

```bash
git checkout -b feature/containerization

git add Dockerfile .dockerignore
git commit -m "build: add multi-stage Dockerfile (Maven build -> JRE alpine, non-root) and .dockerignore"

git add nginx docker-compose.yml
git commit -m "deploy: add docker-compose stack with nginx reverse proxy, 2 API replicas and health checks"

git push -u origin feature/containerization
gh pr create --base main --head feature/containerization \
  --title "Containerization: Dockerfile + Docker Compose stack" \
  --body "Multi-stage Dockerfile, .dockerignore, and a Compose stack with an nginx reverse proxy in front of 2 API replicas, health checks, restart policy and a dedicated network."
gh pr merge feature/containerization --merge

git checkout main
git pull origin main
```

### 3.4 Feature branch 3: `feature/jenkins-pipeline`

```bash
git checkout -b feature/jenkins-pipeline

git add jenkins/Dockerfile jenkins/docker-compose.yml jenkins/.env.example
git commit -m "ci: add Jenkins server image with Docker CLI, Maven and pipeline plugins"

git add Jenkinsfile
git commit -m "ci: add declarative Jenkinsfile (checkout, build, test, docker build, push, deploy, verify, email)"

git push -u origin feature/jenkins-pipeline
gh pr create --base main --head feature/jenkins-pipeline \
  --title "CI/CD: Jenkins pipeline" \
  --body "Declarative Jenkinsfile triggered by GitHub webhook: build, test (JUnit + JaCoCo), Docker build, push with Jenkins credentials, compose deploy with health verification, and email notifications."
gh pr merge feature/jenkins-pipeline --merge

git checkout main
git pull origin main
```

### 3.5 Docs

```bash
git add README.md docs/SETUP_GUIDE.md
git commit -m "docs: add README with build/run/API usage and setup guide"
git push origin main
```

Check: `git log --oneline --graph --all` should show about 10 commits and 3 merged branches. Also check that `git status` is clean, and that `git ls-files | grep -E "target|\.env$|docx"` prints nothing.

> From here on, **every push to `main` triggers Jenkins** (after Part 9). Work on a feature branch and merge it with a PR. The merge to `main` is what deploys.

---

## Part 4 — Docker Hub

1. Sign up or log in at <https://hub.docker.com>. Note your **username** (see the box at the top of this guide).
2. **Create the repository:** *Repositories → Create repository*. Name `calculator-api`, visibility **Public** → *Create*.
3. **Create an access token** (Jenkins uses this instead of your password): avatar → *Account settings → Personal access tokens → Generate new token*. Description `jenkins`, access permissions **Read & Write** → *Generate*. **Copy the token now**, because it is shown only once. Keep it out of every file in the repo.
4. Test it from Ubuntu:
   ```bash
   docker login -u aishrma        # paste the TOKEN as the password
   docker logout
   ```

---

## Part 5 — Check Docker & Compose by hand (before automating)

### 5.1 Single image with `docker run`

```bash
cd ~/Calculator_APP
docker build -t aishrma/calculator-api:manual .
docker images aishrma/calculator-api
docker run -d --name calc-test -p 9090:8080 aishrma/calculator-api:manual
docker ps                                      # STATUS becomes "(healthy)" after about 40 s
curl "http://localhost:9090/api/v1/multiply?a=6&b=7"
docker exec calc-test whoami                   # prints "spring", so the app is not running as root
docker rm -f calc-test
```

**Dockerfile talking points:**
* **Stage 1** `maven:3.9.9-eclipse-temurin-17` copies `pom.xml` first and runs `dependency:go-offline` **before** copying `src`. That dependency layer is cached, so a code-only change does not re-download dependencies.
* **Stage 2** `eclipse-temurin:17-jre-alpine` contains only a JRE and `app.jar`: no JDK, no Maven, no source. That makes the image much smaller (compare the sizes with `docker images`).
* `adduser spring` + `USER spring` → **non-root**; `EXPOSE 8080`; `HEALTHCHECK` on `/actuator/health`.
* `.dockerignore` keeps `target/`, `.git/`, docs and secrets out of the build context.

### 5.2 Full stack with Compose

```bash
docker compose up -d --build
docker compose ps
```
Expected: `calculator-calculator-api-1`, `calculator-calculator-api-2` and `calculator-nginx-1` all **Up (healthy)**. Nginx starts only after the API reports healthy (`depends_on: condition: service_healthy`).

```bash
curl -i "http://localhost:8081/api/v1/add?a=2&b=3"     # note the X-Served-By header
curl -i "http://localhost:8081/api/v1/add?a=2&b=3"     # run it again: a different replica IP answers
curl "http://localhost:8081/actuator/info"
docker compose down
```

**Compose talking points:** a named **network** (`calculator-net`), a **health check** for each service, **restart: unless-stopped**, **environment variables** (`SERVER_PORT`, `JAVA_TOOL_OPTIONS`, `INFO_APP_*`, `TZ`), **2 replicas** behind an **Nginx** reverse proxy that load-balances between them, and a **fixed project name** (`name: calculator`) so that redeploys are idempotent.

---

## Part 6 — Run Jenkins (inside Docker)

Jenkins runs as a container built from `jenkins/Dockerfile`, which adds Maven, the Docker CLI, the Compose plugin and all the required plugins. The container drives the **same Docker engine** you use from Ubuntu, through the mounted `/var/run/docker.sock`. That is how it builds images and runs `docker compose up`.

Jenkins checks the code out from **GitHub** into its own workspace, not from `~/Calculator_APP`. So the pipeline always builds what is pushed.

```bash
cd ~/Calculator_APP/jenkins
docker compose up -d --build        # first build takes 3-5 minutes
docker compose ps                   # "jenkins" should be Up
docker logs -f jenkins              # wait for "Jenkins is fully up and running", then Ctrl+C
```

### 6.1 Unlock and initial setup

1. Get the initial admin password:
   ```bash
   docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```
2. In the Windows browser, open <http://localhost:8080>, paste the password → **Continue**.
3. **Install suggested plugins** and wait for it to finish. The pipeline's own plugins are already preinstalled.
4. Create the admin user (your own username and password) → **Save and Continue**.
5. Instance configuration: Jenkins URL `http://localhost:8080/` → **Save and Finish → Start using Jenkins**.

### 6.2 Check that Jenkins can reach Docker and Maven

```bash
docker exec jenkins docker ps            # lists your containers, including "jenkins"
docker exec jenkins docker compose version
docker exec jenkins mvn -v
```
If you see "permission denied … docker.sock", see Troubleshooting T1.

---

## Part 7 — Credentials & email notification

### 7.1 Docker Hub credential (Credentials store, never hard-coded)

*Manage Jenkins → Credentials → System → Global credentials (unrestricted) → **Add Credentials***

| Field | Value |
|---|---|
| Kind | **Username with password** |
| Scope | Global |
| Username | `aishrma` (your Docker Hub username) |
| Password | the **Docker Hub access token** from Part 4 |
| ID | `dockerhub-creds` (must be exactly this, because the Jenkinsfile refers to it) |
| Description | Docker Hub token |

→ **Create**.

### 7.2 Gmail app password (the SMTP sender)

Jenkins sends mail through Gmail SMTP from a Gmail account you own. It can deliver to your IIITB address.
1. Go to <https://myaccount.google.com/security> and turn on **2-Step Verification** if it is off.
2. Go to <https://myaccount.google.com/apppasswords>, create an app password named `Jenkins`, and copy the 16-character password (no spaces).

### 7.3 Configure email in Jenkins

*Manage Jenkins → System*:

1. **Jenkins Location → System Admin e-mail address:** `Jenkins CI <your.gmail@gmail.com>`. This must be the same Gmail address, or Gmail rejects the mail.
2. **Extended E-mail Notification** (used by `emailext` in the Jenkinsfile):
   * SMTP server: `smtp.gmail.com`
   * SMTP Port: `465`
   * Click **Advanced…**
     * Credentials → **Add → Jenkins**: Kind *Username with password*, Username `your.gmail@gmail.com`, Password = the **app password**, ID `gmail-smtp` → Add, then select it
     * ☑ **Use SSL**
   * Default Content Type: `HTML (text/html)`
   * Default Recipients: `Aishwarya.Sharma@iiitb.ac.in`
3. **E-mail Notification** (classic mailer, only used for the test button):
   * SMTP server `smtp.gmail.com` → **Advanced** → ☑ Use SMTP Authentication: username `your.gmail@gmail.com`, password = app password, ☑ Use SSL, SMTP Port `465`
   * ☑ **Test configuration by sending test e-mail**: enter your IIITB address → **Test configuration**. You should see *"Email was successfully sent"*.
4. **Save**.

---

## Part 8 — Create the pipeline job

1. Dashboard → **New Item** → name `calculator-pipeline` (no spaces) → **Pipeline** → OK.
2. **General:**
   * ☑ **GitHub project**, Project url: `https://github.com/Aishrma/Calculator_APP/`
3. **Triggers:**
   * ☑ **GitHub hook trigger for GITScm polling**
4. **Pipeline:**
   * Definition: **Pipeline script from SCM**
   * SCM: **Git**
   * Repository URL: `https://github.com/Aishrma/Calculator_APP.git`
   * Credentials: *- none -* (the repo is public)
   * Branch Specifier: `*/main`
   * Script Path: `Jenkinsfile`
   * ☑ Lightweight checkout
5. **Save → Build Now.**

The first run takes **5–10 minutes**, because Maven and the Docker base images are downloaded once and cached afterwards. Watch it in *Console Output*, or click **Stages** / *Pipeline Overview* for the stage view.

**Expected:** every stage is green — `Checkout → Build → Test → Docker Build → Push → Deploy → Verify`. Then:
* The **Tests** tab shows 32 passing tests, and the **Coverage** report is linked on the build page.
* Docker Hub → `aishrma/calculator-api` → **Tags** shows `1` and `latest`.
* `docker ps` in Ubuntu shows the 2 API replicas and nginx, all healthy.
* <http://localhost:8081/api/v1/add?a=2&b=3> works in the Windows browser.
* A **SUCCESS email** arrives.

Click **Build Now** a second time. It should also go green. Compose sees the new tag (`2`), replaces the API containers, waits until they are healthy, and nginx keeps serving. This is the **idempotent re-run** requirement.

> The stack Jenkins deploys and the one you started by hand in Part 5 are the **same Compose project** (`calculator`). If you left the manual one running, Jenkins simply takes it over. That is intended.

### What each stage does (for the demo and the report)

| Stage | Command(s) | Why |
|---|---|---|
| Checkout | `checkout scm` | fetches exactly the commit that triggered the build (`skipDefaultCheckout` makes this an explicit, visible stage) |
| Build | `mvn -B clean package -DskipTests` | compiles and produces `target/calculator-api.jar`, which is archived and fingerprinted |
| Test | `mvn -B test` + `junit` + `recordCoverage` | a failing test exits non-zero, which fails the build. Results are published even on failure (`post { always }`). |
| Docker Build | `docker build -t user/calculator-api:$BUILD_NUMBER -t …:latest .` | the build number tag makes every deploy traceable and easy to roll back |
| Push | `withCredentials(dockerhub-creds)` → `docker login --password-stdin` → `docker push` | the secret is masked in the logs and never written in the Jenkinsfile. `docker logout` runs afterwards. |
| Deploy | `docker compose up -d --no-build --remove-orphans --wait` | starts only what changed. `--wait` blocks until the health checks pass, so an unhealthy deploy fails the build. |
| Verify | `wget` through nginx to `/actuator/health` and `/api/v1/add` | an end-to-end smoke test of the deployed stack |
| post | `emailext` on success/failure, `docker image prune -f` | notification, and housekeeping that never touches running containers |

`options { disableConcurrentBuilds() }` keeps two deploys from running at the same time. `timeout`, `timestamps` and `buildDiscarder` keep the job tidy.

---

## Part 9 — Automatic trigger with a GitHub webhook

GitHub cannot reach `localhost`, so you expose Jenkins through a free **ngrok** tunnel. ngrok runs as a container next to Jenkins, so nothing has to be installed in Ubuntu.

### 9.1 ngrok account

1. Sign up at <https://dashboard.ngrok.com/signup> (free).
2. Copy your **Authtoken** from *Getting Started → Your Authtoken*.
3. Go to *Universal Gateway → Domains* (or *Domains*) and claim your **free static domain**, for example `something-random.ngrok-free.app`. A static domain means the webhook URL never changes.

### 9.2 Start the tunnel

```bash
cd ~/Calculator_APP/jenkins
cp .env.example .env
nano .env            # paste NGROK_AUTHTOKEN and NGROK_DOMAIN, then save (Ctrl+O, Enter, Ctrl+X)
chmod 600 .env       # only you can read the token
docker compose --profile webhook up -d
docker logs jenkins-ngrok          # should show "started tunnel"
```
Open `https://<your-domain>.ngrok-free.app` in the browser. You should see the Jenkins login page (click through ngrok's "Visit Site" warning). Open <http://localhost:4040> for ngrok's request inspector.
`jenkins/.env` is git-ignored. Check that `git status` never lists it.

### 9.3 Add the webhook on GitHub

GitHub repo → *Settings → Webhooks → **Add webhook***:

| Field | Value |
|---|---|
| Payload URL | `https://<your-domain>.ngrok-free.app/github-webhook/` (**the trailing `/` matters**) |
| Content type | `application/json` |
| Secret | (leave empty) |
| Which events | **Just the push event** |
| Active | ☑ |

→ **Add webhook**. Refresh the page. **Recent Deliveries** should show a ping with a green ✔ (HTTP 200).

> The `githubPush()` trigger in the Jenkinsfile is registered only after the job has run **once** (you did that in Part 8).

### 9.4 Test the automatic trigger

```bash
cd ~/Calculator_APP
git checkout main && git pull
git checkout -b feature/webhook-test
sed -i '0,/<version>1.0.0<\/version>/s//<version>1.0.1<\/version>/' pom.xml   # bump the project version
git diff                                                                      # only the project version line changed
git commit -am "chore: bump version to 1.0.1"
git push -u origin feature/webhook-test
gh pr create --fill --base main && gh pr merge --merge
git checkout main && git pull
```
**Within seconds** a new build starts in Jenkins without anyone clicking anything. The build page says *"Started by GitHub push by Aishrma"*. When it finishes, `curl http://localhost:8081/actuator/info` shows `"version":"1.0.1"` and the new `image.tag`.

> **Fallback if the webhook is impossible** (e.g. the network blocks ngrok): in the Jenkinsfile, replace `githubPush()` with `pollSCM('H/2 * * * *')`. Jenkins then checks GitHub every 2 minutes. The spec accepts this, but a webhook is preferred.

---

## Part 10 — Screenshots for the report

Save them in `docs/screenshots/` with these names and send them to me. I will then write the report (PDF).
On Windows, the folder is at `\\wsl.localhost\Ubuntu\home\<your-linux-user>\Calculator_APP\docs\screenshots`. Paste that into Explorer's address bar.

| # | File name | What to capture |
|---|---|---|
| 1 | `01-jenkins-pipeline-green.png` | Job page / *Stages* view with **all stages green** (ideally after a webhook-triggered build) |
| 2 | `02-jenkins-console-success.png` | Bottom of *Console Output*: Verify stage output + `Finished: SUCCESS` |
| 3 | `03-jenkins-trigger.png` | Build page showing *"Started by GitHub push by …"* |
| 4 | `04-jenkins-tests.png` | *Tests* result page (32 passed) and/or the *Coverage* report |
| 5 | `05-email-success.png` | The SUCCESS email in your inbox |
| 6 | `06-email-failure.png` | (optional, recommended) a FAILURE email; see Part 11 step 5 |
| 7 | `07-docker-ps.png` | `docker ps` in the Ubuntu terminal showing the api replicas, nginx and jenkins, healthy |
| 8 | `08-api-responses.png` | `curl` output for add, subtract, multiply, divide, divide-by-zero (400) and missing parameter (400) |
| 9 | `09-health.png` | `/actuator/health` and `/actuator/info` responses |
| 10 | `10-dockerhub.png` | Docker Hub repository page with the tags list |
| 11 | `11-github-history.png` | GitHub *Insights → Network* graph, or `git log --oneline --graph --all` |
| 12 | `12-github-prs.png` | *Pull requests → Closed* showing the merged PRs |
| 13 | `13-webhook-delivery.png` | GitHub *Settings → Webhooks → Recent Deliveries* with green ✔ |
| 14 | `14-credentials.png` | Jenkins *Credentials* page showing `dockerhub-creds` (the secret is hidden anyway) |

Handy command for screenshot 8:
```bash
for u in "add?a=2&b=3" "subtract?a=10&b=4" "multiply?a=6&b=7" "divide?a=7&b=2" "divide?a=5&b=0" "add?a=5" "add?a=abc&b=1"; do
  echo "GET /api/v1/$u"
  curl -s -w "  [HTTP %{http_code}]\n" "http://localhost:8081/api/v1/$u"
done
```

Also send me: your **Docker Hub repository URL**, and anything that went wrong while setting up (for the "Challenges faced" section of the report).

---

## Part 11 — Live demo script (Class 17)

**Before class:**
1. Start **Docker Desktop**, then open Ubuntu.
2. Run `cd ~/Calculator_APP/jenkins && docker compose --profile webhook up -d`.
3. Make sure Jenkins is open at `localhost:8080`, the last build is green, and the app is reachable at `localhost:8081`.
4. Open browser tabs for GitHub, Jenkins, Docker Hub and your inbox.

1. **Show the running deployment:** `docker ps`, then `curl "http://localhost:8081/api/v1/add?a=2&b=3"` and `/actuator/info` (note the current version and image tag).
2. **Make a change on a feature branch.** A good visible change is a new endpoint. Add this to `CalculatorService`:
   ```java
   public BigDecimal power(BigDecimal a, int exponent) {
       return normalize(a.pow(exponent, PRECISION));
   }
   ```
   then this to `CalculatorController`:
   ```java
   @GetMapping("/power")
   public CalculationResponse power(@RequestParam BigDecimal a, @RequestParam int b) {
       return response("power", a, BigDecimal.valueOf(b), calculatorService.power(a, b));
   }
   ```
   and a test to `CalculatorServiceTest`:
   ```java
   @Test
   void power() {
       assertNumber(service.power(bd("2"), 10), "1024");
   }
   ```
   (Or, simpler: bump the version in `pom.xml`.)
3. Push it and merge it:
   ```bash
   git checkout -b feature/power-endpoint
   ./mvnw -q test                       # quick local check first
   git add -A && git commit -m "feat: add power endpoint"
   git push -u origin feature/power-endpoint
   gh pr create --fill --base main && gh pr merge --merge
   ```
4. **Switch to Jenkins.** A build starts by itself ("Started by GitHub push"). Walk through the stages as they turn green. Then:
   `curl "http://localhost:8081/api/v1/power?a=2&b=10"` returns `{"operation":"power","a":2,"b":10,"result":1024}`.
   `/actuator/info` shows the **new image tag**, Docker Hub has the new tag, and the success email arrives.
5. **(Optional) Show the failure path:** on a branch, change an expected value in a test (e.g. `"2, 3, 5"` → `"2, 3, 6"`), merge it, and the **Test** stage goes red. Later stages are skipped, the **running deployment is untouched** (`curl` still works), and a **FAILURE email** with the log attached arrives. Revert it afterwards with `git revert HEAD` and push.
6. **Show idempotency:** click *Build Now* on the same commit. It goes green again with no downtime problems.

**Questions to be ready for:** why multi-stage, why a non-root user, how the credentials are protected, what `--wait` does, how nginx finds the replicas (Docker DNS + `resolve`), why a fixed compose project name, what happens when a test fails, how `githubPush()` and the webhook connect, and how WSL, Docker Desktop and the Jenkins container share one Docker engine.

---

## Part 12 — Troubleshooting

| # | Symptom | Fix |
|---|---|---|
| T1 | `permission denied while trying to connect to the Docker daemon socket` **in Jenkins** | Make sure `user: root` is in `jenkins/docker-compose.yml`, then run `docker compose up -d --force-recreate` in `jenkins/` |
| T2 | `permission denied ... docker.sock` **in your Ubuntu shell** | With Docker Desktop: re-toggle *Settings → Resources → WSL integration → Ubuntu*, then run `wsl --shutdown` in Windows and reopen Ubuntu |
| T3 | `The command 'docker' could not be found in this WSL 2 distro` | Docker Desktop is not running, or WSL integration for Ubuntu is off (Part 0.2) |
| T4 | `docker: not found` / `mvn: not found` in the **Jenkins build log** | The job is running on stock Jenkins. Rebuild the custom image: `cd jenkins && docker compose up -d --build` |
| T5 | `./mvnw: Permission denied` | `chmod +x mvnw` (and `git update-index --chmod=+x mvnw` if it was committed without the bit) |
| T6 | `/usr/bin/env: 'sh\r': No such file or directory` | A file got CRLF line endings. `sudo apt install -y dos2unix && dos2unix mvnw`. `.gitattributes` and `core.autocrlf input` prevent this from coming back. |
| T7 | `release version 17 not supported` / `JAVA_HOME` errors | `sudo apt install openjdk-17-jdk-headless`, then `sudo update-alternatives --config java` and pick 17 |
| T8 | `Bind for 0.0.0.0:8080 failed: port is already allocated` | Something else (the app from Part 2?) is on 8080. Find it with `sudo ss -ltnp \| grep 8080`. On the Windows side, check with `netstat -ano \| findstr :8080`. |
| T9 | `localhost:8080` / `8081` does not open in the Windows browser | Try `http://127.0.0.1:8081`. Check that `curl` works inside Ubuntu. Check that Docker Desktop is running. As a last resort, run `wsl --shutdown` in Windows and start again. |
| T10 | Deploy stage: `unknown flag: --wait` | Docker Compose is too old. Update Docker Desktop (the Jenkins image installs the latest compose plugin) |
| T11 | Deploy times out: `container ... is unhealthy` | `docker compose logs calculator-api` (from `~/Calculator_APP`) to see why the app did not start |
| T12 | Push stage: `unauthorized: incorrect username or password` | Re-create the `dockerhub-creds` credential with the **token** (Read & Write) and the right username |
| T13 | Push: `denied: requested access to the resource is denied` | `DOCKERHUB_USER` in the Jenkinsfile does not match the credential's username |
| T14 | Email: `535 Authentication failed` / `Username and Password not accepted` | Use the Gmail **app password**, not your normal one. Use port 465 + SSL. The *System Admin e-mail address* must be the same Gmail address. |
| T15 | No email at all, but the build is green | Check the console for `Sending email to: …`. Check spam. Check that the *Extended E-mail Notification* section (not just *E-mail Notification*) has credentials. |
| T16 | Webhook delivery shows 302/403/404 | The payload URL must end in `/github-webhook/` (trailing slash), and the ngrok tunnel must be running (`docker logs jenkins-ngrok`) |
| T17 | Webhook is 200 but no build starts | Run the job manually once. Tick *GitHub hook trigger for GITScm polling*. The branch must be `*/main` and the push must reach `main`. |
| T18 | `recordCoverage` / `No such DSL method` | A plugin is missing. *Manage Jenkins → Plugins → Available* → install **Coverage** (or **Email Extension**), then restart Jenkins |
| T19 | nginx returns `502 Bad Gateway` | The API replicas are still starting. Wait for `docker compose ps` to show healthy. |
| T20 | `git push` asks for a password / `Authentication failed` | Run `gh auth login`, then `gh auth setup-git` (Part 0.5) |
| T21 | Builds are very slow, or `git status` shows every file as modified (mode change) | You are working under `/mnt/c/...`. Use `~/Calculator_APP` instead (Part 1). |

### Useful commands

```bash
cd ~/Calculator_APP
docker compose ps                         # app stack status (project "calculator")
docker compose logs -f calculator-api     # app logs
docker compose down                       # stop the app stack
(cd jenkins && docker compose stop)       # stop Jenkins + ngrok (data persists in the jenkins_home volume)
(cd jenkins && docker compose --profile webhook up -d)   # start Jenkins + ngrok again
docker system df                          # disk usage; `docker image prune` to clean up
```
