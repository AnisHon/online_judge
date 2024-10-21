import {get} from "@/utils/http"

export interface TagView {

    tagId: number,
    tagName: string,
    tagColor: string,
    createTime: Date
}

async function getAllTags(): Promise<TagView[]> {
    const { code, data, message} = await get("/problem-api/tag/getAll")
    if (code !== 200) {
        ElMessage.warning(message)
    }
    return data;
}

export {
    getAllTags,
}