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
    return endTime.diff(startTime, 'hours');
};

const isContestOver = (end: string) => {
    const currTimeStamp = dayjs().unix();
    const endTimeStamp = dayjs(end).unix();
    return currTimeStamp > endTimeStamp
};

const isNotStart = (start: string) => {
    const currTimeStamp = dayjs().unix();
    const startTimeStamp = dayjs(start).unix();
    return currTimeStamp < startTimeStamp;
};

const formatDate = (dateStr: string) => {
    return  dayjs(dateStr).format('YYYY/MM/DD HH:mm:ss')
};

export {
    authText,
    authTagType,
    differ,
    isContestOver,
    isNotStart,
    formatDate
}