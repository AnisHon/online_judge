import {ContestAuth} from "@/api/contest";
import dayjs from "dayjs";

const authText = (auth: ContestAuth) => {
    const results = ['公开赛', '私有赛', '白名单']
    return results[auth]
}

const authTagType = (auth: ContestAuth) => {
    const results = ['success', 'danger', 'info']
    return results[auth]
}

const differ = (start: string, end: string) => {
    const startTime = dayjs(start);
    const endTime = dayjs(end);
    if (!startTime.isValid() || !endTime.isValid() || !endTime.isAfter(startTime)) {
        return '不足1小时';
    }

    let cursor = startTime;
    const units: Array<[dayjs.ManipulateType, string]> = [
        ['year', '年'],
        ['month', '个月'],
        ['day', '天'],
        ['hour', '小时'],
    ];
    const parts: string[] = [];
    for (const [unit, label] of units) {
        const value = endTime.diff(cursor, unit);
        if (value > 0) {
            parts.push(`${value}${label}`);
            cursor = cursor.add(value, unit);
        }
    }
    return parts.length ? parts.join('') : '不足1小时';
};

const isContestOver = (end: string | undefined): boolean => {
    if (!end) {
        return true;
    }
    const currTimeStamp = dayjs().unix();
    const endTimeStamp = dayjs(end).unix();
    return currTimeStamp > endTimeStamp
};

const isNotStart = (start: string | undefined) => {
    if (!start) {
        return true;
    }
    const currTimeStamp = dayjs().unix();
    const startTimeStamp = dayjs(start).unix();
    return currTimeStamp < startTimeStamp;
};

const formatDate = (dateStr: string) => {
    return dayjs(dateStr).format('YYYY/MM/DD HH:mm')
};

export {
    authText,
    authTagType,
    differ,
    isContestOver,
    isNotStart,
    formatDate
}
