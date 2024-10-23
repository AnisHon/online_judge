function numberToLetter(num: number): string {
    let letter = '';
    while (num > 0) {
        const remainder = (num - 1) % 26;
        letter = String.fromCharCode(65 + remainder) + letter; // 65 是 'A' 的 ASCII 值
        num = Math.floor((num - remainder) / 26);
    }
    return letter;
}

function letterToNumber(letter: string): number {
    let num = 0;
    for (let i = 0; i < letter.length; i++) {
        num = num * 26 + (letter.charCodeAt(i) - 65 + 1); // 'A' 的 ASCII 值为 65
    }
    return num;
}



export {
    numberToLetter,
    letterToNumber,
};