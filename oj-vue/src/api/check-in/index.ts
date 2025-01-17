import {debounce} from "@/utils/debounce";
import {get} from "@/utils/http";
import useLoading from "@/hooks/useLoading";
import type {IdType} from "@/api/common.ts";

interface UserCheckIn {
    userId: IdType;
    nikeName: string;
    continuityDays: number;
    rewardPoint: number;
    signTime: Date;
}

interface CheckInInfo {
    award: number,
    msg: string,
    success: boolean,
    userCheckIn: UserCheckIn
}

const isCheckedIn = async () => {
    const {data} = await get<boolean>("user-api/check-in/already")
    return data
}
const checkInFetcher = (callback: (data :CheckInInfo) => void) => {
    const {loading, isLoading, finish} = useLoading()
    const sendCheckIn = debounce(async () => {
        const {data} = await get<CheckInInfo>("user-api/check-in");
        if (data.success) {
            callback(data);
        } else {
            ElMessage.warning(data.msg);
        }
        finish()
    }, 1000);
    return {isLoading, loading, sendCheckIn};
}

const checkIn = debounce(async (callback: (data :CheckInInfo) => void, loading: any ) => {

    const {data} = await get<CheckInInfo>("user-api/check-in");
    if (data.success) {
        callback(data);
    } else {
        ElMessage.warning(data.msg);
    }
    loading.value = false;
}, 1000)

const checkInList = async () => {
    const {data} = await get<UserCheckIn[]>("user-api/check-in/list");
    return data;
}

const todayCount = async (): Promise<number> => {
    const {data} = await get<number>("user-api/check-in/today");
    return data;
}

export {
    checkInList,
    isCheckedIn,
    // checkIn,
    checkInFetcher,
    todayCount,
    type CheckInInfo,
    type UserCheckIn
}