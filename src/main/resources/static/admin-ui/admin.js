const TOKEN_KEY = "my4cutAdminAccessToken";

const loginPanel = document.querySelector("#login-panel");
const adminPanel = document.querySelector("#admin-panel");
const loginForm = document.querySelector("#login-form");
const createForm = document.querySelector("#create-form");
const editForm = document.querySelector("#edit-form");
const editDialog = document.querySelector("#edit-dialog");
const poseList = document.querySelector("#pose-list");
const poseCardTemplate = document.querySelector("#pose-card-template");
const peopleFilter = document.querySelector("#people-filter");
const loginMessage = document.querySelector("#login-message");
const pageMessage = document.querySelector("#page-message");

let accessToken = sessionStorage.getItem(TOKEN_KEY);
let poses = [];
let imageObjectUrls = [];

loginForm.addEventListener("submit", login);
createForm.addEventListener("submit", createPose);
editForm.addEventListener("submit", updatePose);
peopleFilter.addEventListener("change", loadPoses);
document.querySelector("#logout-button").addEventListener("click", logout);
document.querySelector("#close-dialog").addEventListener("click", () => editDialog.close());

if (accessToken) {
    showAdmin();
    loadPoses();
}

async function login(event) {
    event.preventDefault();
    setMessage(loginMessage, "");

    try {
        const payload = await request("/auth/login", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                email: document.querySelector("#email").value,
                password: document.querySelector("#password").value
            })
        }, false);

        accessToken = payload.data.accessToken;
        sessionStorage.setItem(TOKEN_KEY, accessToken);
        loginForm.reset();
        showAdmin();
        await loadPoses();
    } catch (error) {
        setMessage(loginMessage, error.message);
    }
}

async function loadPoses() {
    try {
        const peopleCount = peopleFilter.value;
        const query = peopleCount ? `?peopleCount=${encodeURIComponent(peopleCount)}` : "";
        const payload = await request(`/poses${query}`);
        poses = payload.data || [];
        renderPoses();
        setMessage(pageMessage, "");
    } catch (error) {
        setMessage(pageMessage, error.message);
    }
}

function renderPoses() {
    imageObjectUrls.forEach((url) => URL.revokeObjectURL(url));
    imageObjectUrls = [];
    poseList.replaceChildren();
    document.querySelector("#pose-count").textContent = `총 ${poses.length}개`;

    if (poses.length === 0) {
        const empty = document.createElement("p");
        empty.className = "muted";
        empty.textContent = "등록된 포즈가 없습니다.";
        poseList.append(empty);
        return;
    }

    poses.forEach((pose) => {
        const card = poseCardTemplate.content.cloneNode(true);
        const image = card.querySelector(".pose-image");
        image.alt = pose.title;
        loadPoseImage(image, pose.viewUrl);
        card.querySelector(".pose-title").textContent = pose.title;
        card.querySelector(".pose-people").textContent = `${pose.peopleCount}명`;
        card.querySelector(".edit-button").addEventListener("click", () => openEditDialog(pose));
        card.querySelector(".delete-button").addEventListener("click", () => deletePose(pose));
        poseList.append(card);
    });
}

async function loadPoseImage(imageElement, viewUrl) {
    if (!viewUrl) {
        imageElement.removeAttribute("src");
        return;
    }

    const imageUrl = new URL(viewUrl, window.location.origin);
    if (imageUrl.origin !== window.location.origin || !imageUrl.pathname.startsWith("/images/")) {
        imageElement.src = viewUrl;
        return;
    }

    try {
        const response = await fetch(imageUrl, {
            headers: {"Authorization": `Bearer ${accessToken}`}
        });
        if (!response.ok) {
            throw new Error("이미지를 불러오지 못했습니다.");
        }
        const objectUrl = URL.createObjectURL(await response.blob());
        imageObjectUrls.push(objectUrl);
        imageElement.src = objectUrl;
    } catch {
        imageElement.removeAttribute("src");
    }
}

async function createPose(event) {
    event.preventDefault();
    const submitButton = createForm.querySelector("button[type='submit']");
    submitButton.disabled = true;

    try {
        const metadata = {
            title: document.querySelector("#create-title").value.trim(),
            peopleCount: Number(document.querySelector("#create-people-count").value)
        };
        const formData = new FormData();
        formData.append("metadata", jsonPart(metadata));
        formData.append("image", document.querySelector("#create-image").files[0]);

        await request("/admin/poses", {method: "POST", body: formData});
        createForm.reset();
        await loadPoses();
        setMessage(pageMessage, "포즈를 등록했습니다.", true);
    } catch (error) {
        setMessage(pageMessage, error.message);
    } finally {
        submitButton.disabled = false;
    }
}

function openEditDialog(pose) {
    document.querySelector("#edit-id").value = pose.poseId;
    document.querySelector("#edit-title").value = pose.title;
    document.querySelector("#edit-people-count").value = pose.peopleCount;
    document.querySelector("#edit-image").value = "";
    editDialog.showModal();
}

async function updatePose(event) {
    event.preventDefault();
    const submitButton = editForm.querySelector("button[type='submit']");
    submitButton.disabled = true;

    try {
        const poseId = document.querySelector("#edit-id").value;
        const metadata = {
            title: document.querySelector("#edit-title").value.trim(),
            peopleCount: Number(document.querySelector("#edit-people-count").value)
        };
        const image = document.querySelector("#edit-image").files[0];
        const formData = new FormData();
        formData.append("metadata", jsonPart(metadata));
        if (image) {
            formData.append("image", image);
        }

        await request(`/admin/poses/${poseId}`, {method: "PATCH", body: formData});
        editDialog.close();
        await loadPoses();
        setMessage(pageMessage, "포즈를 수정했습니다.", true);
    } catch (error) {
        setMessage(pageMessage, error.message);
    } finally {
        submitButton.disabled = false;
    }
}

async function deletePose(pose) {
    if (!window.confirm(`'${pose.title}' 포즈를 삭제할까요?`)) {
        return;
    }

    try {
        await request(`/admin/poses/${pose.poseId}`, {method: "DELETE"});
        await loadPoses();
        setMessage(pageMessage, "포즈를 삭제했습니다.", true);
    } catch (error) {
        setMessage(pageMessage, error.message);
    }
}

async function request(path, options = {}, authenticate = true) {
    const headers = new Headers(options.headers || {});
    if (authenticate && accessToken) {
        headers.set("Authorization", `Bearer ${accessToken}`);
    }

    const response = await fetch(path, {...options, headers});
    const payload = await response.json().catch(() => null);

    if (response.status === 401) {
        logout();
        throw new Error("로그인이 만료되었습니다. 다시 로그인해 주세요.");
    }
    if (response.status === 403) {
        throw new Error("관리자 권한이 없는 계정입니다.");
    }
    if (!response.ok) {
        throw new Error(payload?.message || "요청을 처리하지 못했습니다.");
    }
    return payload;
}

function jsonPart(value) {
    return new Blob([JSON.stringify(value)], {type: "application/json"});
}

function showAdmin() {
    loginPanel.classList.add("hidden");
    adminPanel.classList.remove("hidden");
}

function logout() {
    accessToken = null;
    poses = [];
    sessionStorage.removeItem(TOKEN_KEY);
    adminPanel.classList.add("hidden");
    loginPanel.classList.remove("hidden");
    imageObjectUrls.forEach((url) => URL.revokeObjectURL(url));
    imageObjectUrls = [];
    poseList.replaceChildren();
}

function setMessage(element, message, success = false) {
    element.textContent = message;
    element.classList.toggle("success", success);
}
