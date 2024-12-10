
function getCookie(name: string) {
    const cookieArr = document.cookie.split(';');
    for (let cookie of cookieArr) {
        const [key, value] = cookie.trim().split('=');
        if (key === name) {
            return decodeURIComponent(value); // 解码 Cookie 值
        }
    }
    return null;
}

export {
    getCookie,
}