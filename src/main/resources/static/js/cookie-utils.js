function setCookie(name, value, days, options = {}) {
    const {
        path = "/",
        sameSite = "Lax",
        secure = false
    } = options;

    let cookie = `${name}=${encodeURIComponent(value)}`;

    if (typeof days === "number") {
        const expires = new Date(Date.now() + days * 864e5).toUTCString();
        cookie += `; expires=${expires}`;
    }

    cookie += `; path=${path}; SameSite=${sameSite}`;

    if (secure) {
        cookie += "; Secure";
    }

    document.cookie = cookie;
}

function getCookie(name) {
    return document.cookie
        .split("; ")
        .map(c => c.split("="))
        .find(([key]) => key === name)
        ?.[1]
        ? decodeURIComponent(
            document.cookie
                .split("; ")
                .map(c => c.split("="))
                .find(([key]) => key === name)[1]
        )
        : undefined;
}

function deleteCookie(name, path = "/") {
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=${path}`;
}
