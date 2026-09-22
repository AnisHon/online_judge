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

const parseTime = (value: string | undefined) => dayjs(value || 'invalid');

const getActivityTimeState = (start: string | undefined, end: string | undefined, now = dayjs()) => {
    const startTime = parseTime(start);
    const endTime = parseTime(end);
    const valid = startTime.isValid() && endTime.isValid() && endTime.isAfter(startTime);
    return {
        valid,
        notStarted: !valid || now.isBefore(startTime),
        over: !valid || !now.isBefore(endTime),
    };
};

const differ = (start: string, end: string) => {
    const startTime = parseTime(start);
    const endTime = parseTime(end);
    if (!startTime.isValid() || !endTime.isValid() || !endTime.isAfter(startTime)) {
        return '时间待确认';
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

const isContestOver = (end: string | undefined, now = dayjs()): boolean => {
    if (!end) {
        return true;
    }
    const endTime = parseTime(end);
    return !endTime.isValid() || !now.isBefore(endTime);
};

const isNotStart = (start: string | undefined, now = dayjs()) => {
    if (!start) {
        return true;
    }
    const startTime = parseTime(start);
    return !startTime.isValid() || now.isBefore(startTime);
};

const formatDate = (dateStr: string | undefined) => {
    const date = parseTime(dateStr);
    return date.isValid() ? date.format('YYYY/MM/DD HH:mm') : '时间异常';
};

export {
    authText,
    authTagType,
    differ,
    isContestOver,
    isNotStart,
    formatDate,
    getActivityTimeState,
}
