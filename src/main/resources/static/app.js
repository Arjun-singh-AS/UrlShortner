const form = document.querySelector("#shorten-form");
const urlInput = document.querySelector("#long-url");
const aliasInput = document.querySelector("#custom-alias");
const expirationInput = document.querySelector("#expires-at");
const linksList = document.querySelector("#links-list");
const emptyState = document.querySelector("#empty-state");
const createdResult = document.querySelector("#created-result");
const toast = document.querySelector("#toast");
let links = [];
let toastTimeout;

document.querySelector("#alias-prefix").textContent = `${window.location.host}/`;

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function showToast(message, isError = false) {
    window.clearTimeout(toastTimeout);
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.classList.add("visible");
    toastTimeout = window.setTimeout(() => toast.classList.remove("visible"), 3200);
}

async function request(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: { "Content-Type": "application/json", ...options.headers }
    });
    if (!response.ok) {
        let message = "Something went wrong. Please try again.";
        try {
            const body = await response.json();
            message = body.error || body.detail || message;
        } catch {
            // Keep a readable fallback for non-JSON server errors.
        }
        throw new Error(message);
    }
    return response.status === 204 ? null : response.json();
}

function formattedDate(instant) {
    return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", year: "numeric" }).format(new Date(instant));
}

function destinationHost(url) {
    try {
        return new URL(url).hostname.replace(/^www\./, "");
    } catch {
        return "Website";
    }
}

function renderLinks() {
    const query = document.querySelector("#search-links").value.trim().toLowerCase();
    const filteredLinks = links.filter((link) =>
        `${link.shortCode} ${link.shortUrl} ${link.originalUrl}`.toLowerCase().includes(query)
    );
    const totalClicks = links.reduce((total, link) => total + link.clickCount, 0);
    const activeCount = links.filter((link) => !link.expired).length;

    document.querySelector("#total-links").textContent = links.length.toLocaleString();
    document.querySelector("#total-clicks").textContent = totalClicks.toLocaleString();
    document.querySelector("#active-links").textContent = activeCount.toLocaleString();
    document.querySelector("#nav-count").textContent = links.length > 99 ? "99+" : links.length;
    document.querySelector("#links-count").textContent = filteredLinks.length;
    linksList.innerHTML = filteredLinks.map((link) => {
        const host = destinationHost(link.originalUrl);
        const suffix = new URL(link.originalUrl).pathname + new URL(link.originalUrl).search;
        return `
            <article class="link-row${link.expired ? " expired" : ""}">
                <div class="short-cell">
                    <a href="${escapeHtml(link.shortUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(link.shortUrl)}${link.expired ? '<span class="expired-badge">EXPIRED</span>' : ""}</a>
                    <small>Created ${formattedDate(link.createdAt)}</small>
                </div>
                <div class="destination-cell">
                    <span class="destination-favicon" aria-hidden="true">${escapeHtml(host.charAt(0))}</span>
                    <span class="destination-text"><strong>${escapeHtml(host)}</strong><small>${escapeHtml(suffix || "/")}</small></span>
                </div>
                <div class="clicks-cell">${link.clickCount.toLocaleString()}</div>
                <div class="created-cell">${formattedDate(link.createdAt)}</div>
                <div class="row-actions"><button class="icon-button delete-link" type="button" data-code="${escapeHtml(link.shortCode)}" aria-label="Delete ${escapeHtml(link.shortUrl)}"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16m-10 4v6m4-6v6M5.5 7l1 13h11l1-13M9 7V4h6v3"/></svg></button></div>
            </article>`;
    }).join("");
    emptyState.hidden = filteredLinks.length > 0;
    if (links.length > 0 && filteredLinks.length === 0) {
        emptyState.querySelector("h3").textContent = "No matching links";
        emptyState.querySelector("p").textContent = "Try another URL or short code.";
    } else {
        emptyState.querySelector("h3").textContent = "No links yet";
        emptyState.querySelector("p").textContent = "Your next great link is one URL away.";
    }
}

async function loadLinks() {
    try {
        links = await request("/api/urls");
        renderLinks();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function copyToClipboard(value) {
    try {
        await navigator.clipboard.writeText(value);
        showToast("Short link copied to your clipboard.");
    } catch {
        showToast("Could not access the clipboard. Select and copy the link instead.", true);
    }
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;

    const button = form.querySelector(".create-button");
    button.disabled = true;
    button.querySelector("span").textContent = "Creating...";
    createdResult.hidden = true;

    const expiresAt = expirationInput.value
        ? new Date(`${expirationInput.value}T23:59:59`).toISOString()
        : null;
    try {
        const created = await request("/api/shorten", {
            method: "POST",
            body: JSON.stringify({
                url: urlInput.value.trim(),
                customAlias: aliasInput.value.trim() || null,
                expiresAt
            })
        });
        createdResult.innerHTML = `
            <div class="result-info">
                <span class="result-label">YOUR SHORT LINK IS READY</span>
                <a class="result-link" href="${escapeHtml(created.shortUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(created.shortUrl)}</a>
                <span class="result-destination">${escapeHtml(created.originalUrl)}</span>
            </div>
            <button class="copy-button" type="button" data-copy="${escapeHtml(created.shortUrl)}">Copy link</button>`;
        createdResult.hidden = false;
        form.reset();
        document.querySelector("#alias-prefix").textContent = `${window.location.host}/`;
        showToast("Your short link is ready to share.");
        await loadLinks();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        button.disabled = false;
        button.querySelector("span").textContent = "Create short link";
    }
});

aliasInput.addEventListener("input", () => {
    document.querySelector("#alias-prefix").textContent = `${window.location.host}/`;
});

document.querySelector("#search-links").addEventListener("input", renderLinks);

document.addEventListener("click", async (event) => {
    const copyButton = event.target.closest("[data-copy]");
    if (copyButton) {
        await copyToClipboard(copyButton.dataset.copy);
        return;
    }

    const deleteButton = event.target.closest(".delete-link");
    if (!deleteButton) return;

    try {
        await request(`/api/urls/${encodeURIComponent(deleteButton.dataset.code)}`, { method: "DELETE" });
        links = links.filter((link) => link.shortCode !== deleteButton.dataset.code);
        renderLinks();
        showToast("Short link deleted.");
    } catch (error) {
        showToast(error.message, true);
    }
});

loadLinks();
